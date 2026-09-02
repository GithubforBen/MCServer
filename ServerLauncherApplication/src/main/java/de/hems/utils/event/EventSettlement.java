package de.hems.utils.event;

import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.event.EventUpdatedEvent;
import de.hems.communication.events.event.RunUpdatedEvent;
import de.hems.types.event.AwardData;
import de.hems.types.event.EventData;
import de.hems.types.event.EventState;
import de.hems.types.event.PrizeData;
import de.hems.types.event.RunData;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Closes events that have run their course.
 * <p>
 * An event ends by the clock, which is almost never a moment anybody is watching. So this checks every
 * minute for events whose time is up and settles them in one go: the prizes are worked out from the
 * leaderboard and put aside, the servers the runs were played on are switched off, and the runs themselves
 * are removed. What is left of the event afterwards is the event and the prizes it handed out.
 */
public class EventSettlement {

    /** How often to look for events that have ended. */
    private static final long CHECK_INTERVAL_MS = 60_000L;
    /** How long a run may lie untouched before it is given up on, unless the config says otherwise. */
    private static final int DEFAULT_ABANDON_HOURS = 24;

    private final EventStore events;
    private final RunStore runs;
    private final AwardStore awards;

    public EventSettlement(EventStore events, RunStore runs, AwardStore awards) {
        this.events = events;
        this.runs = runs;
        this.awards = awards;
        Timer timer = new Timer("event-settlement", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    settleFinished();
                } catch (Exception e) {
                    System.out.println("Could not settle the events: " + e.getMessage());
                }
                try {
                    abandonForgottenRuns();
                } catch (Exception e) {
                    System.out.println("Could not tidy up the runs: " + e.getMessage());
                }
            }
        }, CHECK_INTERVAL_MS, CHECK_INTERVAL_MS);
    }

    /**
     * Settles every event whose time is up and that has not been settled yet.
     * <p>
     * A cancelled event is a special case. Cancelling is a decision somebody can take back - the button
     * to do it is right there in the calendar - and settling throws the runs and their worlds away, so an
     * event that was cancelled by mistake used to come back without anything that had been played on it.
     * So a cancelled event is put on ice instead: its servers are switched off, which is the part that
     * costs memory, and the rest waits until the time it was planned for has actually passed.
     */
    public void settleFinished() {
        long now = System.currentTimeMillis();
        for (EventData event : events.getEvents()) {
            if (event.isApplied()) continue;
            EventState state = event.getState();
            if (state == EventState.CANCELLED && now < event.getEndsAt()) {
                suspend(event);
                continue;
            }
            if (state != EventState.FINISHED && state != EventState.CANCELLED) continue;
            settle(event);
        }
    }

    /**
     * Frees what a cancelled event is holding without throwing anything away.
     * <p>
     * Cheap to repeat: a server that is already off is not stopped again, and a run that is already
     * paused stays as it is. So this can run every minute until the event's time is finally up.
     *
     * @param event the cancelled event
     */
    private void suspend(EventData event) {
        List<RunData> board = runs.getRunsOf(event.getId());
        if (board.isEmpty()) return;
        boolean changed = false;
        for (RunData run : board) {
            if (run.getState() != RunData.State.RUNNING) continue;
            // the store hands out its own objects, so changing one and writing it back is the whole update
            run.pause();
            runs.put(run);
            announceRun(run);
            changed = true;
        }
        stopServers(board);
        if (changed) {
            System.out.println("Event " + event.getName() + " is cancelled - its runs are paused and wait "
                    + "until its time is over, so reactivating it brings them back.");
        }
    }

    /**
     * Gives up on runs nobody has come back to.
     * <p>
     * A paused run keeps a whole generated world on the disk and offers its team a "continue" button. Left
     * alone that is fine for an hour and wrong after a week: an event that runs for days would collect a
     * world per attempt, and the button would eventually be pressed for a run whose team has long stopped
     * caring. After the configured time the run is closed as abandoned and its server is thrown away, so
     * what is offered is the truth.
     */
    public void abandonForgottenRuns() {
        long limit = abandonAfterHours() * 60L * 60L * 1000L;
        if (limit <= 0L) return;
        long now = System.currentTimeMillis();
        List<RunData> forgotten = new java.util.ArrayList<>();
        for (RunData run : runs.getRuns()) {
            if (!run.isOpen() || run.quietFor(now) < limit) continue;
            EventData event = events.getEvent(run.getEventId());
            // an event that is over is settled as a whole, which does this and more
            if (event == null || event.isApplied()) continue;
            forgotten.add(run);
        }
        if (forgotten.isEmpty()) return;
        for (RunData run : forgotten) {
            long quiet = run.quietFor(now);
            run.finish(RunData.State.ABANDONED);
            runs.put(run);
            announceRun(run);
            System.out.println("Run " + run.getId() + " was untouched for " + (quiet / 3_600_000L)
                    + " hours and is given up on.");
        }
        discardServers(forgotten);
    }

    /**
     * @return how many hours a run may lie untouched, {@code 0} to never give up on one
     */
    private int abandonAfterHours() {
        YamlConfiguration config = Main.getInstance().getConfiguration().getConfig();
        if (!config.contains("runs.abandon-after-hours")) {
            config.set("runs.abandon-after-hours", DEFAULT_ABANDON_HOURS);
            config.setComments("runs.abandon-after-hours", List.of(
                    "How many hours a paused event run may lie untouched before it is given up on and its",
                    "server is thrown away. 0 keeps every run until its event is over."));
            Main.getInstance().getConfiguration().save();
        }
        return config.getInt("runs.abandon-after-hours", DEFAULT_ABANDON_HOURS);
    }

    /**
     * Tells the network that a run changed.
     *
     * @param run the run in its new state
     */
    private void announceRun(RunData run) {
        try {
            ListenerAdapter.sendListeners(new RunUpdatedEvent(run.getId(), run));
        } catch (Exception e) {
            System.out.println("Could not announce run " + run.getId() + ": " + e.getMessage());
        }
    }

    /**
     * Hands out the prizes of one event and clears up after it.
     *
     * @param event the event that is over
     */
    public void settle(EventData event) {
        List<RunData> board = runs.getRunsOf(event.getId());

        // a cancelled event never really happened, so nobody is rewarded for it
        if (event.getState() != EventState.CANCELLED) {
            awardPlaces(event, board);
            awardParticipation(event, board);
        }
        discardServers(board);
        clearRuns(board);

        EventData settled = event.copy();
        settled.setApplied(true);
        EventStore.Result result = events.put(settled, false);
        if (result.successful()) {
            announceEvent(result.event().getId(), result.event());
        }
        System.out.println("Settled event " + event.getName() + " (" + board.size() + " runs)");
    }

    /**
     * Gives the first three finished runs their prize. Everybody on a winning run gets it, so a team of
     * four takes home four first prizes rather than a quarter each.
     *
     * @param event the event
     * @param board its runs, fastest first
     */
    private void awardPlaces(EventData event, List<RunData> board) {
        int place = 0;
        for (RunData run : board) {
            if (!run.isRanked()) continue;
            place++;
            if (place > PrizeData.PLACES) break;
            PrizeData prize = PrizeData.ofPlace(event, place);
            if (prize.isEmpty()) continue;
            for (UUID member : run.getParticipants()) {
                awards.put(new AwardData(member, event, place, prize));
            }
        }
    }

    /**
     * Gives everybody who took part their prize, once, no matter how often they ran.
     *
     * @param event the event
     * @param board its runs
     */
    private void awardParticipation(EventData event, List<RunData> board) {
        PrizeData prize = PrizeData.ofParticipation(event);
        if (prize.isEmpty()) return;
        Set<UUID> everybody = new LinkedHashSet<>();
        for (RunData run : board) everybody.addAll(run.getParticipants());
        for (UUID member : everybody) {
            awards.put(new AwardData(member, event, AwardData.PARTICIPATION, prize));
        }
    }

    /** How long a run server is given to shut down before its directory is removed. */
    private static final long SHUTDOWN_GRACE_MS = 30_000L;

    /**
     * Switches off the servers the runs were played on and throws their directories away.
     * <p>
     * A run server exists for one attempt at one event. Once that event is settled its world is of no use
     * to anybody, and a five day event with dozens of attempts would otherwise leave dozens of full
     * worlds sitting on the disk forever.
     *
     * @param board the runs of the event
     */
    private void discardServers(List<RunData> board) {
        Set<String> servers = stopServers(board);
        if (servers.isEmpty()) return;
        // the process needs a moment to let go of its files, so the directories go after a grace period
        new Timer("run-server-cleanup", true).schedule(new TimerTask() {
            @Override
            public void run() {
                for (String server : servers) discardServer(server);
            }
        }, SHUTDOWN_GRACE_MS);
    }

    /**
     * Switches the servers of a set of runs off and leaves their worlds where they are.
     * <p>
     * This is the half that is always safe: a stopped server costs nothing and can be started again under
     * the same name, which is exactly what continuing a run does. Throwing the directory away is the other
     * half, and it is only right once nobody can want the world back.
     *
     * @param board the runs whose servers should stop
     * @return the servers that were addressed
     */
    private Set<String> stopServers(List<RunData> board) {
        Set<String> servers = new LinkedHashSet<>();
        for (RunData run : board) {
            if (run.getServerName() != null) servers.add(run.getServerName());
        }
        for (String server : servers) {
            try {
                ListenerAdapter.ServerName name = ListenerAdapter.ServerName.valueOf(server);
                if (Main.getInstance().getServerHandler().doesInstanceExist(name)) {
                    Main.getInstance().getServerHandler().stop(name);
                }
            } catch (Exception e) {
                System.out.println("Could not stop the run server " + server + ": " + e.getMessage());
            }
        }
        return servers;
    }

    /**
     * Removes what is left of a run server: its directory and the entry that remembers its port.
     *
     * @param server the server to discard
     */
    private void discardServer(String server) {
        try {
            ListenerAdapter.ServerName name = ListenerAdapter.ServerName.valueOf(server);
            if (Main.getInstance().getServerHandler().doesInstanceExist(name)) {
                System.out.println("Run server " + server + " is still up - leaving its files alone.");
                return;
            }
            File directory = new File("./servers/" + server + "/");
            if (directory.exists() && !delete(directory)) {
                System.out.println("Could not remove the directory of " + server);
                return;
            }
            // the port is free again, and a stale entry would keep it reserved for a server that is gone
            YamlConfiguration config = Main.getInstance().getConfiguration().getConfig();
            config.set("servers." + server, null);
            Main.getInstance().getConfiguration().save();
            System.out.println("Discarded run server " + server);
        } catch (Exception e) {
            System.out.println("Could not discard the run server " + server + ": " + e.getMessage());
        }
    }

    /**
     * @param folder the directory to remove, with everything in it
     * @return whether it is gone
     */
    private static boolean delete(File folder) {
        try (Stream<Path> paths = Files.walk(folder.toPath())) {
            // deepest first, a directory can only go once it is empty
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    System.out.println("Could not delete " + path + ": " + e.getMessage());
                }
            });
        } catch (IOException e) {
            System.out.println("Could not walk " + folder + ": " + e.getMessage());
            return false;
        }
        return !folder.exists();
    }

    /**
     * Removes the runs of an event, telling the network about each one so no leaderboard keeps showing
     * results for an event that is finished and paid out.
     *
     * @param board the runs to remove
     */
    private void clearRuns(List<RunData> board) {
        for (RunData run : board) {
            if (!runs.delete(run.getId())) continue;
            try {
                ListenerAdapter.sendListeners(new RunUpdatedEvent(run.getId(), null));
            } catch (Exception e) {
                System.out.println("Could not announce the removal of run " + run.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Removes everything belonging to an event that was deleted outright.
     *
     * @param eventId the event that is gone
     */
    public void discard(UUID eventId) {
        clearRuns(runs.getRunsOf(eventId));
    }

    private static void announceEvent(UUID id, EventData event) {
        try {
            ListenerAdapter.sendListeners(new EventUpdatedEvent(id, event));
        } catch (Exception e) {
            System.out.println("Could not announce the event " + id + ": " + e.getMessage());
        }
    }
}

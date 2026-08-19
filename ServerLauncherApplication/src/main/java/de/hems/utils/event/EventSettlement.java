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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

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
            }
        }, CHECK_INTERVAL_MS, CHECK_INTERVAL_MS);
    }

    /**
     * Settles every event whose time is up and that has not been settled yet.
     */
    public void settleFinished() {
        for (EventData event : events.getEvents()) {
            if (event.isApplied()) continue;
            EventState state = event.getState();
            if (state != EventState.FINISHED && state != EventState.CANCELLED) continue;
            settle(event);
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
        stopServers(board);
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

    /**
     * Switches off the servers the runs were played on. Their worlds are of no use once the event is over.
     *
     * @param board the runs of the event
     */
    private void stopServers(List<RunData> board) {
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

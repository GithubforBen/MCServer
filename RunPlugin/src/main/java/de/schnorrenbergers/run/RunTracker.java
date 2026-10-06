package de.schnorrenbergers.run;

import de.hems.api.ServerApi;
import de.hems.communication.ListenerAdapter;
import de.hems.paper.event.EventService;
import de.hems.paper.event.RunQueue;
import de.hems.paper.event.RunService;
import de.hems.paper.warp.ServerConnector;
import de.hems.types.event.EventData;
import de.hems.types.event.RunData;
import de.hems.types.event.UhcObjective;
import de.hems.types.event.UhcSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Watches the run this server was created for.
 * <p>
 * The clock is counted in ticks, and only while somebody is actually playing. A team can log off in the
 * middle of an attempt and pick it up days later: the time in between is not part of their run. That is
 * also why the server is allowed to shut itself down once nobody has been on for a while - the world stays
 * on disk, and starting the server again continues the same run.
 * <p>
 * A run that is over is different: there is nothing to come back to, so the server goes the moment the
 * last player has left it.
 * <p>
 * A server can also be up before it has a run at all: the ghost server the launcher keeps ready for an
 * event, so the next run does not wait for a world to be generated. Until a run claims it, nothing here
 * counts, pauses or stops - the launcher looks after it.
 */
public class RunTracker implements Listener {

    /** How often the accumulated ticks are pushed to the launcher. */
    private static final long SYNC_TICKS = 20L * 5L;
    /** How long the server waits with nobody on it before it stops itself. */
    private static final long IDLE_SHUTDOWN_MS = 10L * 60L * 1000L;
    /** How long a finished run is left standing before everyone is sent back to the lobby. */
    private static final long RETURN_DELAY_TICKS = 20L * 15L;
    /** How long a run that was called off is left standing - there is nothing to look at. */
    private static final long ABORT_RETURN_DELAY_TICKS = 20L * 3L;
    /**
     * How long a team is kept here after a reset at most. Its next run is sent to the lobby the moment
     * its server is known, which takes seconds; this only catches the case that never happens.
     */
    private static final long MOVE_ON_TIMEOUT_TICKS = 20L * 30L;

    private final Plugin plugin;

    /** The run this server hosts. Looked up once and then held, since it never changes. */
    private RunData run;
    /** Ticks counted since the last sync, kept out of the run until they are written in. */
    private long pendingTicks;
    /** When the server last had a participant on it, for the idle shutdown. */
    private long lastActive = System.currentTimeMillis();
    /** Set once the shutdown has been asked for, so it is not asked for again every tick. */
    private boolean shuttingDown;
    /** Set once the end of the run has been announced here and everybody is on the way out. */
    private boolean ended;
    /**
     * Who is about to go on to their next run, after a reset. They are not sent to the lobby with
     * everybody else: they go once their next server is known, so the lobby can send them on to it.
     */
    private final Set<UUID> movingOn = new HashSet<>();

    public RunTracker(Plugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::sync, SYNC_TICKS, SYNC_TICKS);
    }

    /**
     * @return the run this server hosts, or {@code null} while it is not known yet
     */
    private RunData run() {
        if (run != null) return run;
        String self = ListenerAdapter.getName().toString();
        for (EventData event : EventService.getEvents()) {
            if (!event.getType().isTimed()) continue;
            for (RunData candidate : RunService.getRunsOf(event.getId())) {
                if (candidate.isOpen() && self.equals(candidate.getServerName())) {
                    run = candidate;
                    // a ghost can stand for an hour before it is claimed, and that hour is not its team
                    // being away - the idle shutdown starts counting now
                    lastActive = System.currentTimeMillis();
                    Bukkit.getScheduler().runTask(plugin, this::placePlayers);
                    return run;
                }
            }
        }
        return null;
    }

    private static EventData eventOf(RunData run) {
        return EventService.getEvent(run.getEventId());
    }

    /**
     * @return whether anybody who belongs to the run is online
     */
    private boolean hasParticipantOnline() {
        RunData current = run();
        if (current == null) return false;
        for (UUID member : current.getParticipants()) {
            if (Bukkit.getPlayer(member) != null) return true;
        }
        return false;
    }

    /**
     * @return whether this server hosts a run that is still open
     */
    public boolean hasOpenRun() {
        RunData current = run();
        return current != null && current.isOpen();
    }

    /**
     * @param player a player
     * @return whether they are on the run of this server
     */
    public boolean isParticipant(UUID player) {
        RunData current = run();
        return current != null && current.getParticipants().contains(player);
    }

    /**
     * @return whether this server has a run, open or already over
     */
    public boolean hasRun() {
        return run() != null;
    }

    /**
     * Calls the run off without a next one: it is closed without a time, everybody goes back to the lobby
     * and the server follows them out.
     *
     * @param by the name of who called it off
     * @return whether there was a run to call off
     */
    public boolean abort(String by) {
        RunData current = run();
        if (current == null || !current.isOpen()) return false;
        writePendingTicks(current);
        current.finish(RunData.State.ABANDONED);
        RunService.save(current);
        ended = true;
        broadcast(Component.text(by + " hat den Lauf abgebrochen.", NamedTextColor.YELLOW));
        returnToLobby(ABORT_RETURN_DELAY_TICKS);
        return true;
    }

    /**
     * Ends the run of this server and starts the next one for the team on it - what a speedrunner means by
     * a reset.
     * <p>
     * An open run is called off first and counts as an attempt like any other. A run that is already over -
     * somebody died, or it is done - is simply followed by the next one. The new run goes through the same
     * rules as the queue, and it takes the ghost server of the event when nobody else has, which is what
     * makes a reset quick. The team waits for its new server in the lobby rather than here, so this server
     * can switch itself off right away instead of holding its memory while the next world is generated.
     *
     * @param by the name of who asked for it
     * @return what to tell whoever asked, or {@code null} when the broadcast says it all
     */
    public String reset(String by) {
        RunData current = run();
        if (current == null) return "Auf diesem Server gibt es keinen Lauf.";
        if (!movingOn.isEmpty()) return "Euer neuer Lauf wird schon vorbereitet.";
        boolean wasOpen = current.isOpen();
        if (wasOpen) {
            writePendingTicks(current);
            current.finish(RunData.State.ABANDONED);
            RunService.save(current);
        }
        ended = true;
        broadcast(Component.text(by + (wasOpen ? " hat den Lauf abgebrochen - es geht von vorne los."
                : " startet einen neuen Lauf."), NamedTextColor.YELLOW));

        Set<UUID> team = new LinkedHashSet<>();
        for (UUID member : current.getParticipants()) {
            if (Bukkit.getPlayer(member) != null) team.add(member);
        }
        if (team.isEmpty()) {
            // called off from the console or by an admin with nobody of the team here - nobody to restart for
            returnToLobby(ABORT_RETURN_DELAY_TICKS);
            return null;
        }
        Map<UUID, String> left = new LinkedHashMap<>();
        String problem = RunQueue.restart(eventOf(current), team, left);
        for (Map.Entry<UUID, String> entry : left.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) player.sendMessage(Component.text(entry.getValue(), NamedTextColor.RED));
        }
        if (problem != null) {
            broadcast(Component.text(problem, NamedTextColor.RED));
            returnToLobby(ABORT_RETURN_DELAY_TICKS);
            return null;
        }
        team.removeAll(left.keySet());
        movingOn.addAll(team);
        // spectators and whoever can not come along go now, the team once its next server has a name
        returnToLobby(ABORT_RETURN_DELAY_TICKS);
        // if that never happens, the team must not be left standing on a server that is over
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            movingOn.clear();
            returnToLobby(0L);
        }, MOVE_ON_TIMEOUT_TICKS);
        return null;
    }

    /**
     * Stops the clock when the server goes down with the run still open - switched off by an admin, or
     * with the whole network. Nobody quits before a plugin is disabled, so without this the run would stay
     * "running" on a server that is off.
     */
    public void close() {
        RunData current = run;
        if (current == null || !current.isOpen()) return;
        if (current.getState() == RunData.State.PAUSED && pendingTicks <= 0) return;
        writePendingTicks(current);
        current.pause();
        RunService.saveNow(current);
    }

    /**
     * Takes over an end the run was given somewhere else: called off from the event panel, or given up
     * on by the launcher. What this server holds is its own copy, and it would otherwise keep counting.
     *
     * @param current the run as this server knows it
     */
    private void followTheNetwork(RunData current) {
        RunData known = RunService.getRun(current.getId());
        if (known == null || known == current || known.isOpen()) return;
        current.finish(known.getState());
        current.setFinishedAt(known.getFinishedAt());
    }

    /**
     * What is left to do once the run is over: say so if nothing here has yet, and switch the server off
     * as soon as it is empty.
     *
     * @param current the run, closed
     */
    private void afterTheEnd(RunData current) {
        if (!ended) {
            // it was not closed by anything that happened here, so nobody on this server has been told
            ended = true;
            pendingTicks = 0;
            broadcast(Component.text(current.getState() == RunData.State.ABANDONED
                    ? "Der Lauf wurde abgebrochen." : "Der Lauf ist vorbei.", NamedTextColor.YELLOW));
            returnToLobby(ABORT_RETURN_DELAY_TICKS);
        }
        if (!shuttingDown && Bukkit.getOnlinePlayers().isEmpty()) {
            shutDown(current, "The run is over and everybody has left");
        }
    }

    /**
     * One tick of the clock, plus the timer everybody sees and the idle shutdown.
     */
    private void tick() {
        RunData current = run();
        if (current == null) return;
        if (current.isOpen()) followTheNetwork(current);
        if (!current.isOpen()) {
            afterTheEnd(current);
            return;
        }

        if (hasParticipantOnline()) {
            lastActive = System.currentTimeMillis();
            if (current.getState() == RunData.State.PAUSED) {
                current.resume();
                broadcast(Component.text("Weiter geht's - die Zeit läuft wieder.", NamedTextColor.GREEN));
                sync();
            }
            // the clock only moves while the run is being played, which is the whole point of counting
            // ticks rather than looking at the wall clock
            pendingTicks++;
            showTimer(current);
            return;
        }

        if (current.getState() == RunData.State.RUNNING) {
            pause(current, "Niemand ist mehr da - die Zeit steht.");
        }
        if (!shuttingDown && System.currentTimeMillis() - lastActive > IDLE_SHUTDOWN_MS) {
            shutDown(current, "No participants for 10 minutes");
        }
    }

    /**
     * Stops the clock and writes it out.
     *
     * @param current the run
     * @param message what to say about it
     */
    private void pause(RunData current, String message) {
        writePendingTicks(current);
        current.pause();
        RunService.save(current);
        broadcast(Component.text(message, NamedTextColor.YELLOW));
    }

    /**
     * Asks the launcher to switch this server off. The world of an open run stays where it is, so the same
     * team can pick the run up again later and simply carry on.
     *
     * @param current the run
     * @param reason  why, for the log
     */
    private void shutDown(RunData current, String reason) {
        shuttingDown = true;
        if (current.isOpen()) {
            writePendingTicks(current);
            RunService.save(current);
        }
        String self = ListenerAdapter.getName().toString();
        Bukkit.getLogger().info(reason + " - stopping " + self);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                ServerApi.stopServer(self);
            } catch (Exception e) {
                Bukkit.getLogger().warning("Could not stop " + self + ": " + e.getMessage());
                shuttingDown = false;
            }
        });
    }

    /**
     * Moves the ticks counted since the last sync into the run.
     *
     * @param current the run
     */
    private void writePendingTicks(RunData current) {
        if (pendingTicks <= 0) return;
        current.addTicks(pendingTicks);
        pendingTicks = 0;
        // the estimate other servers extrapolate from starts again from this exact value
        if (current.getState() == RunData.State.RUNNING) current.resume();
    }

    /**
     * Pushes the clock to the launcher, so the leaderboard elsewhere is never far behind.
     */
    private void sync() {
        RunData current = run();
        if (current == null || !current.isOpen()) return;
        if (pendingTicks <= 0) return;
        writePendingTicks(current);
        RunService.save(current);
    }

    /**
     * Ticks off an objective when the right boss dies.
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        RunData current = run();
        if (current == null || current.getState() != RunData.State.RUNNING) return;
        UhcObjective objective = UhcObjective.byEntityType(event.getEntity().getType().name());
        if (objective == null) return;
        EventData eventData = eventOf(current);
        if (eventData == null) return;
        List<UhcObjective> required = UhcObjective.of(eventData.getType());
        if (!required.contains(objective) || !current.complete(objective)) return;

        broadcast(Component.text("✔ " + objective.getTitle() + " erledigt", NamedTextColor.GREEN));
        writePendingTicks(current);
        if (current.hasCompletedAll(required)) {
            current.finish(RunData.State.FINISHED);
            ended = true;
            broadcast(Component.text("Geschafft! Zeit: " + RunData.formatTicks(current.getElapsedTicks()),
                    NamedTextColor.GOLD));
            returnToLobby(RETURN_DELAY_TICKS);
        } else {
            StringBuilder left = new StringBuilder();
            for (UhcObjective open : current.getRemaining(required)) {
                if (!left.isEmpty()) left.append(", ");
                left.append(open.getTitle());
            }
            broadcast(Component.text("Noch offen: " + left, NamedTextColor.GRAY));
        }
        RunService.save(current);
    }

    /**
     * Ends the run when somebody dies and the event is hardcore.
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        RunData current = run();
        if (current == null || current.getState() != RunData.State.RUNNING) return;
        if (!current.getParticipants().contains(event.getPlayer().getUniqueId())) return;
        EventData eventData = eventOf(current);
        if (eventData == null || !new UhcSettings(eventData).isHardcore()) return;

        writePendingTicks(current);
        current.finish(RunData.State.FAILED);
        ended = true;
        RunService.save(current);
        broadcast(Component.text(event.getPlayer().getName() + " ist gestorben - der Lauf ist vorbei.",
                NamedTextColor.RED));
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (UUID member : current.getParticipants()) {
                Player online = Bukkit.getPlayer(member);
                if (online != null) online.setGameMode(GameMode.SPECTATOR);
            }
        });
        returnToLobby(RETURN_DELAY_TICKS);
    }

    /**
     * Sends everyone back to the lobby once the run is over.
     * <p>
     * Not straight away: the last thing that happened is worth a moment to look at, and the time needs to
     * be readable before the screen changes. Without this players are simply left standing on a server
     * that has nothing left for them.
     *
     * @param delay how many ticks to wait first
     */
    private void returnToLobby(long delay) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                // on the way to their next run, sent to the lobby by it
                if (movingOn.contains(player.getUniqueId())) continue;
                player.sendMessage(Component.text("Zurück in die Lobby ...", NamedTextColor.GRAY));
                ServerConnector.connect(player, ListenerAdapter.ServerName.LOBBY);
            }
        }, delay);
    }

    /**
     * Puts a joiner into the right mode. The clock itself is started by the tick task, so a run resumes
     * the moment somebody who belongs to it walks in.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        RunData current = run();
        Player player = event.getPlayer();
        if (current == null) {
            // a ghost, or a run server that has not heard of its run yet: its world belongs to whoever
            // claims it, untouched. The team is let loose the moment the run is known, see placePlayers()
            if (RunData.isRunServerName(ListenerAdapter.getName().toString())) {
                player.setGameMode(GameMode.SPECTATOR);
                player.sendMessage(Component.text("Dieser Server wartet noch auf seinen Lauf.",
                        NamedTextColor.GRAY));
            }
            return;
        }
        if (!current.getParticipants().contains(player.getUniqueId())) {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(Component.text("Hier läuft ein Versuch - du schaust zu.", NamedTextColor.GRAY));
            return;
        }
        EventData eventData = eventOf(current);
        if (eventData == null) return;
        player.sendMessage(Component.text(eventData.getName() + " - Zeit bisher: "
                + RunData.formatTicks(current.getElapsedTicks()), NamedTextColor.GOLD));
    }

    /**
     * Puts everybody who is already here into the right mode, the moment the run of this server becomes
     * known. That is the case on a ghost: the team is warped over as soon as it claimed it, and may be
     * standing here before the run has reached this server.
     */
    private void placePlayers() {
        RunData current = run;
        if (current == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean participant = current.getParticipants().contains(player.getUniqueId());
            if (participant && player.getGameMode() == GameMode.SPECTATOR) {
                player.setGameMode(GameMode.SURVIVAL);
                player.sendMessage(Component.text("Euer Lauf ist da - los geht's!", NamedTextColor.GREEN));
            } else if (!participant && player.getGameMode() != GameMode.SPECTATOR) {
                player.setGameMode(GameMode.SPECTATOR);
                player.sendMessage(Component.text("Hier läuft ein Versuch - du schaust zu.", NamedTextColor.GRAY));
            }
        }
    }

    /**
     * Stops the clock as soon as the last participant is gone, without waiting for the next tick to
     * notice - the quit event fires before the player is off the list.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        RunData current = run();
        if (current == null || current.getState() != RunData.State.RUNNING) return;
        if (!current.getParticipants().contains(event.getPlayer().getUniqueId())) return;
        for (UUID member : current.getParticipants()) {
            Player online = Bukkit.getPlayer(member);
            if (online != null && !online.getUniqueId().equals(event.getPlayer().getUniqueId())) return;
        }
        pause(current, "Niemand ist mehr da - die Zeit steht.");
    }

    /**
     * The clock above the hotbar, which is what a speedrun is played by.
     *
     * @param current the run
     */
    private void showTimer(RunData current) {
        EventData eventData = eventOf(current);
        if (eventData == null) return;
        int done = current.getCompleted().size();
        int total = UhcObjective.of(eventData.getType()).size();
        Component line = Component.text(RunData.formatTicks(current.getElapsedTicksRaw() + pendingTicks) + "  ",
                        NamedTextColor.GOLD)
                .append(Component.text(done + "/" + total + " Ziele", NamedTextColor.GRAY));
        for (Player player : Bukkit.getOnlinePlayers()) player.sendActionBar(line);
    }

    private static void broadcast(Component message) {
        for (Player player : Bukkit.getOnlinePlayers()) player.sendMessage(message);
    }
}

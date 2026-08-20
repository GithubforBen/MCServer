package de.schnorrenbergers.run;

import de.hems.api.ServerApi;
import de.hems.communication.ListenerAdapter;
import de.hems.paper.event.EventService;
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

import java.util.List;
import java.util.UUID;

/**
 * Watches the run this server was created for.
 * <p>
 * The clock is counted in ticks, and only while somebody is actually playing. A team can log off in the
 * middle of an attempt and pick it up days later: the time in between is not part of their run. That is
 * also why the server is allowed to shut itself down once nobody has been on for a while - the world stays
 * on disk, and starting the server again continues the same run.
 */
public class RunTracker implements Listener {

    /** How often the accumulated ticks are pushed to the launcher. */
    private static final long SYNC_TICKS = 20L * 5L;
    /** How long the server waits with nobody on it before it stops itself. */
    private static final long IDLE_SHUTDOWN_MS = 10L * 60L * 1000L;
    /** How long a finished run is left standing before everyone is sent back to the lobby. */
    private static final long RETURN_DELAY_TICKS = 20L * 15L;

    private final Plugin plugin;

    /** The run this server hosts. Looked up once and then held, since it never changes. */
    private RunData run;
    /** Ticks counted since the last sync, kept out of the run until they are written in. */
    private long pendingTicks;
    /** When the server last had a participant on it, for the idle shutdown. */
    private long lastActive = System.currentTimeMillis();
    /** Set once the shutdown has been asked for, so it is not asked for again every tick. */
    private boolean shuttingDown;

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
     * One tick of the clock, plus the timer everybody sees and the idle shutdown.
     */
    private void tick() {
        RunData current = run();
        if (current == null || !current.isOpen()) return;

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
            shutDown(current);
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
     * Asks the launcher to switch this server off. The world stays where it is, so the same team can pick
     * the run up again later and simply carry on.
     *
     * @param current the run
     */
    private void shutDown(RunData current) {
        shuttingDown = true;
        writePendingTicks(current);
        RunService.save(current);
        String self = ListenerAdapter.getName().toString();
        Bukkit.getLogger().info("No participants for 10 minutes - stopping " + self);
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
            broadcast(Component.text("Geschafft! Zeit: " + RunData.formatTicks(current.getElapsedTicks()),
                    NamedTextColor.GOLD));
            returnToLobby();
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
        RunService.save(current);
        broadcast(Component.text(event.getPlayer().getName() + " ist gestorben - der Lauf ist vorbei.",
                NamedTextColor.RED));
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (UUID member : current.getParticipants()) {
                Player online = Bukkit.getPlayer(member);
                if (online != null) online.setGameMode(GameMode.SPECTATOR);
            }
        });
        returnToLobby();
    }

    /**
     * Sends everyone back to the lobby once the run is over.
     * <p>
     * Not straight away: the last thing that happened is worth a moment to look at, and the time needs to
     * be readable before the screen changes. Without this players are simply left standing on a server
     * that has nothing left for them.
     */
    private void returnToLobby() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(Component.text("Zurück in die Lobby ...", NamedTextColor.GRAY));
                ServerConnector.connect(player, ListenerAdapter.ServerName.LOBBY);
            }
        }, RETURN_DELAY_TICKS);
    }

    /**
     * Puts a joiner into the right mode. The clock itself is started by the tick task, so a run resumes
     * the moment somebody who belongs to it walks in.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        RunData current = run();
        if (current == null) return;
        Player player = event.getPlayer();
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

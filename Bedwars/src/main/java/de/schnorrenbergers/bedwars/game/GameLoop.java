package de.schnorrenbergers.bedwars.game;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Level;

/**
 * The single repeating task of the plugin.
 * <p>
 * Generators, countdowns and the timeline all hang off this one tick instead of scheduling their own, so
 * that everything the round does happens in a known order and stops together with the round.
 * <p>
 * A phase that throws must not take the loop down with it: the round would freeze at whatever it was doing,
 * with no way back. The failure is logged once - repeating it sixty times a second would bury the stack
 * trace that actually explains it.
 */
public final class GameLoop {

    private final Plugin plugin;
    private final Game game;

    private BukkitTask task;
    private long ticks;
    private boolean reportedFailure;

    public GameLoop(Plugin plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
    }

    /**
     * Starts ticking, doing nothing when it is already running.
     */
    public void start() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void stop() {
        if (task == null) return;
        task.cancel();
        task = null;
    }

    public boolean isRunning() {
        return task != null;
    }

    public long getTicks() {
        return ticks;
    }

    private void tick() {
        ticks++;
        try {
            game.tickPhase(ticks);
            reportedFailure = false;
        } catch (Throwable failure) {
            if (reportedFailure) return;
            reportedFailure = true;
            plugin.getLogger().log(Level.SEVERE,
                    "The " + game.getPhaseType() + " phase failed. The round keeps ticking.", failure);
        }
    }
}

package de.schnorrenbergers.survival.featrues.chunklimiter;

import de.schnorrenbergers.survival.Survival;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Keeps the chunk limiter in step with players coming and going.
 * <p>
 * This replaces the old chunk load listener: reacting to every loaded chunk did the same work hundreds of
 * times a second, while a player only ever needs their distance set when they join.
 */
public class ChunkLimiterListener implements Listener {

    private static boolean registered = false;

    public ChunkLimiterListener() {
        if (registered) {
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, Survival.getInstance());
        registered = true;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        ChunkLimiter limiter = ChunkLimiter.getInstance();
        if (limiter == null) return;
        // a player that just paid should not have to wait a minute for the cache to expire
        PayingPlayers.invalidate();
        PayingPlayers.refreshIfDue();
        limiter.apply(event.getPlayer(), false);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ChunkLimiter limiter = ChunkLimiter.getInstance();
        if (limiter != null) limiter.forget(event.getPlayer().getUniqueId());
    }
}

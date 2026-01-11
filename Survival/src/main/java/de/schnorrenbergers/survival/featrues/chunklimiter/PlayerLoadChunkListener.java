package de.schnorrenbergers.survival.featrues.chunklimiter;

import de.schnorrenbergers.survival.Survival;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class PlayerLoadChunkListener implements Listener {

    private static boolean registered = false;

    public PlayerLoadChunkListener() {
        if (registered) {
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, Survival.getInstance());
        registered = true;
    }

    @EventHandler
    public void onLoad(ChunkLoadEvent event) {
        int maxChunks = 10;
        if (Bukkit.getServer().getTPS()[0] <= 15) {
            maxChunks -= 2;
            if (Bukkit.getServer().getTPS()[0] <= 10) {
                maxChunks -= 2;
                if (Bukkit.getServer().getTPS()[0] <= 5) {
                    maxChunks -= 2;
                    if (Bukkit.getServer().getTPS()[0] <= 3) {
                        maxChunks -= 2;
                    }
                }
            }
        }
        int finalMaxChunks = maxChunks;
        Bukkit.getOnlinePlayers().forEach((p) -> {
            try {
                if (!PayingPlayer.isPaying(p)) {
                    if (p.getViewDistance() != finalMaxChunks) {
                        p.setViewDistance(finalMaxChunks);
                        p.setSimulationDistance(finalMaxChunks);
                        p.sendMessage("You view and simulation distance has been set to " + finalMaxChunks + "!");
                        p.sendMessage("This is done to prevent server lag.");
                    }
                } else {
                    p.setViewDistance(12);
                    p.setSimulationDistance(12);
                }
            } catch (Exception e) {
                //throw new RuntimeException(e);
            }
        });
    }
}

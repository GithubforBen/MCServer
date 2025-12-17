package de.schnorrenbergers.survival.featrues.team;

import de.schnorrenbergers.survival.Survival;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.configuration.file.YamlConfiguration;

public class ClaimManager {
    protected static boolean claimChunk(Chunk chunk, String team) {
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            return false;
        }
        if (team == null) {
            return false;
        }
        YamlConfiguration config = Survival.getInstance().getTeamConfig().getConfig();
        if (config.contains("claims." + chunk.getWorld().getName() + "." + chunk.getX() + "." + chunk.getZ())) {
            return false;
        }
        config.set("claims." + chunk.getWorld().getName() + "." + chunk.getX() + "." + chunk.getZ(), team);
        return true;
    }

    public static boolean unclaimChunk(Chunk chunk, String team) {
        YamlConfiguration config = Survival.getInstance().getTeamConfig().getConfig();
        if (!config.contains("claims." + chunk.getWorld().getName() + "." + chunk.getX() + "." + chunk.getZ())) {
            return false;
        }
        if (!config.getString("claims." + chunk.getWorld().getName() + "." + chunk.getX() + "." + chunk.getZ()).equals(team)) {
            return false;
        }
        config.set("claims." + chunk.getWorld().getName() + "." + chunk.getX() + "." + chunk.getZ(), null);
        return true;
    }

    public static int getTeamChunkAmount(String team) {
        YamlConfiguration config = Survival.getInstance().getTeamConfig().getConfig();
        int amount = 0;
        for (Object value : config.getValues(true).values()) {
            if (value instanceof String) {
                if (value.equals(team)) {
                    amount++;
                }
            }
        }
        return amount;
    }

    public static String getTeamOfChunk(Chunk chunk) {
        YamlConfiguration config = Survival.getInstance().getTeamConfig().getConfig();
        if (!config.contains("claims." + chunk.getWorld().getName() + "." + chunk.getX() + "." + chunk.getZ())) {
            return null;
        }
        return config.getString("claims." + chunk.getWorld().getName() + "." + chunk.getX() + "." + chunk.getZ());
    }

}

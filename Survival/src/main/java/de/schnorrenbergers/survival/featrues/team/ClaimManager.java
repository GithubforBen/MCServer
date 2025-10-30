package de.schnorrenbergers.survival.featrues.team;

import de.schnorrenbergers.survival.Survival;
import org.bukkit.Chunk;
import org.bukkit.configuration.file.YamlConfiguration;

public class ClaimManager {
    public static boolean claimChunk(Chunk chunk, String name) {
        YamlConfiguration config = Survival.getInstance().getTeamConfig().getConfig();
        if (config.contains("claims." + chunk.getWorld().getName() + "." + chunk.getX() + "." + chunk.getZ())) {
            return false;
        }
        config.set("claims." + chunk.getWorld().getName() + "." + chunk.getX() + "." + chunk.getZ(), name);
        return true;
    }

    public static boolean unclaimChunk(Chunk chunk) {
        YamlConfiguration config = Survival.getInstance().getTeamConfig().getConfig();
        if (!config.contains("claims." + chunk.getWorld().getName() + "." + chunk.getX() + "." + chunk.getZ())) {
            return false;
        }
        config.set("claims." + chunk.getWorld().getName() + "." + chunk.getX() + "." + chunk.getZ(), null);
        return true;
    }

    public static String getTeamOfChunk(Chunk chunk) {
        YamlConfiguration config = Survival.getInstance().getTeamConfig().getConfig();
        if (!config.contains("claims." + chunk.getWorld().getName() + "." + chunk.getX() + "." + chunk.getZ())) {
            return null;
        }
        return config.getString("claims." + chunk.getWorld().getName() + "." + chunk.getX() + "." + chunk.getZ());
    }

}

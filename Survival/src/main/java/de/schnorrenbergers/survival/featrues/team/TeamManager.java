package de.schnorrenbergers.survival.featrues.team;

import de.schnorrenbergers.survival.Survival;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

public class TeamManager {
    private String name;
    public boolean claimChunk(Chunk chunk) {
        //TODO:check cost
        return ClaimManager.claimChunk(chunk, name);
    }
}

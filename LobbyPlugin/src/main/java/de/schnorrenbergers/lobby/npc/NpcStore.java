package de.schnorrenbergers.lobby.npc;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Keeps the npcs of the lobby in {@code npcs.yml}, next to the parkour and the lotto stand.
 * <p>
 * Only the settings are written - type, target, name, skin and spot. The npcs all stand in the one lobby
 * world, so the world is not written either: a lobby map that is swapped for a new one keeps its npcs.
 */
public final class NpcStore {

    private final File file;
    private final Logger logger;

    public NpcStore(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    /**
     * @param world the lobby world, which every npc stands in
     * @return every npc that was saved, by id, in the order they were saved in
     */
    public Map<String, LobbyNpc> load(World world) {
        Map<String, LobbyNpc> npcs = new LinkedHashMap<>();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("npcs");
        if (section == null) return npcs;
        for (String id : section.getKeys(false)) {
            ConfigurationSection npc = section.getConfigurationSection(id);
            if (npc == null) continue;
            NpcType type = NpcType.parse(npc.getString("type", ""));
            if (type == null) {
                logger.warning("The npc '" + id + "' has no type anybody knows - skipped.");
                continue;
            }
            Location location = new Location(world, npc.getDouble("x"), npc.getDouble("y"), npc.getDouble("z"),
                    (float) npc.getDouble("yaw"), 0f);
            npcs.put(id, new LobbyNpc(id, type, npc.getString("target"), npc.getString("name", id),
                    npc.getString("skin"), location));
        }
        return npcs;
    }

    /**
     * Writes every npc, replacing what was there.
     *
     * @param npcs the npcs of the lobby
     */
    public void save(Collection<LobbyNpc> npcs) {
        YamlConfiguration config = new YamlConfiguration();
        for (LobbyNpc npc : npcs) {
            String path = "npcs." + npc.getId() + ".";
            Location at = npc.getLocation();
            config.set(path + "type", npc.getType().name());
            config.set(path + "target", npc.getTarget());
            config.set(path + "name", npc.getName());
            config.set(path + "skin", npc.getSkin());
            config.set(path + "x", at.getX());
            config.set(path + "y", at.getY());
            config.set(path + "z", at.getZ());
            config.set(path + "yaw", at.getYaw());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            logger.warning("Could not save the npcs: " + e.getMessage());
        }
    }
}

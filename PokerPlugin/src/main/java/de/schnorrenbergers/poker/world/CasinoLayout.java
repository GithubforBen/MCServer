package de.schnorrenbergers.poker.world;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Where everything in the casino is, written down so it survives the world being edited.
 * <p>
 * The casino is built once by {@link CasinoBuilder} and belongs to whoever builds on it afterwards. That
 * only works if the plugin stops reading the building and reads this instead: a table is where this file
 * says it is, not wherever a green carpet happens to be. Move a table in the world, correct it here with
 * {@code /poker setup}, and everything - chairs, cards, chips, the floating heads - follows.
 */
public final class CasinoLayout {

    private static final String FILE = "configs/poker/layout.yml";

    private final File file;
    private final YamlConfiguration config;

    private String worldName = "casino";
    private final List<TableSpot> tables = new ArrayList<>();
    private double spawnX;
    private double spawnY;
    private double spawnZ;
    private float spawnYaw;
    /** The height of the floor people stand and sit on. */
    private double floorY;
    private boolean built;

    /**
     * @param plugin only for its logger - the file lives under {@code configs/poker/} next to the server,
     *               the way the bedwars configs do, so an admin finds all of them in one place rather than
     *               inside a plugin folder
     */
    public CasinoLayout(Plugin plugin) {
        this.file = new File(FILE);
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        this.config = YamlConfiguration.loadConfiguration(file);
        load();
    }

    private void load() {
        worldName = config.getString("world", "casino");
        built = config.getBoolean("built", false);
        floorY = config.getDouble("floor-y", 64d);
        spawnX = config.getDouble("spawn.x", 0.5d);
        spawnY = config.getDouble("spawn.y", 64d);
        spawnZ = config.getDouble("spawn.z", 0.5d);
        spawnYaw = (float) config.getDouble("spawn.yaw", 0d);
        tables.clear();
        ConfigurationSection section = config.getConfigurationSection("tables");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) continue;
            int index;
            try {
                index = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                continue;
            }
            TableSpot spot = new TableSpot(index, entry.getDouble("x"), entry.getDouble("y"),
                    entry.getDouble("z"), entry.getDouble("seat-radius", 4.5d));
            spot.layOutSeats(entry.getInt("seats", 8));
            tables.add(spot);
        }
        tables.sort(java.util.Comparator.comparingInt(TableSpot::getIndex));
    }

    public void save() {
        config.set("world", worldName);
        config.set("built", built);
        config.set("floor-y", floorY);
        config.set("spawn.x", spawnX);
        config.set("spawn.y", spawnY);
        config.set("spawn.z", spawnZ);
        config.set("spawn.yaw", spawnYaw);
        config.set("tables", null);
        for (TableSpot spot : tables) {
            String path = "tables." + spot.getIndex();
            config.set(path + ".x", spot.getCentreX());
            config.set(path + ".y", spot.getCentreY());
            config.set(path + ".z", spot.getCentreZ());
            config.set(path + ".seat-radius", spot.getSeatRadius());
            config.set(path + ".seats", spot.getSeatCount());
        }
        try {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            config.save(file);
        } catch (IOException e) {
            org.bukkit.Bukkit.getLogger().warning("Could not save the casino layout: " + e.getMessage());
        }
    }

    public boolean isBuilt() {
        return built;
    }

    public void setBuilt(boolean built) {
        this.built = built;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public double getFloorY() {
        return floorY;
    }

    public void setFloorY(double floorY) {
        this.floorY = floorY;
    }

    public List<TableSpot> getTables() {
        return tables;
    }

    /**
     * Replaces the tables, which is what building the casino and {@code /poker setup} both do.
     *
     * @param spots the tables as they now are
     */
    public void setTables(List<TableSpot> spots) {
        tables.clear();
        tables.addAll(spots);
    }

    public Location getSpawn(World world) {
        Location spawn = new Location(world, spawnX, spawnY, spawnZ);
        spawn.setYaw(spawnYaw);
        return spawn;
    }

    public void setSpawn(Location spawn) {
        this.spawnX = spawn.getX();
        this.spawnY = spawn.getY();
        this.spawnZ = spawn.getZ();
        this.spawnYaw = spawn.getYaw();
    }
}

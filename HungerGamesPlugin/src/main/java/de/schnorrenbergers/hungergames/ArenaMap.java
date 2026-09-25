package de.schnorrenbergers.hungergames;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.data.Directional;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The layout of the arena: where the middle is, where people start and which chests are the cornucopia.
 * <p>
 * A prepared map says so in a {@code hungergames.yml} inside its world folder. Everything in it is
 * optional, and a map without the file works too - the middle is then the world spawn, which is where
 * somebody building a map for this would put the cornucopia anyway:
 * <pre>
 * center: {x: 0, z: 0}
 * cornucopia-radius: 12   # chests this close to the middle get the cornucopia loot
 * spawn-radius: 22        # how far from the middle the start ring is
 * spawns: ["10,70,-4", ...] # fixed start places instead of the ring, optional
 * </pre>
 * {@code /hg mitte} writes the first three from where an admin is standing.
 * <p>
 * If there is no chest near the middle at all - a freshly generated world, or a map built without one - a
 * small cornucopia is built there, so the game always has its middle.
 */
public final class ArenaMap {

    /** The layout file, inside the world folder so it travels with the map. */
    public static final String FILE = "hungergames.yml";
    /** The map as it lies next to the launcher, relative to a server directory. */
    private static final String SHARED_MAP = "../../hungergames-world";

    private static final int DEFAULT_CORNUCOPIA_RADIUS = 12;

    private final World world;
    private final Location center;
    private final int cornucopiaRadius;
    private final int spawnRadius;
    private final List<Location> fixedSpawns = new ArrayList<>();
    private final List<Location> cornucopiaChests = new ArrayList<>();

    private ArenaMap(World world, Location center, int cornucopiaRadius, int spawnRadius) {
        this.world = world;
        this.center = center;
        this.cornucopiaRadius = cornucopiaRadius;
        this.spawnRadius = spawnRadius;
    }

    /**
     * Reads the layout of the main world and makes sure there is a cornucopia.
     *
     * @param plugin the plugin, for its logger
     * @return the layout
     */
    public static ArenaMap load(Plugin plugin) {
        World world = Bukkit.getWorlds().get(0);
        File file = new File(rootOf(world), FILE);
        YamlConfiguration config = file.isFile() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();

        Location spawn = world.getSpawnLocation();
        int x = config.getInt("center.x", spawn.getBlockX());
        int z = config.getInt("center.z", spawn.getBlockZ());
        int cornucopia = Math.max(4, config.getInt("cornucopia-radius", DEFAULT_CORNUCOPIA_RADIUS));
        int ring = Math.max(cornucopia + 4, config.getInt("spawn-radius", cornucopia + 10));
        Location center = surface(world, x, z);
        ArenaMap map = new ArenaMap(world, center, cornucopia, ring);

        for (String spec : config.getStringList("spawns")) {
            String[] parts = spec.split(",");
            if (parts.length != 3) continue;
            try {
                map.fixedSpawns.add(new Location(world, Double.parseDouble(parts[0].trim()) + 0.5,
                        Double.parseDouble(parts[1].trim()), Double.parseDouble(parts[2].trim()) + 0.5));
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Unreadable spawn in " + FILE + ": " + spec);
            }
        }

        map.findChests();
        if (map.cornucopiaChests.isEmpty()) {
            plugin.getLogger().info("No chest near the middle - building a cornucopia at "
                    + center.getBlockX() + ", " + center.getBlockY() + ", " + center.getBlockZ() + ".");
            map.build();
        }
        plugin.getLogger().info("Arena: middle at " + center.getBlockX() + "/" + center.getBlockZ() + ", "
                + map.cornucopiaChests.size() + " cornucopia chests, "
                + (map.fixedSpawns.isEmpty() ? "start ring at " + ring + " blocks" : map.fixedSpawns.size()
                + " fixed start places") + ".");
        return map;
    }

    /**
     * The folder the world was copied in as - the one holding {@code level.dat}.
     * <p>
     * On 26.2 a world keeps its dimensions in {@code dimensions/minecraft/...} below that folder, and what
     * {@link World#getWorldFolder()} points at is not something to rely on across versions. The layout file
     * lies next to {@code level.dat}, because that is where somebody preparing a map puts it, so this walks up
     * from wherever the server says the world is until it finds that folder.
     *
     * @param world the world
     * @return its root folder
     */
    static File rootOf(World world) {
        File folder = world.getWorldFolder();
        for (int depth = 0; folder != null && depth < 4; depth++) {
            if (new File(folder, "level.dat").isFile()) return folder;
            folder = folder.getParentFile();
        }
        return new File(Bukkit.getWorldContainer(), world.getName());
    }

    /**
     * @param world the world
     * @param x     block x
     * @param z     block z
     * @return the first free block above the ground there, ignoring leaves so nobody starts in a tree top
     */
    static Location surface(World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        return new Location(world, x + 0.5, y + 1, z + 0.5);
    }

    private void findChests() {
        cornucopiaChests.clear();
        int minChunkX = (center.getBlockX() - cornucopiaRadius) >> 4;
        int maxChunkX = (center.getBlockX() + cornucopiaRadius) >> 4;
        int minChunkZ = (center.getBlockZ() - cornucopiaRadius) >> 4;
        int maxChunkZ = (center.getBlockZ() + cornucopiaRadius) >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                Chunk chunk = world.getChunkAt(cx, cz);
                for (BlockState state : chunk.getTileEntities()) {
                    if (!(state instanceof Chest)) continue;
                    if (isCornucopia(state.getLocation())) cornucopiaChests.add(state.getLocation());
                }
            }
        }
    }

    /**
     * @param location a block
     * @return whether it is close enough to the middle to count as the cornucopia
     */
    public boolean isCornucopia(Location location) {
        if (location.getWorld() != world) return false;
        double dx = location.getBlockX() + 0.5 - center.getX();
        double dz = location.getBlockZ() + 0.5 - center.getZ();
        return dx * dx + dz * dz <= (double) cornucopiaRadius * cornucopiaRadius;
    }

    /**
     * Builds a small cornucopia at the middle: a round stone floor, a golden horn in the middle, and a ring
     * of chests facing it.
     */
    private void build() {
        int cx = center.getBlockX();
        int cz = center.getBlockZ();
        int floor = center.getBlockY() - 1;
        int radius = 7;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                // clear what stands on the spot, so a tree or a hill does not cut through the horn
                for (int dy = 1; dy <= 6; dy++) world.getBlockAt(cx + dx, floor + dy, cz + dz).setType(Material.AIR, false);
                world.getBlockAt(cx + dx, floor, cz + dz).setType(
                        dx * dx + dz * dz >= (radius - 1) * (radius - 1) ? Material.STONE_BRICKS
                                : Material.POLISHED_ANDESITE, false);
                // and fill below it, so it does not hang over a hole or a river
                for (int dy = 1; dy <= 4; dy++) {
                    Block below = world.getBlockAt(cx + dx, floor - dy, cz + dz);
                    if (below.getType().isAir() || below.isLiquid()) below.setType(Material.DIRT, false);
                }
            }
        }
        // the horn: a golden column that bends outwards, open to the north
        world.getBlockAt(cx, floor + 1, cz).setType(Material.GOLD_BLOCK, false);
        world.getBlockAt(cx, floor + 2, cz).setType(Material.GOLD_BLOCK, false);
        world.getBlockAt(cx, floor + 3, cz).setType(Material.HAY_BLOCK, false);
        world.getBlockAt(cx, floor + 3, cz + 1).setType(Material.HAY_BLOCK, false);
        world.getBlockAt(cx, floor + 4, cz + 1).setType(Material.GOLD_BLOCK, false);
        world.getBlockAt(cx, floor + 4, cz + 2).setType(Material.GOLD_BLOCK, false);

        int[][] ring = {{3, 0}, {-3, 0}, {0, 3}, {0, -3}, {2, 2}, {-2, 2}, {2, -2}, {-2, -2},
                {5, 1}, {-5, -1}, {1, -5}, {-1, 5}};
        for (int[] offset : ring) {
            Block block = world.getBlockAt(cx + offset[0], floor + 1, cz + offset[1]);
            block.setType(Material.CHEST, false);
            if (block.getBlockData() instanceof Directional directional) {
                directional.setFacing(facingTowards(offset[0], offset[1]));
                block.setBlockData(directional, false);
            }
            cornucopiaChests.add(block.getLocation());
        }
        world.getBlockAt(cx + 6, floor + 1, cz).setType(Material.SEA_LANTERN, false);
        world.getBlockAt(cx - 6, floor + 1, cz).setType(Material.SEA_LANTERN, false);
        world.getBlockAt(cx, floor + 1, cz + 6).setType(Material.SEA_LANTERN, false);
        world.getBlockAt(cx, floor + 1, cz - 6).setType(Material.SEA_LANTERN, false);
    }

    /**
     * @return the face of a chest at that offset that looks at the middle
     */
    private static BlockFace facingTowards(int dx, int dz) {
        if (Math.abs(dx) >= Math.abs(dz)) return dx > 0 ? BlockFace.WEST : BlockFace.EAST;
        return dz > 0 ? BlockFace.NORTH : BlockFace.SOUTH;
    }

    /**
     * @param count how many start places are needed
     * @return that many, spread evenly on the ring and looking at the middle - or the fixed ones of the map,
     *         repeated if there are more players than places
     */
    public List<Location> spawns(int count) {
        List<Location> spawns = new ArrayList<>();
        if (!fixedSpawns.isEmpty()) {
            for (int i = 0; i < count; i++) spawns.add(face(fixedSpawns.get(i % fixedSpawns.size()).clone()));
            return spawns;
        }
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / Math.max(1, count);
            int x = center.getBlockX() + (int) Math.round(Math.cos(angle) * spawnRadius);
            int z = center.getBlockZ() + (int) Math.round(Math.sin(angle) * spawnRadius);
            spawns.add(face(surface(world, x, z)));
        }
        return spawns;
    }

    private Location face(Location location) {
        Location look = location.clone();
        look.setDirection(center.toVector().subtract(location.toVector()));
        look.setPitch(0);
        return look;
    }

    /**
     * Writes the middle into the layout file of this world and, if the map lies next to the launcher, into
     * that copy too - which is the one the next arena gets.
     *
     * @param where the new middle
     * @return what happened, to be told to whoever asked
     */
    public static String saveCenter(Location where) {
        List<File> targets = new ArrayList<>();
        targets.add(new File(rootOf(where.getWorld()), FILE));
        File shared = new File(SHARED_MAP);
        if (new File(shared, "level.dat").isFile()) targets.add(new File(shared, FILE));
        for (File file : targets) {
            YamlConfiguration config = file.isFile() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
            config.set("center.x", where.getBlockX());
            config.set("center.z", where.getBlockZ());
            if (!config.contains("cornucopia-radius")) config.set("cornucopia-radius", DEFAULT_CORNUCOPIA_RADIUS);
            if (!config.contains("spawn-radius")) config.set("spawn-radius", DEFAULT_CORNUCOPIA_RADIUS + 10);
            try {
                config.save(file);
            } catch (IOException e) {
                return "Konnte " + file.getPath() + " nicht schreiben: " + e.getMessage();
            }
        }
        return targets.size() > 1
                ? "Mitte gespeichert - auch in der Karte beim Launcher, das nächste Event nutzt sie."
                : "Mitte gespeichert, aber nur auf diesem Server: beim Launcher liegt keine Karte "
                + "in ./hungergames-world.";
    }

    public World getWorld() {
        return world;
    }

    public Location getCenter() {
        return center.clone();
    }

    public List<Location> getCornucopiaChests() {
        return cornucopiaChests;
    }
}

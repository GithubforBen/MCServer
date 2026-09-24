package de.schnorrenbergers.poker.world;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Random;
import java.util.stream.Stream;

/**
 * The one world the casino stands in.
 * <p>
 * It is generated exactly once, in code, and then it is a world like any other. What makes that useful
 * rather than a trap is where it is kept: a poker night gets a fresh server every time, so a world that
 * only ever lived on the server would be thrown away with it and generated again the next evening, and
 * every change anybody made to it would be gone. So the world is copied out to {@code ./poker-world} next
 * to the launcher when it is built and copied back in when a new casino starts. Build on it, and the next
 * poker night is played in the room you built.
 * <p>
 * {@code /poker karte speichern} does that copy on demand, which is what somebody who has just spent an
 * hour rearranging the tables wants.
 */
public final class CasinoWorld {

    /** What the world is called on this server. */
    public static final String WORLD_NAME = "casino";
    /** The layout of the tables, kept next to the world so it travels with it. */
    public static final String LAYOUT_FILE = "layout.yml";
    /** Where the casino is kept between servers, relative to a server directory. */
    public static final String SHARED_SOURCE = "../../poker-world";
    /** And where a server keeps its own copy of that, so a start with no shared folder still works. */
    private static final String LOCAL_SOURCE = "poker-world";

    private static World world;

    private CasinoWorld() {
    }

    /**
     * Puts the casino world in place and hands it back.
     * <p>
     * Three cases, in this order: a world that is already here is used as it stands, a copy lying next to
     * the launcher is brought in, and only when there is neither does anything get built.
     *
     * @param plugin the plugin, for its logger
     * @param layout where the tables are recorded
     * @param tables how many tables to build, if it comes to building
     * @param seats  how many chairs each of them gets
     * @return the world, or {@code null} if it could not be made
     */
    public static World load(Plugin plugin, CasinoLayout layout, int tables, int seats) {
        String name = layout.getWorldName() == null ? WORLD_NAME : layout.getWorldName();

        if (!new File(name, "level.dat").isFile()) {
            File source = firstExisting(new File(SHARED_SOURCE), new File(LOCAL_SOURCE));
            if (source != null) {
                plugin.getLogger().info("Bringing the casino in from " + source.getPath() + ".");
                try {
                    copyTree(source.toPath(), new File(name).toPath());
                } catch (IOException e) {
                    plugin.getLogger().warning("The casino could not be copied in ("
                            + e.getMessage() + ") - a fresh one is built instead.");
                }
            }
        }

        world = Bukkit.getWorld(name);
        if (world == null) {
            world = Bukkit.createWorld(new WorldCreator(name)
                    .type(WorldType.FLAT)
                    .generateStructures(false)
                    .generator(new EmptyGenerator()));
        }
        if (world == null) {
            plugin.getLogger().severe("The casino world could not be created - nothing can be played here.");
            return null;
        }

        settle(world);

        if (!layout.isBuilt() || layout.getTables().isEmpty()) {
            plugin.getLogger().info("There is no casino here yet - building one with "
                    + tables + " tables.");
            CasinoBuilder.build(world, tables, seats, layout);
            // straight out to the shared folder, so the first evening's room is already the one that comes
            // back next time rather than being generated again from scratch. One tick later, once the server
            // is up: while plugins load, the main world has not written the level.dat the copy needs yet
            Bukkit.getScheduler().runTask(plugin, () -> export(plugin));
        }
        return world;
    }

    /**
     * @return the world, or {@code null} before it was loaded
     */
    public static World get() {
        return world;
    }

    /**
     * Copies the casino out to where the next poker night will find it.
     *
     * @param plugin the plugin, for its logger
     * @return what happened, to be told to whoever asked
     */
    public static String export(Plugin plugin) {
        if (world == null) return "Es gibt noch keine Casino-Welt.";
        // everything that is only in memory goes to disk first, or the copy is of yesterday's room
        world.save();
        File target = new File(SHARED_SOURCE);
        try {
            if (target.exists()) deleteTree(target.toPath());
            // since 26.2 an extra world lives as a dimension inside the main world, not in a folder of its
            // own name - the server says where, and that is the only place worth asking
            copyTree(world.getWorldFolder().toPath(), target.toPath());
            // a dimension folder has no level.dat, and without one the copy is not a world that the next
            // casino can import. The main world's stands in; the generator is set in code anyway
            File layout = new File("configs/poker/layout.yml");
            if (layout.isFile()) {
                Files.copy(layout.toPath(), new File(target, LAYOUT_FILE).toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            File levelDat = new File(target, "level.dat");
            // on a fresh server the main world has not written its level.dat yet - saving it does
            World main = Bukkit.getWorlds().getFirst();
            main.save();
            File mainLevel = levelDatAbove(main.getWorldFolder());
            if (!levelDat.isFile() && mainLevel != null) {
                Files.copy(mainLevel.toPath(), levelDat.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            if (!levelDat.isFile()) {
                // without it the next casino cannot import the world, and would take over a layout that
                // says "built" for an empty room - so the layout goes too, and the next night builds anew
                new File(target, LAYOUT_FILE).delete();
                plugin.getLogger().warning("The casino was copied out without a level.dat - the next poker "
                        + "night will build its own.");
            }
            // a copied world that keeps its session lock and its player data is a world that argues with
            // the server it is copied into
            new File(target, "session.lock").delete();
            new File(target, "uid.dat").delete();
            deleteQuietly(new File(target, "playerdata"));
            deleteQuietly(new File(target, "stats"));
            deleteQuietly(new File(target, "advancements"));
            plugin.getLogger().info("The casino was written to " + target.getPath() + ".");
            return "Die Casino-Welt liegt jetzt in " + target.getPath()
                    + " - die nächste Pokernacht spielt darin.";
        } catch (IOException e) {
            plugin.getLogger().warning("The casino could not be written out: " + e.getMessage());
            return "Konnte nicht gespeichert werden: " + e.getMessage();
        }
    }

    /**
     * Makes the world behave like a room rather than like a world: no weather, no time, no mobs, no
     * hunger, and nothing that falls out of it.
     */
    private static void settle(World world) {
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setTime(18000L);
        world.setStorm(false);
        world.setThundering(false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.MOB_GRIEFING, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.FALL_DAMAGE, false);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
    }

    /**
     * Sends somebody to the door of the casino.
     *
     * @param player who to send
     * @param layout where the door is
     */
    public static void sendToSpawn(Player player, CasinoLayout layout) {
        if (world == null) return;
        Location spawn = layout.getSpawn(world);
        player.teleport(spawn);
    }

    /**
     * @param folder where to start
     * @return the {@code level.dat} in that folder or one of the few above it, or {@code null}
     */
    private static File levelDatAbove(File folder) {
        for (int depth = 0; folder != null && depth < 4; depth++) {
            File levelDat = new File(folder, "level.dat");
            if (levelDat.isFile()) return levelDat;
            folder = folder.getParentFile();
        }
        return null;
    }

    private static File firstExisting(File... candidates) {
        for (File candidate : candidates) {
            if (candidate != null && new File(candidate, "level.dat").isFile()) return candidate;
        }
        return null;
    }

    private static void deleteQuietly(File file) {
        if (!file.exists()) return;
        try {
            deleteTree(file.toPath());
        } catch (IOException ignored) {
            // leftover player data in the copy is untidy, not broken
        }
    }

    private static void copyTree(Path from, Path to) throws IOException {
        try (Stream<Path> paths = Files.walk(from)) {
            for (Path path : paths.toList()) {
                String relative = from.relativize(path).toString();
                // a live world holds its lock file open, and copying it is what makes the copy refuse to load
                if (relative.equals("session.lock")) continue;
                Path destination = to.resolve(relative);
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                    continue;
                }
                Files.createDirectories(destination.getParent());
                Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void deleteTree(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (Stream<Path> paths = Files.walk(path)) {
            for (Path entry : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(entry);
            }
        }
    }

    /**
     * A generator that generates nothing.
     * <p>
     * The casino is a built room in an empty world - terrain around it would only be something to fall out
     * of, and generating it costs memory a small server does not have to spend.
     */
    public static final class EmptyGenerator extends ChunkGenerator {

        @Override
        public void generateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ,
                                  ChunkData chunkData) {
            // nothing, on purpose
        }

        @Override
        public boolean shouldGenerateCaves() {
            return false;
        }

        @Override
        public boolean shouldGenerateDecorations() {
            return false;
        }

        @Override
        public boolean shouldGenerateMobs() {
            return false;
        }

        @Override
        public boolean shouldGenerateStructures() {
            return false;
        }
    }
}

package de.schnorrenbergers.bedwars.map;

import de.hems.files.FileTrees;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Brings a map into the server as a world, and puts it back when it was set up.
 * <p>
 * The round is never played in the map itself but in a copy called {@code arena_<name>}. That is what makes
 * the reset free: the copy is thrown away with the server, and the map you downloaded is untouched no
 * matter what a round did to it.
 * <p>
 * <b>Where a world lives has changed.</b> On 26.2 an extra world is a dimension inside the main world
 * ({@code world/dimensions/minecraft/arena_<name>}), not a folder of its own next to it. Copying a map in
 * still works, because the server imports a folder in the old shape - which is also what makes downloaded
 * maps work at all, since those are all in the old shape. But the copy is <em>moved</em> in the process, so
 * nothing may assume it can find the world again where it put it: {@link World#getWorldFolder()} is the
 * only honest answer, and it is what writing a map back reads from.
 */
public final class MapLoader {

    /** The folders that hold the world itself, replaced wholesale when a map is written back. */
    private static final List<String> DATA_FOLDERS = List.of("region", "entities", "poi", "data");

    /** What the copy of a map is called while it is being played. */
    public static final String ARENA_PREFIX = "arena_";

    private final MapRepository repository;

    public MapLoader(MapRepository repository) {
        this.repository = repository;
    }

    /**
     * @param name the map
     * @return the name its copy is loaded under
     */
    public static String arenaName(String name) {
        return ARENA_PREFIX + name;
    }

    /**
     * Copies a map into the server and loads it.
     *
     * @param name the map
     * @return the loaded world, or {@code null} when the map has no world folder or the copy failed
     */
    public @Nullable World load(String name) {
        if (!repository.hasWorld(name)) return null;
        String arena = arenaName(name);

        World loaded = Bukkit.getWorld(arena);
        if (loaded != null) return loaded;

        File dropOff = new File(Bukkit.getWorldContainer(), arena);
        try {
            // both places the world could still be lying around from an earlier run: the folder the copy
            // goes into, and wherever the server moved the last one to. A leftover would be loaded instead
            // of the map, which looks exactly like the map having the wrong blocks in it
            FileTrees.delete(dropOff.toPath());
            FileTrees.delete(dimensionFolder(arena).toPath());
            FileTrees.copy(repository.worldFolder(name).toPath(), dropOff.toPath(), FileTrees.WORLD_IDENTITY);
        } catch (IOException e) {
            Bukkit.getLogger().warning("[Bedwars] Could not copy the map " + name + ": " + e.getMessage());
            return null;
        }

        return new WorldCreator(arena)
                .environment(Environment.NORMAL)
                .generator(new VoidChunkGenerator())
                .generateStructures(false)
                .createWorld();
    }

    /**
     * Writes the world of a setup session back over the map it came from.
     * <p>
     * Only called from {@code /bw setup save}, never on its own: a map is something you built, and it
     * changes when you say so rather than because somebody blew a hole into the arena.
     * <p>
     * The map keeps its own {@code level.dat}. A dimension folder does not have one, and without it the map
     * would stop being a world folder that can be copied back in.
     *
     * @param name  the map
     * @param world the arena that was worked in
     * @return whether it could be written back
     */
    public boolean saveBack(String name, World world) {
        world.save();
        Path source = world.getWorldFolder().toPath();
        Path target = repository.worldFolder(name).toPath();
        try {
            for (String folder : DATA_FOLDERS) {
                FileTrees.delete(target.resolve(folder));
            }
            FileTrees.copy(source, target, FileTrees.WORLD_IDENTITY);
            return true;
        } catch (IOException e) {
            Bukkit.getLogger().warning("[Bedwars] Could not write the map " + name + " back: " + e.getMessage());
            return false;
        }
    }

    /**
     * Works out where the server would keep a world of this name, without assuming the layout.
     * <p>
     * Whatever the main world's folder sits in is where the others sit too - a folder next to it on the old
     * layout, a dimension next to it on the new one.
     *
     * @param arena the world name
     * @return the folder it would live in
     */
    private static File dimensionFolder(String arena) {
        List<World> worlds = Bukkit.getWorlds();
        if (worlds.isEmpty()) return new File(Bukkit.getWorldContainer(), arena);
        File beside = worlds.getFirst().getWorldFolder().getParentFile();
        return beside == null ? new File(Bukkit.getWorldContainer(), arena) : new File(beside, arena);
    }

}

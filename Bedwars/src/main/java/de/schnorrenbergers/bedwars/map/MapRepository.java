package de.schnorrenbergers.bedwars.map;

import de.schnorrenbergers.bedwars.game.GameMode;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The maps that lie on this server.
 * <p>
 * A map is two things next to each other under {@code maps/}: the world folder you downloaded, and a yaml
 * file with the points that were set in it. They are kept apart because they are edited by completely
 * different means - one with a world editor, one with {@code /bw setup}.
 */
public final class MapRepository {

    /** Where the maps live, next to the server jar. */
    public static final String DIRECTORY = "./maps";

    private final File directory;

    public MapRepository() {
        this(new File(DIRECTORY));
    }

    public MapRepository(File directory) {
        this.directory = directory;
        if (!directory.exists()) directory.mkdirs();
    }

    /**
     * @return every map that has a world folder, whether or not it has been set up yet
     */
    public List<String> list() {
        File[] entries = directory.listFiles();
        if (entries == null) return List.of();
        List<String> names = new ArrayList<>();
        for (File entry : entries) {
            if (!entry.isDirectory()) continue;
            if (!new File(entry, "level.dat").isFile()) continue;
            names.add(entry.getName().toLowerCase(Locale.ROOT));
        }
        Collections.sort(names);
        return names;
    }

    /**
     * @param name the map
     * @return whether a world folder for it exists
     */
    public boolean hasWorld(String name) {
        return new File(worldFolder(name), "level.dat").isFile();
    }

    /**
     * @param name the map
     * @return its world folder, whether or not it is there
     */
    public File worldFolder(String name) {
        return new File(directory, name.toLowerCase(Locale.ROOT));
    }

    /**
     * @param name the map
     * @return the file its points are written in
     */
    public File definitionFile(String name) {
        return new File(directory, name.toLowerCase(Locale.ROOT) + ".yml");
    }

    /**
     * @param name the map
     * @return it, with an empty definition when it has never been set up
     */
    public ArenaMap load(String name) {
        File file = definitionFile(name);
        if (!file.isFile()) return new ArenaMap(name);
        return ArenaMap.read(name, YamlConfiguration.loadConfiguration(file));
    }

    /**
     * @param map the map to write down
     * @return whether it could be written
     */
    public boolean save(ArenaMap map) {
        try {
            map.write().save(definitionFile(map.getName()));
            return true;
        } catch (IOException e) {
            Bukkit.getLogger().warning("[Bedwars] Could not save the map " + map.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Picks a map to play.
     *
     * @param wanted what the config or the event asked for, may be blank
     * @param mode   the mode to be played
     * @return a map that can host it, or {@code null} when there is none
     */
    public @Nullable ArenaMap pick(String wanted, GameMode mode) {
        if (wanted != null && !wanted.isBlank()) {
            if (!hasWorld(wanted)) return null;
            ArenaMap map = load(wanted);
            // a named map is used even when it does not fit the mode - saying so is the validator's job,
            // and silently playing a different map than the one that was asked for is worse
            return map;
        }
        List<String> candidates = new ArrayList<>();
        for (String name : list()) {
            if (load(name).supports(mode)) candidates.add(name);
        }
        if (candidates.isEmpty()) return null;
        return load(candidates.get((int) (Math.random() * candidates.size())));
    }

    public File getDirectory() {
        return directory;
    }
}

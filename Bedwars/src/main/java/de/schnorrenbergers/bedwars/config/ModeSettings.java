package de.schnorrenbergers.bedwars.config;

import de.schnorrenbergers.bedwars.game.GameMode;
import de.schnorrenbergers.bedwars.util.ConfigFile;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The modes a round can be played in, out of {@code modes.yml}.
 * <p>
 * The four hypixel modes are written into the file as defaults rather than kept in code, so an event that
 * wants two teams of eight is a new block in the file and not a new enum constant.
 */
public final class ModeSettings {

    /** The mode used when nothing else is asked for. */
    public static final String DEFAULT_MODE = "quad";

    private final ConfigFile file;
    private final Map<String, GameMode> modes = new LinkedHashMap<>();

    public ModeSettings() {
        file = new ConfigFile("modes.yml");
        load();
    }

    /**
     * Reads the file, writing the four standard modes into it when they are missing.
     */
    public void load() {
        file.reload();
        modes.clear();
        file.section("modes",
                "Every block below is a mode: how many teams play and how many players fit into one.",
                "Add your own - a mode is nothing but these two numbers plus a name.");
        define("solo", "Solo", 8, 1);
        define("doubles", "Doubles", 8, 2);
        define("trio", "3v3v3v3", 4, 3);
        define("quad", "4v4v4v4", 4, 4);
        for (String id : file.keys("modes")) {
            String path = "modes." + id;
            String display = file.get(path + ".display-name", id);
            int teams = file.get(path + ".teams", 4);
            int size = file.get(path + ".team-size", 4);
            modes.put(id.toLowerCase(Locale.ROOT), new GameMode(id.toLowerCase(Locale.ROOT), display, teams, size));
        }
        file.save();
    }

    /**
     * Writes one of the standard modes into the file, without touching it when it is already there.
     *
     * @param id       the key
     * @param display  what it is called
     * @param teams    how many teams
     * @param teamSize how many players per team
     */
    private void define(String id, String display, int teams, int teamSize) {
        String path = "modes." + id;
        file.get(path + ".display-name", display);
        file.get(path + ".teams", teams);
        file.get(path + ".team-size", teamSize);
    }

    /**
     * @param id the key from the config, a command or an event
     * @return the mode, or the default one when there is none by that name
     */
    public GameMode get(String id) {
        if (id == null || id.isBlank()) return getDefault();
        GameMode mode = modes.get(id.toLowerCase(Locale.ROOT));
        return mode == null ? getDefault() : mode;
    }

    /**
     * @param id the key to look for
     * @return whether a mode by that name exists
     */
    public boolean has(String id) {
        return id != null && modes.containsKey(id.toLowerCase(Locale.ROOT));
    }

    /**
     * @return the mode used when nothing else is asked for, or any mode at all if even that is missing
     */
    public GameMode getDefault() {
        GameMode mode = modes.get(DEFAULT_MODE);
        if (mode != null) return mode;
        return modes.values().stream().findFirst()
                .orElseGet(() -> new GameMode(DEFAULT_MODE, "4v4v4v4", 4, 4));
    }

    public Collection<GameMode> all() {
        return modes.values();
    }
}

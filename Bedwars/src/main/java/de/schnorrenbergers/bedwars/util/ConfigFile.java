package de.schnorrenbergers.bedwars.util;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One yaml file that writes itself.
 * <p>
 * Every value is read through {@link #get(String, Object, String...)}, which puts the default and a
 * comment into the file the first time it is missing. A fresh server therefore ends up with configs that
 * list and explain every knob, instead of empty files nobody can guess the keys of - the same way
 * {@code chunklimiter.yml} works on the survival server.
 */
public class ConfigFile {

    /** Where all bedwars configs live, next to the other {@code configs/} files of the network. */
    public static final String DIRECTORY = "./configs/bedwars";

    private final File file;
    private final String name;
    private YamlConfiguration config;

    /**
     * @param name the file name, e.g. {@code game.yml}
     */
    public ConfigFile(String name) {
        this(new File(DIRECTORY, name));
    }

    /**
     * @param file the file to back this config with
     */
    public ConfigFile(File file) {
        this.file = file;
        this.name = file.getName();
        reload();
    }

    /**
     * Reads the file from disk, creating it when it is not there yet.
     */
    public final void reload() {
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                warn("Could not create " + name + ": " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Looks a value up, writing the default into the file if it is not there yet.
     *
     * @param path     where the value lives
     * @param fallback the value to use and store when it is missing
     * @param comments what the value does, written above it
     * @return the configured value, or the fallback when the file holds something unusable
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String path, T fallback, String... comments) {
        if (!config.contains(path)) {
            config.set(path, fallback);
            if (comments.length > 0) config.setComments(path, List.of(comments));
            return fallback;
        }
        Object value = config.get(path);
        try {
            if (fallback instanceof Boolean) return (T) Boolean.valueOf(config.getBoolean(path));
            if (fallback instanceof Integer) return (T) Integer.valueOf(config.getInt(path));
            if (fallback instanceof Long) return (T) Long.valueOf(config.getLong(path));
            if (fallback instanceof Double) return (T) Double.valueOf(config.getDouble(path));
            if (fallback instanceof String) return (T) config.getString(path);
            return value == null ? fallback : (T) value;
        } catch (ClassCastException e) {
            warn("'" + path + "' in " + name + " is not usable, falling back to " + fallback + ".");
            return fallback;
        }
    }

    /**
     * Makes sure a section exists, so that values written underneath it end up in a documented block.
     *
     * @param path     the section
     * @param comments what the section is for
     */
    public void section(String path, String... comments) {
        if (config.contains(path)) return;
        config.createSection(path);
        if (comments.length > 0) config.setComments(path, List.of(comments));
    }

    /**
     * @param path the section to look at
     * @return the keys directly underneath it, empty when there is no such section
     */
    public Set<String> keys(String path) {
        ConfigurationSection section = config.getConfigurationSection(path);
        return section == null ? Set.of() : section.getKeys(false);
    }

    /**
     * @param path the list to read
     * @return the entries, empty when there is no such list
     */
    public List<Map<?, ?>> mapList(String path) {
        List<Map<?, ?>> entries = config.getMapList(path);
        return entries == null ? Collections.emptyList() : entries;
    }

    public boolean contains(String path) {
        return config.contains(path);
    }

    public void set(String path, Object value) {
        config.set(path, value);
    }

    public YamlConfiguration raw() {
        return config;
    }

    public String getName() {
        return name;
    }

    /**
     * Writes the file back, including every default that was filled in while reading.
     */
    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            warn("Could not save " + name + ": " + e.getMessage());
        }
    }

    /**
     * Logs to the console directly, because configs are read before the plugin logger is of any use.
     *
     * @param message what went wrong
     */
    private static void warn(String message) {
        Bukkit.getLogger().warning("[Bedwars] " + message);
    }
}

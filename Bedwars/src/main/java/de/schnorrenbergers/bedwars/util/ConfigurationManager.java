package de.schnorrenbergers.bedwars.util;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigurationManager {

    private YamlConfiguration  config;
    private File file;

    public ConfigurationManager() {
        file = new File("./configs/config.yml");
        if(!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public YamlConfiguration getConfig() {
        return config;
    }
    public void saveConfig() {
        try {
            config.save(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Object getOrDefault(String key, Object defaultValue) {
        if (config.contains(key)) {
            return config.get(key);
        }
        return defaultValue;
    }

    public int getOrDefault(String key, int defaultValue) {
        if (config.contains(key)) {
            return config.getInt(key);
        }
        return defaultValue;
    }
    public boolean getOrDefault(String key, boolean defaultValue) {
        if (config.contains(key)) {
            return config.getBoolean(key);
        }
        return defaultValue;
    }
    public String getOrDefault(String key, String defaultValue) {
        if (config.contains(key)) {
            return config.getString(key);
        }
        return defaultValue;
    }
    public Location getOrDefault(String key, Location defaultValue) {
        if (config.contains(key)) {
            return config.getLocation(key);
        }
        return defaultValue;
    }
    public double getOrDefault(String key, double defaultValue) {
        if (config.contains(key)) {
            return config.getDouble(key);
        }
        return defaultValue;
    }
}

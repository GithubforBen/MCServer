package de.hems.utils;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class Configuration {
    private File file;
    private YamlConfiguration config;
    public Configuration() {
        file = new File("./main-config.yml");
        System.out.println(file.getAbsolutePath());
        // strict on purpose: this file holds the proxy secret and the website account, and reading a broken
        // one as empty would quietly make new ones
        config = YamlFiles.load(file);
    }

    public void save() {
        try {
            YamlFiles.save(config, file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public YamlConfiguration getConfig() {
        return config;
    }
}

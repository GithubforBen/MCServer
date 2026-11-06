package com.benschnorr.survival.featrues.lab.brew;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * What the server decides about lab brewing, in {@code ./configs/lab.yml}. Missing values are written
 * back with their defaults so the file documents itself.
 */
public final class BrewSettings {

    private final int brewSeconds;

    public BrewSettings(File file, Logger logger) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.contains("brew-seconds")) {
            config.set("brew-seconds", 20);
            config.setComments("brew-seconds", List.of("How long one lab brew takes, in seconds."));
            try {
                if (file.getParentFile() != null) file.getParentFile().mkdirs();
                config.save(file);
            } catch (IOException e) {
                logger.warning("Could not write " + file.getPath() + ": " + e.getMessage());
            }
        }
        brewSeconds = Math.max(1, config.getInt("brew-seconds", 20));
    }

    /** One brew in ticks. */
    public int brewTicks() {
        return brewSeconds * 20;
    }
}

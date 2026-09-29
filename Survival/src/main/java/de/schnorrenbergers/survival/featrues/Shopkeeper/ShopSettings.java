package de.schnorrenbergers.survival.featrues.Shopkeeper;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * What the server decides about shops, in {@code ./configs/shop.yml} - next to {@code team.yml}, and apart
 * from {@code shop-config.yml}, which holds the shops themselves.
 */
public final class ShopSettings {

    private final int createCost;

    /**
     * Reads the file, writing back what was missing so it documents itself.
     *
     * @param file   where the settings are
     * @param logger where a file that cannot be written is reported
     */
    public ShopSettings(File file, Logger logger) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.contains("create-cost")) {
            config.set("create-cost", 2000);
            config.setComments("create-cost", List.of(
                    "What opening a shop costs, in bits. Paid by the player who opens it. 0 makes it free."));
            try {
                if (file.getParentFile() != null) file.getParentFile().mkdirs();
                config.save(file);
            } catch (IOException e) {
                logger.warning("Could not write " + file.getPath() + ": " + e.getMessage());
            }
        }
        createCost = Math.max(0, config.getInt("create-cost", 2000));
    }

    /**
     * @return what opening a shop costs
     */
    public int getCreateCost() {
        return createCost;
    }
}

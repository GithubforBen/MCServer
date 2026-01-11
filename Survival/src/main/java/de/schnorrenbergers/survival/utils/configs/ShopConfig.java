package de.schnorrenbergers.survival.utils.configs;

import de.schnorrenbergers.survival.Survival;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ShopConfig {
    private File file;
    private YamlConfiguration config;
    public ShopConfig() {
        file = new File("./configs/shop-config.yml");
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

    public void  save() {
        try {
            config.save(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Survival.getInstance().getLogger().info("ShopConfig saved!");
    }

    public YamlConfiguration getConfig() {
        return config;
    }
}

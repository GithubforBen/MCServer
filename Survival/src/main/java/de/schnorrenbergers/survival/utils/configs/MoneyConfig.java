package de.schnorrenbergers.survival.utils.configs;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class MoneyConfig {
    private File file;
    private CustomConfig config;
    public MoneyConfig() {
        file = new File("./configs/money-config.yml");
        if(!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        config = CustomConfig.loadConfiguration(file);
    }

    public void  save() {
        try {
            config.save(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public CustomConfig getConfig() {
        return config;
    }
}

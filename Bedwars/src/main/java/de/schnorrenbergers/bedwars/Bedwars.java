package de.schnorrenbergers.bedwars;

import de.schnorrenbergers.bedwars.commands.GameSettingsCommand;
import de.schnorrenbergers.bedwars.util.ConfigurationManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Bedwars extends JavaPlugin {
    private static Bedwars instance;
    private ConfigurationManager configurationManager;
    @Override
    public void onLoad() {
        instance = this;
        configurationManager = new ConfigurationManager();
    }

    @Override
    public void onEnable() {
        getCommand("gameSettings").setExecutor(new GameSettingsCommand());
        getCommand("gameSettings").setTabCompleter(new GameSettingsCommand());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static Bedwars getInstance() {
        return instance;
    }

    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }
}

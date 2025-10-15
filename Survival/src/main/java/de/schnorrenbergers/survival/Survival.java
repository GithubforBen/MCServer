package de.schnorrenbergers.survival;

import de.hems.ListenerAdapter;
import org.bukkit.entity.Golem;
import org.bukkit.plugin.java.JavaPlugin;

public final class Survival extends JavaPlugin {

    @Override
    public void onEnable() {
        try {
            new ListenerAdapter();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}

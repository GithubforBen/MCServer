package de.schnorrenbergers.lobbyPlugin;

import de.hems.communication.ListenerAdapter;
import de.hems.paper.customInventory.CustomInventoryListener;
import de.schnorrenbergers.lobbyPlugin.parkour.CheckpointListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class LobbyPlugin extends JavaPlugin {

    private static LobbyPlugin instance;

    @Override
    public void onLoad() {
        instance = this;
        try {
            new ListenerAdapter(ListenerAdapter.ServerName.LOBBY);
            new CustomInventoryListener(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }//TODO: parkour
        new CheckpointListener();
    }

    @Override
    public void onEnable() {
        // Plugin startup logic

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static LobbyPlugin getInstance() {
        return instance;
    }
}

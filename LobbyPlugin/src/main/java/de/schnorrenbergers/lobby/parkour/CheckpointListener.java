package de.schnorrenbergers.lobby.parkour;

import de.schnorrenbergers.lobby.LobbyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

public class CheckpointListener implements Listener {
    public CheckpointListener() {
        Bukkit.getPluginManager().registerEvents( this, LobbyPlugin.getInstance());
    }

}

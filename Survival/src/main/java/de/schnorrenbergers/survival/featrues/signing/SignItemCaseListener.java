package de.schnorrenbergers.survival.featrues.signing;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

/** Brigadier matches labels case-sensitively, so {@code /signItem} is rewritten to {@code /signitem}. */
public final class SignItemCaseListener implements Listener {

    private SignItemCaseListener() {
    }

    public static void init(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(new SignItemCaseListener(), plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String rewritten = SignItemLabel.rewrite(event.getMessage());
        if (rewritten != null) event.setMessage(rewritten);
    }
}

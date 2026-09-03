package de.hems.paper.cosmetic;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;

/**
 * Sets off the kill effects.
 * <p>
 * On the death rather than on the hit, and on the vanilla death event rather than on a game mode's own:
 * every mode on this network lets a real {@link PlayerDeathEvent} through, so one listener covers all of
 * them and no mode has to remember to call anything. What it does not cover is a death nobody caused - a
 * fall into the void has no killer, and an effect over an empty hole would be celebrating nothing.
 */
public class CosmeticKillListener implements Listener {

    private final Plugin plugin;

    public CosmeticKillListener(Plugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Monitor, because whether somebody died is decided by the game mode and not here: at this priority
     * the death is final and the killer is whoever the server says it is.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;
        KillEffects.playFor(plugin, killer, victim, victim.getLocation());
    }
}

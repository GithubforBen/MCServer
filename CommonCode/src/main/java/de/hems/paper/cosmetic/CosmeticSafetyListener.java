package de.hems.paper.cosmetic;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Keeps a celebration from hurting anybody.
 * <p>
 * A firework that goes off next to a player deals damage, which is fine for a firework somebody shot and
 * absurd for one that exists because they won. Every firework a cosmetic spawns is marked, and a marked
 * one never hurts anyone.
 */
public class CosmeticSafetyListener implements Listener {

    /** Marks a firework that belongs to a win effect. */
    public static final NamespacedKey COSMETIC_KEY = new NamespacedKey("hems", "cosmetic-firework");

    public CosmeticSafetyListener(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * @param firework a firework a cosmetic spawned
     */
    public static void mark(Firework firework) {
        firework.getPersistentDataContainer().set(COSMETIC_KEY, PersistentDataType.BYTE, (byte) 1);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Firework firework)) return;
        if (!firework.getPersistentDataContainer().has(COSMETIC_KEY, PersistentDataType.BYTE)) return;
        event.setCancelled(true);
    }
}

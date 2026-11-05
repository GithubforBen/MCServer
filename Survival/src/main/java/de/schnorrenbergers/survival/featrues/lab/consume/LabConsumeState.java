package de.schnorrenbergers.survival.featrues.lab.consume;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * What is pending on a player, kept in their persistent data so it survives a logout and a restart: the last
 * time they consumed, the crash that is due, and what Kokain gave to Speed and Haste (to tell it from a potion
 * or a beacon). Times are wall-clock milliseconds, so they keep running while the player is offline.
 */
final class LabConsumeState {

    private final NamespacedKey last;
    private final NamespacedKey crashDue;
    private final NamespacedKey crashPurity;
    private final NamespacedKey speedAmp;
    private final NamespacedKey speedEnd;
    private final NamespacedKey hasteAmp;
    private final NamespacedKey hasteEnd;

    LabConsumeState(Plugin plugin) {
        last = new NamespacedKey(plugin, "lab_last_consume");
        crashDue = new NamespacedKey(plugin, "lab_crash_due");
        crashPurity = new NamespacedKey(plugin, "lab_crash_purity");
        speedAmp = new NamespacedKey(plugin, "lab_speed_amp");
        speedEnd = new NamespacedKey(plugin, "lab_speed_end");
        hasteAmp = new NamespacedKey(plugin, "lab_haste_amp");
        hasteEnd = new NamespacedKey(plugin, "lab_haste_end");
    }

    /** The recorded level of one of our effects. */
    record Recorded(int amplifier, long endMillis) {
    }

    long lastConsume(Player player) {
        Long value = player.getPersistentDataContainer().get(last, PersistentDataType.LONG);
        return value == null ? LabConsumeRules.NEVER : value;
    }

    void setLastConsume(Player player, long now) {
        player.getPersistentDataContainer().set(last, PersistentDataType.LONG, now);
    }

    void setCrash(Player player, long dueMillis, int purity) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        data.set(crashDue, PersistentDataType.LONG, dueMillis);
        data.set(crashPurity, PersistentDataType.INTEGER, purity);
    }

    /** The pending crash as {due, purity}, or null. */
    long[] crash(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        Long due = data.get(crashDue, PersistentDataType.LONG);
        Integer purity = data.get(crashPurity, PersistentDataType.INTEGER);
        if (due == null || purity == null) return null;
        return new long[]{due, purity};
    }

    /** Forgets the crash and what Kokain gave. */
    void clearCrash(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        data.remove(crashDue);
        data.remove(crashPurity);
        data.remove(speedAmp);
        data.remove(speedEnd);
        data.remove(hasteAmp);
        data.remove(hasteEnd);
    }

    void setEffect(Player player, boolean speed, int amplifier, long endMillis) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        data.set(speed ? speedAmp : hasteAmp, PersistentDataType.INTEGER, amplifier);
        data.set(speed ? speedEnd : hasteEnd, PersistentDataType.LONG, endMillis);
    }

    void clearEffect(Player player, boolean speed) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        data.remove(speed ? speedAmp : hasteAmp);
        data.remove(speed ? speedEnd : hasteEnd);
    }

    Recorded effect(Player player, boolean speed) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        Integer amp = data.get(speed ? speedAmp : hasteAmp, PersistentDataType.INTEGER);
        Long end = data.get(speed ? speedEnd : hasteEnd, PersistentDataType.LONG);
        return amp == null || end == null ? null : new Recorded(amp, end);
    }
}

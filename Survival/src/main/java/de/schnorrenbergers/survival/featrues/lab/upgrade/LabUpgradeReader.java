package de.schnorrenbergers.survival.featrues.lab.upgrade;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Reads the upgrade blocks around a stand, and keeps what a running brew uses in the stand's persistent
 * data next to its progress, so unloading and restarts change nothing. Upgrades are read when a brew
 * starts and kept for that brew. see knowledge: /decisions/gadget-choices.md
 */
public final class LabUpgradeReader {

    /** How far from the stand upgrades reach, in every direction. */
    public static final int REACH = 2;

    private final NamespacedKey key;

    public LabUpgradeReader(JavaPlugin plugin) {
        this.key = new NamespacedKey(plugin, "lab_brew_upgrades");
    }

    /** The blocks within {@link #REACH} of the stand; unloaded chunks are not loaded for it and count as nothing. */
    public LabUpgrades scan(Block stand) {
        World world = stand.getWorld();
        boolean water = false;
        boolean amethyst = false;
        boolean rod = false;
        boolean smoker = false;
        for (int x = -REACH; x <= REACH; x++) {
            for (int y = -REACH; y <= REACH; y++) {
                for (int z = -REACH; z <= REACH; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    int bx = stand.getX() + x;
                    int bz = stand.getZ() + z;
                    int by = stand.getY() + y;
                    if (by < world.getMinHeight() || by >= world.getMaxHeight()) continue;
                    if (!world.isChunkLoaded(bx >> 4, bz >> 4)) continue;
                    Material type = world.getBlockAt(bx, by, bz).getType();
                    switch (type) {
                        case WATER_CAULDRON -> water = true; // an empty cauldron is a different material
                        case AMETHYST_BLOCK -> amethyst = true;
                        case LIGHTNING_ROD -> rod = true;
                        case SMOKER -> smoker = true;
                        default -> {
                        }
                    }
                }
            }
        }
        return new LabUpgrades(water, amethyst, rod, smoker);
    }

    /** What the running brew on this stand started with; none for a stand without a record. */
    public LabUpgrades stored(PersistentDataContainer data) {
        Integer bits = data.get(key, PersistentDataType.INTEGER);
        return bits == null ? LabUpgrades.NONE : LabUpgrades.fromBits(bits);
    }

    public void store(PersistentDataContainer data, LabUpgrades upgrades) {
        data.set(key, PersistentDataType.INTEGER, upgrades.bits());
    }

    public void clear(PersistentDataContainer data) {
        data.remove(key);
    }
}

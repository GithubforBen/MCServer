package com.benschnorr.survival.featrues.lab.item;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.Plugin;

/**
 * Keeps lab items from doing things they should not. Register once with {@link #init(Plugin)}.
 */
public final class LabItemListeners implements Listener {

    private LabItemListeners() {
    }

    public static void init(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(new LabItemListeners(), plugin);
    }

    /** Pflanzenabfall never becomes a block, whatever its base material would allow. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (LabItems.isKind(event.getItemInHand(), LabKind.PFLANZENABFALL)) event.setCancelled(true);
    }
}

package com.benschnorr.survival.featrues.signing.guard;

import com.benschnorr.survival.featrues.signing.SignItemCommand;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.block.Furnace;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Furnaces, blast furnaces and smokers do not finish a smelt of a signed item (the event is cancelled, so
 * the item stays in the input slot and a hopper never loses it). In the anvil a signed item on the right
 * gives no result; a signed item on the left keeps its signature on the result.
 * {@code FurnaceStartSmeltEvent} cannot be cancelled in the API, so a smelt is also blocked at its end as a safety net.
 */
public final class SignedSmeltAnvilGuard implements Listener {

    /** The lore line of a signature, without colors. */
    private static final String LORE_MARK = "Signiert von ";

    private SignedSmeltAnvilGuard() {
    }

    public static void init(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(new SignedSmeltAnvilGuard(), plugin);
    }

    private static boolean signed(ItemStack item) {
        return SignItemCommand.signerOf(item) != null;
    }

    /** No fuel is lit for a signed input; fuel that already burns just runs out. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBurn(FurnaceBurnEvent event) {
        if (event.getBlock().getState(false) instanceof Furnace furnace
                && signed(furnace.getInventory().getSmelting())) {
            event.setCancelled(true);
        }
    }

    /** BlockCookEvent covers FurnaceSmeltEvent for furnace, blast furnace and smoker, and the campfire. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSmelt(BlockCookEvent event) {
        if (signed(event.getSource())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAnvil(PrepareAnvilEvent event) {
        ItemStack left = event.getInventory().getFirstItem();
        ItemStack right = event.getInventory().getSecondItem();
        if (signed(right)) {
            event.setResult(null);
            return;
        }
        UUID signer = SignItemCommand.signerOf(left);
        ItemStack result = event.getResult();
        if (signer == null || result == null || result.getType().isAir()) return;
        if (signed(result)) return;
        event.setResult(restore(result, left, signer));
    }

    /** Puts the signature and its lore line from the left item back on a result that lost them. */
    static ItemStack restore(ItemStack result, ItemStack left, UUID signer) {
        ItemStack fixed = result.clone();
        ItemMeta meta = fixed.getItemMeta();
        if (meta == null) return fixed;
        meta.getPersistentDataContainer().set(SignItemCommand.SIGNED_BY, PersistentDataType.STRING, signer.toString());
        ItemMeta leftMeta = left.getItemMeta();
        List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        if (leftMeta != null && leftMeta.hasLore() && leftMeta.getLore() != null) {
            for (String line : leftMeta.getLore()) {
                if (line.contains(LORE_MARK) && !lore.contains(line)) lore.add(line);
            }
        }
        meta.setLore(lore);
        fixed.setItemMeta(meta);
        return fixed;
    }
}

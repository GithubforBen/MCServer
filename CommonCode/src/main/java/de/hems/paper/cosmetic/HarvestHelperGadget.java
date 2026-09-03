package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.Cosmetics;
import de.hems.types.cosmetic.GadgetSlot;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Harvests a ripe plant and puts a new one in its place, on one right click.
 * <p>
 * It saves the second click and nothing else. What the block would have dropped is what drops, minus the
 * one seed the new plant is grown from - exactly what a player pays who breaks it and plants again by
 * hand. Without that subtraction the gadget would hand out a free seed per harvest, which on a survival
 * server is a shop selling wheat.
 * <p>
 * The break is announced as a break. A gadget that takes a block away without anybody being able to say
 * no would harvest through every protected region on the server, so the answer of whatever guards the
 * world is asked first and taken as final.
 */
public class HarvestHelperGadget implements Gadget, Listener {

    /** What each crop is planted from. Anything not in here is not a crop as far as this is concerned. */
    private static Material seedOf(Material crop) {
        return switch (crop) {
            case WHEAT -> Material.WHEAT_SEEDS;
            case CARROTS -> Material.CARROT;
            case POTATOES -> Material.POTATO;
            case BEETROOTS -> Material.BEETROOT_SEEDS;
            case NETHER_WART -> Material.NETHER_WART;
            default -> null;
        };
    }

    @Override
    public String getId() {
        return Cosmetics.GADGET_HARVEST_HELPER;
    }

    @Override
    public Set<GadgetSlot> slots() {
        return Set.of(GadgetSlot.SURVIVAL);
    }

    @Override
    public @Nullable String hint() {
        return "Erntehelfer: Rechtsklick auf reife Pflanzen erntet und pflanzt neu.";
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        Material seed = seedOf(block.getType());
        if (seed == null || !(block.getBlockData() instanceof Ageable crop)) return;
        if (crop.getAge() < crop.getMaximumAge()) return;

        Player player = event.getPlayer();
        if (Gadgets.settingsFor(player, getId()) == null) return;
        // whatever they were holding stays out of it: a hoe would till and a seed would plant, and the
        // gadget is neither of those happening as well
        event.setCancelled(true);

        ItemStack tool = player.getInventory().getItemInMainHand();
        List<ItemStack> harvest = withoutOneSeed(block.getDrops(tool, player), seed);
        // no seed among the drops is a plant that cannot be put back, and half a harvest is worse than
        // none: it stays standing and the player breaks it themselves
        if (harvest == null) return;

        BlockBreakEvent breaking = new BlockBreakEvent(block, player);
        breaking.setDropItems(false);
        if (!breaking.callEvent()) return;

        BlockData fresh = crop.clone();
        ((Ageable) fresh).setAge(0);
        block.setBlockData(fresh);
        for (ItemStack drop : harvest) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5d, 0.2d, 0.5d), drop);
        }
        player.playSound(block.getLocation(), Sound.ITEM_CROP_PLANT, 0.7f, 1.2f);
    }

    /**
     * @param drops what the block would have dropped
     * @param seed  what the next plant is grown from
     * @return the drops with one seed taken out, or {@code null} when there was none to take
     */
    private @Nullable List<ItemStack> withoutOneSeed(Collection<ItemStack> drops, Material seed) {
        List<ItemStack> harvest = new ArrayList<>(drops.size());
        boolean paid = false;
        for (ItemStack drop : drops) {
            if (!paid && drop.getType() == seed) {
                paid = true;
                if (drop.getAmount() <= 1) continue;
                drop.setAmount(drop.getAmount() - 1);
            }
            harvest.add(drop);
        }
        return paid ? harvest : null;
    }
}

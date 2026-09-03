package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.Cosmetics;
import de.hems.types.cosmetic.GadgetSlot;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * A workbench that travels.
 * <p>
 * A workbench and nothing else. A furnace saves a trip home for the fuel, an anvil saves the trip for the
 * levels, and either of those is a change to how survival is played rather than a convenience - while
 * crafting a ladder halfway up a cliff costs nobody anything.
 * <p>
 * The item is a crafting table and never becomes one: placing it is refused, because a gadget somebody
 * can put down is a gadget they lose.
 */
public class WorkbenchGadget implements Gadget, Listener {

    @Override
    public String getId() {
        return Cosmetics.GADGET_WORKBENCH;
    }

    @Override
    public Set<GadgetSlot> slots() {
        return Set.of(GadgetSlot.SURVIVAL);
    }

    @Override
    public ItemStack item(CosmeticData cosmetic) {
        return GadgetItems.of(Material.CRAFTING_TABLE, getId(), "Werkbank", "Rechtsklick: aufklappen");
    }

    @Override
    public @Nullable String hint() {
        return "Werkbank: Rechtsklick, und du kannst überall craften.";
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!GadgetItems.is(event.getItem(), getId())) return;
        // cancelled before the check, so the block never goes down even for somebody whose gadget is off
        event.setCancelled(true);

        Player player = event.getPlayer();
        if (Gadgets.settingsFor(player, getId()) == null) return;
        player.openWorkbench(null, true);
        player.playSound(player, Sound.BLOCK_WOOD_PLACE, 0.6f, 1.3f);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (GadgetItems.is(event.getItemInHand(), getId())) event.setCancelled(true);
    }
}

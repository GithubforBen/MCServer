package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.Cosmetics;
import de.hems.types.cosmetic.GadgetSlot;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * A horse that comes when called and goes when told.
 * <p>
 * Tamed and saddled, so it can actually be ridden, and invulnerable and silent, so it is a way of getting
 * across the lobby rather than an animal. Its owner gets on straight away - a mount that has to be
 * mounted after being called is two clicks for one idea.
 * <p>
 * Lobby only. A horse that appears out of nothing is a transport in a world people walk through on
 * purpose, and survival has its own animals to ride.
 */
public class MountGadget implements TickingGadget, Listener {

    /** How fast it is, in hundredths of the vanilla speed of a very quick horse. */
    private static final String SETTING_SPEED = "speed";
    private static final int DEFAULT_SPEED = 25;

    private final GadgetEntities mounts = new GadgetEntities();

    @Override
    public String getId() {
        return Cosmetics.GADGET_MOUNT;
    }

    @Override
    public Set<GadgetSlot> slots() {
        return Set.of(GadgetSlot.LOBBY);
    }

    @Override
    public ItemStack item(CosmeticData cosmetic) {
        return GadgetItems.of(Material.SADDLE, getId(), "Reittier", "Rechtsklick: rufen und wegschicken");
    }

    @Override
    public @Nullable String hint() {
        return "Reittier: Rechtsklick mit dem Sattel ruft es - nochmal, und es ist wieder weg.";
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!GadgetItems.is(event.getItem(), getId())) return;
        event.setCancelled(true);

        Player player = event.getPlayer();
        CosmeticData gadget = Gadgets.settingsFor(player, getId());
        if (gadget == null) return;

        if (mounts.of(player) != null) {
            mounts.remove(player);
            player.playSound(player, Sound.ENTITY_HORSE_BREATHE, 0.6f, 1.2f);
            return;
        }
        call(player, gadget);
    }

    /**
     * Keeps the horse honest: one that its rider left behind, or that stayed in a world its owner walked
     * out of, is a horse standing around for nobody.
     */
    @Override
    public void tick(Player player, CosmeticData cosmetic) {
        Entity mount = mounts.of(player);
        if (mount == null) return;
        if (mount.getWorld() != player.getWorld() || !player.equals(mount.getPassengers().stream()
                .findFirst().orElse(null))) {
            mounts.remove(player);
        }
    }

    @Override
    public void cleanUp(Player player) {
        mounts.remove(player);
    }

    private void call(Player player, CosmeticData gadget) {
        Entity spawned = player.getWorld().spawnEntity(player.getLocation(), EntityType.HORSE);
        if (!(spawned instanceof Horse horse)) {
            spawned.remove();
            return;
        }
        // tamed and saddled is what makes it steerable; without both it is an animal that walks where it
        // wants with somebody sitting on it
        horse.setTamed(true);
        horse.setOwner(player);
        horse.setAdult();
        horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
        horse.setJumpStrength(0.8d);
        mounts.keep(player, horse);
        // the AI has to stay on: a horse without one cannot be ridden anywhere
        horse.setAI(true);
        speed(horse, gadget);
        horse.addPassenger(player);
        player.playSound(player, Sound.ENTITY_HORSE_AMBIENT, 0.7f, 1.3f);
    }

    private void speed(Horse horse, CosmeticData gadget) {
        int hundredths = Math.max(1, gadget.getNumber(SETTING_SPEED, DEFAULT_SPEED));
        org.bukkit.attribute.AttributeInstance speed =
                horse.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(hundredths / 100.0d);
    }
}

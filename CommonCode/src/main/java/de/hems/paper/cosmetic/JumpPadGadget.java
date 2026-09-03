package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.Cosmetics;
import de.hems.types.cosmetic.GadgetSlot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A pad that throws whoever steps on it into the air, and is gone a few seconds later.
 * <p>
 * The pad is not a block. It is a block change sent to the people nearby and a location this gadget
 * remembers, which is what makes it safe to hand out in a lobby: there is nothing to break, nothing to
 * stack into a staircase, and nothing left over when its owner logs off mid jump.
 * <p>
 * Anybody who walks onto one is thrown, not only its owner. That is the point of putting one down.
 */
public class JumpPadGadget implements Gadget, Listener {

    /** How hard it throws when nobody set anything, in tenths of a block per tick. */
    private static final int DEFAULT_POWER = 11;
    /** How long a pad stays, in ticks. */
    private static final int DEFAULT_DURATION_TICKS = 120;
    /** How long before the next pad can be put down, in ticks. */
    private static final int DEFAULT_COOLDOWN_TICKS = 60;
    /** How far away a pad is still drawn, in blocks. */
    private static final double RANGE = 32.0d;

    private final Plugin plugin;
    /** Where the pads are, and who put them there. */
    private final Map<Location, UUID> pads = new ConcurrentHashMap<>();

    public JumpPadGadget(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return Cosmetics.GADGET_JUMP_PAD;
    }

    @Override
    public Set<GadgetSlot> slots() {
        return Set.of(GadgetSlot.LOBBY);
    }

    @Override
    public ItemStack item(CosmeticData cosmetic) {
        return GadgetItems.of(Material.SLIME_BALL, getId(), "Sprungpad",
                "Rechtsklick auf den Boden");
    }

    @Override
    public @Nullable String hint() {
        return "Sprungpad: leg eins hin - wer drauftritt, fliegt.";
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        if (!GadgetItems.is(event.getItem(), getId())) return;
        event.setCancelled(true);

        Player player = event.getPlayer();
        CosmeticData gadget = Gadgets.settingsFor(player, getId());
        if (gadget == null) return;
        if (player.hasCooldown(Material.SLIME_BALL)) return;

        Block on = event.getClickedBlock().getRelative(org.bukkit.block.BlockFace.UP);
        if (!on.getType().isAir()) return;
        Location at = block(on.getLocation());
        pads.put(at, player.getUniqueId());
        draw(at, Material.SLIME_BLOCK.createBlockData());

        player.setCooldown(Material.SLIME_BALL, Math.max(1,
                gadget.getNumber(Cosmetics.SETTING_COOLDOWN_TICKS, DEFAULT_COOLDOWN_TICKS)));
        player.playSound(player, Sound.BLOCK_SLIME_BLOCK_PLACE, 0.8f, 1.2f);
        int life = Math.max(20, gadget.getNumber(Cosmetics.SETTING_DURATION_TICKS,
                DEFAULT_DURATION_TICKS));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> remove(at), life);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        // one map lookup per block somebody walks onto, and the map is empty almost always
        if (!event.hasChangedBlock() || pads.isEmpty()) return;
        // the pad is the air its owner's feet are in, so the block somebody walked into is the key
        if (!pads.containsKey(block(event.getTo()))) return;

        Player player = event.getPlayer();
        double power = DEFAULT_POWER / 10.0d;
        CosmeticData gadget = CosmeticService.get(getId());
        if (gadget != null) {
            power = Math.max(0.1d, gadget.getNumber(Cosmetics.SETTING_POWER, DEFAULT_POWER) / 10.0d);
        }
        player.setVelocity(player.getVelocity().setY(power));
        // the landing, not the flight: without it the pad is a way of dying in a lobby with a high roof
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0, true, false));
        player.playSound(player, Sound.BLOCK_SLIME_BLOCK_HIT, 0.9f, 1.4f);
    }

    @Override
    public void cleanUp(Player player) {
        for (Map.Entry<Location, UUID> pad : pads.entrySet()) {
            if (pad.getValue().equals(player.getUniqueId())) remove(pad.getKey());
        }
    }

    /**
     * Takes one pad away again and gives the air underneath back to everybody who was shown a block.
     */
    private void remove(Location at) {
        if (pads.remove(at) == null) return;
        draw(at, at.getBlock().getBlockData());
    }

    private void draw(Location at, org.bukkit.block.data.BlockData data) {
        if (at.getWorld() == null) return;
        for (Player viewer : at.getWorld().getPlayers()) {
            if (viewer.getLocation().distanceSquared(at) > RANGE * RANGE) continue;
            viewer.sendBlockChange(at, data);
        }
    }

    /**
     * @param at somewhere
     * @return the same place, rounded to the block it is in - the key the pads are kept under
     */
    private static Location block(Location at) {
        return new Location(at.getWorld(), at.getBlockX(), at.getBlockY(), at.getBlockZ());
    }
}

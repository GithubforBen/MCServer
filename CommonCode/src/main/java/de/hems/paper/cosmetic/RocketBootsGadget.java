package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.Cosmetics;
import de.hems.types.cosmetic.GadgetSlot;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * A rocket that throws its owner into the air.
 * <p>
 * The rocket is never actually used up - it is a cosmetic, not a stack of fireworks - so the click is
 * cancelled and the push is done by hand. The firework that goes off is only there to be seen; it is
 * marked as a cosmetic one so it cannot hurt anybody standing next to the launch.
 * <p>
 * Lobby only, and that is a decision about the map rather than about the gadget: this throws somebody
 * high enough to land on things a lobby is built to keep them off, and a hub has walls for that while a
 * survival world does not.
 */
public class RocketBootsGadget implements Gadget, Listener {

    /** How hard it throws when nobody set anything, in tenths of a block per tick. */
    private static final int DEFAULT_POWER = 14;
    /** How long before it can be used again, in ticks. */
    private static final int DEFAULT_COOLDOWN_TICKS = 80;
    /** How long the soft landing lasts, in ticks. */
    private static final int DEFAULT_DURATION_TICKS = 140;

    @Override
    public String getId() {
        return Cosmetics.GADGET_ROCKET_BOOTS;
    }

    @Override
    public Set<GadgetSlot> slots() {
        return Set.of(GadgetSlot.LOBBY);
    }

    @Override
    public ItemStack item(CosmeticData cosmetic) {
        return GadgetItems.of(Material.FIREWORK_ROCKET, getId(), "Raketenstiefel",
                "Rechtsklick: ab nach oben");
    }

    @Override
    public @Nullable String hint() {
        return "Raketenstiefel: Rechtsklick wirft dich hoch, runter kommst du sanft.";
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!GadgetItems.is(event.getItem(), getId())) return;
        Player player = event.getPlayer();
        // cancelled either way: a firework rocket in hand is a firework rocket, and one that goes off in
        // somebody's hotbar because the gadget was not theirs to use is a rocket they paid nothing for
        event.setCancelled(true);

        CosmeticData gadget = Gadgets.settingsFor(player, getId());
        if (gadget == null) return;
        if (player.hasCooldown(Material.FIREWORK_ROCKET)) return;

        double power = Math.max(0.1d, gadget.getNumber(Cosmetics.SETTING_POWER, DEFAULT_POWER) / 10.0d);
        player.setVelocity(player.getVelocity().setY(power));
        int duration = Math.max(1, gadget.getNumber(Cosmetics.SETTING_DURATION_TICKS,
                DEFAULT_DURATION_TICKS));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0, true, false));
        player.setCooldown(Material.FIREWORK_ROCKET, Math.max(1,
                gadget.getNumber(Cosmetics.SETTING_COOLDOWN_TICKS, DEFAULT_COOLDOWN_TICKS)));
        player.playSound(player, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1.2f);
        show(player);
    }

    /**
     * The puff of colour under the launch. Detonated at once, so it is a bang and not a rocket somebody
     * has to wait for.
     */
    private void show(Player player) {
        Firework firework = player.getWorld().spawn(player.getLocation(), Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BURST)
                .withColor(Color.FUCHSIA, Color.AQUA).withTrail().build());
        firework.setFireworkMeta(meta);
        CosmeticSafetyListener.mark(firework);
        firework.detonate();
    }
}

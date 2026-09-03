package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.Cosmetics;
import de.hems.types.cosmetic.GadgetSlot;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A bang of colour, and nothing else.
 * <p>
 * The one gadget in the list that changes nothing at all: no push, no reach, no advantage. That is why it
 * is allowed on survival as well - a cosmetic that is only a picture cannot be sold as a shortcut.
 */
public class ConfettiCannonGadget implements Gadget, Listener {

    /** How long before it can be fired again, in ticks. */
    private static final int DEFAULT_COOLDOWN_TICKS = 40;
    /** How many specks of colour one shot is. */
    private static final int SPECKS = 60;
    /** How big the cloud is, in blocks. */
    private static final double SPREAD = 1.1d;

    @Override
    public String getId() {
        return Cosmetics.GADGET_CONFETTI;
    }

    @Override
    public Set<GadgetSlot> slots() {
        return Set.of(GadgetSlot.LOBBY, GadgetSlot.SURVIVAL);
    }

    @Override
    public ItemStack item(CosmeticData cosmetic) {
        return GadgetItems.of(Material.FIREWORK_STAR, getId(), "Konfetti-Kanone", "Rechtsklick: Konfetti");
    }

    @Override
    public @Nullable String hint() {
        return "Konfetti-Kanone: Rechtsklick, und es regnet Farbe.";
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
        if (player.hasCooldown(Material.FIREWORK_STAR)) return;
        player.setCooldown(Material.FIREWORK_STAR, Math.max(1,
                gadget.getNumber(Cosmetics.SETTING_COOLDOWN_TICKS, DEFAULT_COOLDOWN_TICKS)));

        Location at = player.getLocation().add(0.0d, 1.2d, 0.0d);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < SPECKS; i++) {
            Color colour = Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256));
            // one speck per call, because a single call with a count draws them all in one colour
            player.getWorld().spawnParticle(Particle.DUST, at, 1, SPREAD, 0.5d, SPREAD, 0.0d,
                    new Particle.DustOptions(colour, 1.1f));
        }
        player.getWorld().playSound(at, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.6f, 1.6f);
    }
}

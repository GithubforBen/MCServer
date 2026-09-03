package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.Cosmetics;
import de.hems.types.cosmetic.GadgetSlot;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * A second jump in mid air.
 * <p>
 * There is no jump event in the game, so the double jump is the flight toggle: whoever wears it is
 * allowed to fly, presses space a second time, and the moment the server asks whether they may start
 * flying is the moment they get thrown forwards instead. Which is also why this one is the lobby's alone
 * - survival hands out flight of its own, and two plugins turning the same switch on and off would leave
 * somebody standing in a world they cannot fly in with the ability to double jump out of it.
 * <p>
 * The landing is soft on purpose. A jump that has to be aimed to avoid dying is one nobody uses twice.
 */
public class DoubleJumpGadget implements TickingGadget, Listener {

    /** How hard it throws when nobody set anything, in tenths of a block per tick. */
    private static final int DEFAULT_POWER = 8;
    /** How long the soft landing lasts, in ticks. */
    private static final int DEFAULT_DURATION_TICKS = 60;
    /** The upward part of the jump, on top of whatever direction its owner is looking. */
    private static final double LIFT = 0.55d;

    @Override
    public String getId() {
        return Cosmetics.GADGET_DOUBLE_JUMP;
    }

    @Override
    public Set<GadgetSlot> slots() {
        return Set.of(GadgetSlot.LOBBY);
    }

    @Override
    public @Nullable String hint() {
        return "Doppelsprung: in der Luft nochmal springen.";
    }

    /**
     * Hands the second jump back, once its owner is standing on something again.
     */
    @Override
    public void tick(Player player, CosmeticData cosmetic) {
        if (!manages(player)) return;
        if (!player.isOnGround() || player.getAllowFlight()) return;
        player.setAllowFlight(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        if (!event.isFlying()) return;
        Player player = event.getPlayer();
        if (!manages(player)) return;
        CosmeticData gadget = Gadgets.settingsFor(player, getId());
        if (gadget == null) return;

        event.setCancelled(true);
        player.setAllowFlight(false);
        player.setFlying(false);

        double power = Math.max(0.1d, gadget.getNumber(Cosmetics.SETTING_POWER, DEFAULT_POWER) / 10.0d);
        player.setVelocity(player.getLocation().getDirection().multiply(power).setY(LIFT));
        int duration = Math.max(1, gadget.getNumber(Cosmetics.SETTING_DURATION_TICKS,
                DEFAULT_DURATION_TICKS));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0, true, false));
        player.playSound(player, Sound.ENTITY_BAT_TAKEOFF, 0.6f, 1.4f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 12, 0.3d, 0.1d, 0.3d, 0.02d);
    }

    @Override
    public void cleanUp(Player player) {
        if (!manages(player)) return;
        player.setFlying(false);
        player.setAllowFlight(false);
    }

    /**
     * @param player somebody
     * @return whether their flight is this gadget's to turn on and off - a creative builder's is not
     */
    private boolean manages(Player player) {
        GameMode mode = player.getGameMode();
        return mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE;
    }
}

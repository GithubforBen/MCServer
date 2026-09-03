package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.Cosmetics;
import de.hems.types.cosmetic.GadgetSlot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Set;

/**
 * A balloon on a string over its owner's head.
 * <p>
 * The balloon is a stand nobody can see wearing a coloured block, and the string is drawn rather than
 * tied: a real lead needs an animal on the other end, and an animal is a thing that can be pushed, hit
 * and stood on. Drawn, the string cannot be any of those.
 * <p>
 * It bobs, because a balloon that hangs perfectly still over somebody's head reads as a bug.
 */
public class BalloonGadget implements TickingGadget {

    /** Which block the balloon is made of. */
    private static final String SETTING_COLOUR = "colour";
    private static final Material DEFAULT_COLOUR = Material.RED_CONCRETE;
    /** How high above its owner it floats, in blocks. */
    private static final double HEIGHT = 2.6d;
    /** How far it bobs up and down, in blocks. */
    private static final double BOB = 0.12d;
    /** How many puffs of smoke the string is drawn out of. */
    private static final int STRING_STEPS = 6;

    private final GadgetEntities balloons = new GadgetEntities();

    @Override
    public String getId() {
        return Cosmetics.GADGET_BALLOON;
    }

    @Override
    public Set<GadgetSlot> slots() {
        return Set.of(GadgetSlot.LOBBY, GadgetSlot.SURVIVAL);
    }

    @Override
    public @Nullable String hint() {
        return "Ballon: schwebt an einer Schnur über dir.";
    }

    @Override
    public void tick(Player player, CosmeticData cosmetic) {
        Entity balloon = balloons.of(player);
        if (balloon == null || balloon.getWorld() != player.getWorld()) {
            balloons.remove(player);
            balloon = spawn(player, cosmetic);
            if (balloon == null) return;
        }
        // the bob comes from the world's clock rather than a counter of our own, so two balloons next to
        // each other move together instead of drifting apart
        double bob = Math.sin(player.getWorld().getFullTime() / 8.0d) * BOB;
        Location goal = player.getLocation().add(0.0d, HEIGHT + bob, 0.0d);
        goal.setYaw(player.getLocation().getYaw());
        balloon.teleport(goal);
        drawString(player, goal);
    }

    @Override
    public void cleanUp(Player player) {
        balloons.remove(player);
    }

    private @Nullable Entity spawn(Player player, CosmeticData cosmetic) {
        Entity spawned = player.getWorld().spawnEntity(
                player.getLocation().add(0.0d, HEIGHT, 0.0d), EntityType.ARMOR_STAND);
        if (!(spawned instanceof ArmorStand stand)) {
            spawned.remove();
            return null;
        }
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setSmall(true);
        // a marker has no hitbox at all, which is what keeps the balloon from being something people can
        // shoot at, stand on or push around
        stand.setMarker(true);
        stand.getEquipment().setHelmet(new ItemStack(colour(cosmetic)));
        balloons.keep(player, stand);
        return stand;
    }

    /**
     * The string: a few particles on the line between its owner's shoulder and the balloon.
     */
    private void drawString(Player player, Location balloon) {
        Location shoulder = player.getLocation().add(0.0d, 1.2d, 0.0d);
        Vector step = balloon.toVector().subtract(shoulder.toVector()).multiply(1.0d / STRING_STEPS);
        for (int i = 1; i < STRING_STEPS; i++) {
            Location at = shoulder.clone().add(step.clone().multiply(i));
            player.getWorld().spawnParticle(Particle.WHITE_ASH, at, 1, 0.0d, 0.0d, 0.0d, 0.0d);
        }
    }

    private Material colour(CosmeticData cosmetic) {
        String named = cosmetic == null ? null : cosmetic.getSettings().get(SETTING_COLOUR);
        Material material = named == null ? null : Material.matchMaterial(named.trim().toUpperCase(Locale.ROOT));
        return material != null && material.isBlock() ? material : DEFAULT_COLOUR;
    }
}

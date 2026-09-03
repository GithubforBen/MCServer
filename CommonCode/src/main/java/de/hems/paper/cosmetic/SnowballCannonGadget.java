package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.Cosmetics;
import de.hems.types.cosmetic.GadgetSlot;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Snowballs that shove people about.
 * <p>
 * A vanilla snowball already pushes a little and hurts nobody. This one pushes properly, and that is the
 * whole gadget - which is also why it is the lobby's alone: being shoved is funny where there is nothing
 * to fall off and a grief where there is.
 * <p>
 * Every snowball it throws is marked. Without that the push would be on every snowball on the server,
 * including the ones somebody brought from somewhere else.
 */
public class SnowballCannonGadget implements Gadget, Listener {

    /** Marks a snowball this gadget threw. */
    private static final NamespacedKey THROWN = new NamespacedKey("hems", "gadget-snowball");
    /** How hard it shoves when nobody set anything, in tenths of a block per tick. */
    private static final int DEFAULT_POWER = 8;
    /** How long until the next snowball arrives, in ticks. */
    private static final int DEFAULT_COOLDOWN_TICKS = 20;
    /** The upward part of every shove, so people are pushed off their feet rather than into the floor. */
    private static final double LIFT = 0.34d;

    private final Plugin plugin;

    public SnowballCannonGadget(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return Cosmetics.GADGET_SNOWBALL_CANNON;
    }

    @Override
    public Set<GadgetSlot> slots() {
        return Set.of(GadgetSlot.LOBBY);
    }

    @Override
    public ItemStack item(CosmeticData cosmetic) {
        return GadgetItems.of(Material.SNOWBALL, getId(), "Schneeball-Kanone",
                "Wirf sie - sie kommt zurück");
    }

    @Override
    public @Nullable String hint() {
        return "Schneeball-Kanone: schubst andere weg und tut niemandem weh.";
    }

    @EventHandler
    public void onLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) return;
        if (!(snowball.getShooter() instanceof Player player)) return;
        CosmeticData gadget = Gadgets.settingsFor(player, getId());
        if (gadget == null) return;

        snowball.getPersistentDataContainer().set(THROWN, PersistentDataType.BYTE, (byte) 1);
        int cooldown = Math.max(1,
                gadget.getNumber(Cosmetics.SETTING_COOLDOWN_TICKS, DEFAULT_COOLDOWN_TICKS));
        Gadgets.later(plugin, player, getId(), cooldown, (thrower, worn) -> {
            if (Gadgets.give(thrower, item(worn))) {
                thrower.playSound(thrower, Sound.BLOCK_SNOW_BREAK, 0.4f, 1.6f);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onHit(ProjectileHitEvent event) {
        if (!marked(event.getEntity())) return;
        if (!(event.getHitEntity() instanceof Player hit)) return;

        Vector push = event.getEntity().getVelocity();
        if (push.lengthSquared() < 0.0001d) return;
        push.normalize();
        double power = DEFAULT_POWER / 10.0d;
        if (event.getEntity().getShooter() instanceof Player shooter) {
            CosmeticData gadget = Gadgets.settingsFor(shooter, getId());
            if (gadget != null) {
                power = Math.max(0.1d, gadget.getNumber(Cosmetics.SETTING_POWER, DEFAULT_POWER) / 10.0d);
            }
        }
        push.multiply(power).setY(LIFT);
        hit.setVelocity(hit.getVelocity().add(push));
        hit.playSound(hit, Sound.BLOCK_SNOW_BREAK, 0.7f, 1.2f);
    }

    /**
     * Keeps the shove from ever becoming a hit. A snowball does no damage to a player in the first place,
     * but it does to a few mobs, and a cosmetic that kills the lobby's chickens is not a cosmetic.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (marked(event.getDamager())) event.setCancelled(true);
    }

    private static boolean marked(org.bukkit.entity.Entity entity) {
        return entity instanceof Projectile
                && entity.getPersistentDataContainer().has(THROWN, PersistentDataType.BYTE);
    }
}

package de.schnorrenbergers.bedwars.listener;

import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.config.Feature;
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The rest of 1.8 combat: everything an attack speed and a cancelled sweep leave behind.
 * <p>
 * Taking the cooldown out makes a fight look like 1.8 and still play like today, because the numbers
 * underneath are today's. An axe hits for nine and no longer has to recharge, armour gives way to a hard
 * hit, a player in the air is not lifted by the next hit, a full food bar heals two hearts a second, a
 * sprinting player cannot crit, and there is a second hand. Each of those is put back to what it was:
 * <ul>
 *     <li>weapons deal what they dealt in 1.8, and sharpness adds 1.25 a level</li>
 *     <li>a jump hit is a critical hit whether or not the attacker sprints</li>
 *     <li>armour takes four percent a point off every hit, however hard it is</li>
 *     <li>a hit lifts its victim in the air as well, and a sprint hit pushes on top of it</li>
 *     <li>a full food bar heals half a heart every four seconds</li>
 *     <li>the off hand stays empty</li>
 * </ul>
 * All of it hangs off {@link Feature#OLD_PVP}, so switching that off gives the modern game back whole.
 */
public class OldCombatListener implements Listener {

    /** What a critical hit multiplies the weapon's damage by, then as now. */
    private static final double CRIT_FACTOR = 1.5d;
    /** How much of a hit one point of armour takes off. */
    private static final double ARMOR_PER_POINT = 0.04d;
    /** More armour than this never counted. */
    private static final double MAX_ARMOR = 20.0d;
    /** The knockback every hit deals, which is how the hit itself is told from the sprint on top of it. */
    private static final double HIT_KNOCKBACK = 0.4d;
    /** How far up a hit throws at most, and how far up a sprint hit adds. */
    private static final double KNOCKBACK_UP_LIMIT = 0.4d;
    private static final double SPRINT_KNOCKBACK_UP = 0.1d;
    /** How long a full food bar takes for half a heart, in ticks, and what that costs. */
    private static final int REGEN_INTERVAL_TICKS = 80;
    private static final float REGEN_EXHAUSTION = 3.0f;
    /** Where the off hand sits in a player's own inventory. */
    private static final int OFF_HAND_SLOT = 40;

    private final Plugin plugin;
    /** When somebody last healed from a full food bar, in server ticks. */
    private final Map<UUID, Integer> lastRegen = new HashMap<>();

    public OldCombatListener(Plugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ------------------------------------------------------------------- damage

    /**
     * Puts the damage of a hit and what armour takes off it back to the 1.8 numbers.
     * <p>
     * After the team check and before the hit is looked at for being the last one, so a team mate's hit
     * is never worked on and a death is decided on the damage that is really dealt.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!on()) return;
        if (event instanceof EntityDamageByEntityEvent hit) weaponDamage(hit);
        if (event.getEntity() instanceof Player victim) armor(event, victim);
    }

    private void weaponDamage(EntityDamageByEntityEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        int sharpness = weapon.getEnchantmentLevel(Enchantment.SHARPNESS);
        boolean critNow = event.isCritical();
        boolean crit = critNow || jumpHit(attacker);

        // today's hit is weapon times crit plus sharpness, so that is how it is taken apart again
        double weaponNow = (event.getDamage() - sharpnessNow(sharpness)) / (critNow ? CRIT_FACTOR : 1.0d);
        double damage = damage(weaponNow, weapon.getType(), sharpness, crit);
        if (Math.abs(damage - event.getDamage()) > 1.0e-6d) event.setDamage(damage);

        // a crit the server did not see is one it did not show either
        if (crit && !critNow) {
            victim.getWorld().spawnParticle(Particle.CRIT,
                    victim.getLocation().add(0.0d, victim.getHeight() / 2.0d, 0.0d),
                    12, 0.3d, 0.4d, 0.3d, 0.4d);
        }
    }

    /**
     * @param weaponNow what the weapon deals today, without sharpness and without a crit
     * @param weapon    what is being hit with
     * @param sharpness the level of sharpness on it
     * @param crit      whether it is a critical hit
     * @return what that hit dealt in 1.8, before armour
     */
    static double damage(double weaponNow, Material weapon, int sharpness, boolean crit) {
        double base = Math.max(0.0d, weaponNow + weaponShift(weapon));
        return base * (crit ? CRIT_FACTOR : 1.0d) + 1.25d * sharpness;
    }

    /**
     * @param weapon what is being hit with
     * @return how much more it dealt in 1.8 than it does today
     */
    static double weaponShift(Material weapon) {
        String name = weapon.name();
        // 1.9 took a point off every sword and pickaxe, gave the shovel half a point and made the axe
        // the hardest hit in the game - which without a cooldown is the only weapon worth buying
        if (name.endsWith("_SWORD") || name.endsWith("_PICKAXE")) return 1.0d;
        if (name.endsWith("_SHOVEL")) return -0.5d;
        return switch (name) {
            case "WOODEN_AXE", "GOLDEN_AXE", "IRON_AXE" -> -3.0d;
            case "STONE_AXE", "COPPER_AXE" -> -4.0d;
            case "DIAMOND_AXE", "NETHERITE_AXE" -> -2.0d;
            default -> 0.0d;
        };
    }

    private static double sharpnessNow(int level) {
        return level <= 0 ? 0.0d : 0.5d * level + 0.5d;
    }

    /**
     * @return whether a hit of theirs is a critical one by the 1.8 rule, which never asked about sprinting
     */
    private static boolean jumpHit(Player attacker) {
        return attacker.getFallDistance() > 0.0f
                && !attacker.isOnGround()
                && !attacker.isClimbing()
                && !attacker.isInWater()
                && !attacker.hasPotionEffect(PotionEffectType.BLINDNESS)
                && !attacker.isInsideVehicle();
    }

    /**
     * Works out again what armour takes off a hit, and everything that is taken off after it.
     * <p>
     * Today armour is worth less the harder the hit is: ten points stop a quarter of a diamond sword
     * rather than four tenths. The parts behind armour are a share of what armour lets through, so they
     * keep their share and are worked out on the new number.
     */
    @SuppressWarnings("deprecation") // the parts of a hit have no other api, and they are what this is about
    private static void armor(EntityDamageEvent event, Player victim) {
        if (!event.isApplicable(DamageModifier.ARMOR)) return;
        double armorNow = event.getDamage(DamageModifier.ARMOR);
        // nothing taken off means this is a hit armour does not count for - the void, a fall, a potion
        if (armorNow >= 0.0d) return;
        AttributeInstance attribute = victim.getAttribute(Attribute.ARMOR);
        if (attribute == null) return;

        double incoming = 0.0d;
        for (DamageModifier part : new DamageModifier[]{DamageModifier.BASE,
                DamageModifier.INVULNERABILITY_REDUCTION, DamageModifier.FREEZING,
                DamageModifier.HARD_HAT, DamageModifier.BLOCKING}) {
            if (event.isApplicable(part)) incoming += event.getDamage(part);
        }
        if (incoming <= 0.0d) return;

        double armor = -armorOff(incoming, attribute.getValue());
        event.setDamage(DamageModifier.ARMOR, armor);
        double before = incoming + armorNow;
        double left = incoming + armor;
        for (DamageModifier part : new DamageModifier[]{DamageModifier.RESISTANCE, DamageModifier.MAGIC}) {
            if (!event.isApplicable(part)) continue;
            double now = event.getDamage(part);
            double then = before <= 0.0d ? 0.0d : now / before * left;
            event.setDamage(part, then);
            before += now;
            left += then;
        }
        if (event.isApplicable(DamageModifier.ABSORPTION)) {
            event.setDamage(DamageModifier.ABSORPTION,
                    -Math.max(0.0d, Math.min(left, victim.getAbsorptionAmount())));
        }
    }

    /**
     * @param damage what arrives at the armour
     * @param points how many points of armour are worn
     * @return how much of it the armour takes off
     */
    static double armorOff(double damage, double points) {
        return damage * Math.max(0.0d, Math.min(MAX_ARMOR, points)) * ARMOR_PER_POINT;
    }

    // ---------------------------------------------------------------- knockback

    /**
     * Throws a player who is hit the way 1.8 did.
     * <p>
     * Two things changed in 1.9 and both of them are why a fight feels different. A hit no longer lifts
     * somebody who is already in the air, so the second hit of a combo drops them instead of carrying
     * them; and the push of a sprint hit halves the hit before it instead of adding to it, which makes a
     * sprint hit throw seven tenths of a block a tick where it used to throw nine.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onKnockback(EntityKnockbackEvent event) {
        if (!on()) return;
        if (event.getCause() != EntityKnockbackEvent.Cause.ENTITY_ATTACK) return;
        if (!(event instanceof EntityKnockbackByEntityEvent hit)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        double strength = hit.getKnockbackStrength();
        if (Math.abs(strength - HIT_KNOCKBACK) < 1.0e-3d) {
            // the hit itself: sideways it is right already, upwards it is only right on the ground
            double moving = victim.getVelocity().getY();
            Vector push = event.getKnockback();
            event.setKnockback(new Vector(push.getX(), hitLift(moving) - moving, push.getZ()));
            return;
        }
        // what comes on top for sprinting or an enchantment, in the direction the attacker looks
        if (!(hit.getHitBy() instanceof Player attacker)) return;
        double yaw = Math.toRadians(attacker.getYaw());
        event.setKnockback(new Vector(-Math.sin(yaw) * strength, SPRINT_KNOCKBACK_UP, Math.cos(yaw) * strength));
    }

    /**
     * @param moving how fast somebody is moving upwards when they are hit
     * @return how fast they move upwards after it
     */
    static double hitLift(double moving) {
        return Math.min(KNOCKBACK_UP_LIMIT, moving / 2.0d + HIT_KNOCKBACK);
    }

    // -------------------------------------------------------------------- regen

    /**
     * Slows the healing of a full food bar down to half a heart every four seconds.
     * <p>
     * Since 1.9 a full bar with saturation left heals a heart a second, and every respawn and every
     * golden apple comes with saturation - so whoever backs out of a fight for five seconds is back at
     * full health, and a fight that was nearly won starts over.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onRegen(EntityRegainHealthEvent event) {
        if (!on()) return;
        if (event.getRegainReason() != EntityRegainHealthEvent.RegainReason.SATIATED) return;
        if (!(event.getEntity() instanceof Player player)) return;

        int now = Bukkit.getCurrentTick();
        Integer last = lastRegen.get(player.getUniqueId());
        boolean due = last == null || now - last >= REGEN_INTERVAL_TICKS;
        if (due) {
            lastRegen.put(player.getUniqueId(), now);
            event.setAmount(1.0d);
        } else {
            event.setCancelled(true);
        }
        // the server takes the price of the fast healing either way, right after this. What is paid is
        // what 1.8 asked for the half heart, and nothing at all for a heal that did not happen
        float exhaustion = player.getExhaustion() + (due ? REGEN_EXHAUSTION : 0.0f);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) player.setExhaustion(exhaustion);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastRegen.remove(event.getPlayer().getUniqueId());
    }

    // ----------------------------------------------------------------- off hand

    /**
     * Keeps the off hand empty: there was one hand, and what is held in it is what is being used.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (!on() || event.getPlayer().getGameMode() == GameMode.CREATIVE) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onOffHandClick(InventoryClickEvent event) {
        if (!on() || event.getWhoClicked().getGameMode() == GameMode.CREATIVE) return;
        boolean offHandSlot = event.getClickedInventory() instanceof PlayerInventory
                && event.getSlot() == OFF_HAND_SLOT;
        if (event.getClick() == ClickType.SWAP_OFFHAND || offHandSlot) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onOffHandDrag(InventoryDragEvent event) {
        if (!on() || event.getWhoClicked().getGameMode() == GameMode.CREATIVE) return;
        // only somebody's own inventory shows the off hand, and there it is the one slot after everything else
        if (event.getView().getTopInventory().getType() != InventoryType.CRAFTING) return;
        if (event.getRawSlots().contains(45)) event.setCancelled(true);
    }

    private static boolean on() {
        Bedwars bedwars = Bedwars.getInstance();
        return bedwars != null && bedwars.getFeatureSettings().is(Feature.OLD_PVP);
    }
}

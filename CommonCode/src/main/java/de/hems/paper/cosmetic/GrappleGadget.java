package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.Cosmetics;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A fishing rod that pulls its owner to where the hook landed.
 * <p>
 * Two numbers make or break it. The pull needs a floor under its upward part, or a hook in the wall in
 * front of you drags you into that wall instead of up it; and the landing has to be free of fall damage
 * for a moment, because a mobility gadget that kills its owner every third use is one nobody puts on. The
 * window is short and it starts at the throw, so it cannot be used to walk off a tower safely - which is
 * the one thing it must not become in a round where the drop is the map.
 */
public class GrappleGadget implements Gadget, Listener {

    /** How hard it pulls when nobody set anything, in tenths of a block per tick. */
    private static final int DEFAULT_POWER = 12;
    /** How long before it can be used again, in ticks. */
    private static final int DEFAULT_COOLDOWN_TICKS = 60;
    /** How long after a throw its owner cannot take fall damage, in milliseconds. */
    private static final String SETTING_NO_FALL_MILLIS = "no-fall-millis";
    private static final int DEFAULT_NO_FALL_MILLIS = 6000;
    /** The least upward push every pull has, so a hook in a wall lifts rather than flattens. */
    private static final double LIFT = 0.42d;
    /** How close is too close to be worth pulling towards, in blocks squared. */
    private static final double TOO_CLOSE = 4.0d;

    /** Who is landing from a grapple right now, and until when. */
    private final Map<UUID, Long> landing = new ConcurrentHashMap<>();

    @Override
    public String getId() {
        return Cosmetics.GADGET_GRAPPLE;
    }

    @Override
    public ItemStack item(CosmeticData cosmetic) {
        ItemStack rod = new ItemStack(Material.FISHING_ROD, 1);
        ItemMeta meta = rod.getItemMeta();
        if (meta != null) {
            // unbreakable, because the gadget is the pull and not a rod with sixty-four uses, and a rod
            // that breaks mid round would leave its owner with a cosmetic they cannot use again
            meta.setUnbreakable(true);
            rod.setItemMeta(meta);
        }
        return rod;
    }

    @Override
    public @Nullable String hint() {
        return "Enterhaken: wirf den Haken und zieh dich dorthin - danach kurz Abklingzeit.";
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        switch (event.getState()) {
            case IN_GROUND, REEL_IN, FAILED_ATTEMPT, CAUGHT_ENTITY -> {
            }
            default -> {
                return;
            }
        }
        Player player = event.getPlayer();
        CosmeticData gadget = Gadgets.settingsFor(player, getId());
        if (gadget == null) return;
        if (player.hasCooldown(Material.FISHING_ROD)) return;

        Location hook = event.getHook().getLocation();
        if (hook.getWorld() == null || hook.getWorld() != player.getWorld()) return;
        Vector pull = hook.toVector().subtract(player.getLocation().toVector());
        // a hook at your own feet has no direction to pull in, and normalising it would be a division
        // by nearly zero - which is a player thrown at the sky by a rod they dropped
        if (pull.lengthSquared() < TOO_CLOSE) return;

        double power = Math.max(0.1d, gadget.getNumber(Cosmetics.SETTING_POWER, DEFAULT_POWER) / 10.0d);
        Vector velocity = pull.normalize().multiply(power);
        velocity.setY(Math.max(velocity.getY(), LIFT));
        player.setVelocity(velocity);

        int cooldown = Math.max(1,
                gadget.getNumber(Cosmetics.SETTING_COOLDOWN_TICKS, DEFAULT_COOLDOWN_TICKS));
        player.setCooldown(Material.FISHING_ROD, cooldown);
        landing.put(player.getUniqueId(), System.currentTimeMillis()
                + Math.max(0, gadget.getNumber(SETTING_NO_FALL_MILLIS, DEFAULT_NO_FALL_MILLIS)));
        player.playSound(player, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 0.8f, 1.4f);
    }

    /**
     * Takes the fall damage off the landing, and only the landing.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player player)) return;
        Long until = landing.remove(player.getUniqueId());
        if (until == null || until < System.currentTimeMillis()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        landing.remove(event.getPlayer().getUniqueId());
    }
}

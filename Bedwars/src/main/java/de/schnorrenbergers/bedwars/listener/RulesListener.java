package de.schnorrenbergers.bedwars.listener;

import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.config.Feature;
import de.schnorrenbergers.bedwars.config.FeatureSettings;
import de.schnorrenbergers.bedwars.game.CompassTracker;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.game.Rules;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

/**
 * What the switches of {@code features.yml} do while the round runs.
 * <p>
 * The parts that cannot be a game rule or an attribute live here: the sweep attack that 1.8 did not have,
 * the food bar, and the compass that points at whoever is closest.
 */
public class RulesListener implements Listener {

    /** How often a held compass is pointed at the nearest enemy again, in ticks. */
    private static final long TRACK_INTERVAL_TICKS = 10L;

    private final Plugin plugin;

    public RulesListener(Plugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, this::trackCompasses,
                TRACK_INTERVAL_TICKS, TRACK_INTERVAL_TICKS);
    }

    /**
     * Gives everybody who arrives the attack speed the server plays with.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Rules.applyTo(event.getPlayer(), features());
    }

    /**
     * Takes the sweep attack out, which is the half of 1.9 combat an attack speed alone leaves behind.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSweep(EntityDamageEvent event) {
        if (!features().is(Feature.OLD_PVP)) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) event.setCancelled(true);
    }

    /**
     * Keeps the food bar where it is while hunger is switched off.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (features().is(Feature.HUNGER)) return;
        if (!(event.getEntity() instanceof Player player)) return;
        // only downwards: a golden apple still has to be able to fill somebody up
        if (event.getFoodLevel() >= player.getFoodLevel()) return;
        event.setCancelled(true);
    }

    /**
     * Points every held compass at the closest enemy.
     * <p>
     * This is what is left of the locator bar once it is off: the same information, but it costs
     * something, it has to be held, and it only ever says one direction rather than all seven.
     */
    private void trackCompasses() {
        if (!features().is(Feature.COMPASS_TRACKER)) return;
        Game game = Bedwars.getInstance() == null ? null : Bedwars.getInstance().getGame();
        if (game == null || !game.isRunning()) return;
        for (GamePlayer participant : game.getPlayers()) {
            Player player = participant.getPlayer();
            if (player == null || !participant.isAlive() || !holdsCompass(player)) continue;
            player.sendActionBar(CompassTracker.actionBar(CompassTracker.aim(game, player)));
        }
    }

    /**
     * Points a compass at the next team when its holder right clicks with it.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCompass(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!features().is(Feature.COMPASS_TRACKER)) return;
        if (!isCompass(event.getItem())) return;
        Game game = Bedwars.getInstance().getGame();
        GamePlayer holder = game == null ? null : game.get(event.getPlayer());
        if (game == null || !game.isRunning() || holder == null || !holder.isAlive()) return;
        event.setCancelled(true);
        CompassTracker.cycle(game, event.getPlayer());
    }

    /**
     * @param player who to look at
     * @return whether they are holding a compass in either hand
     */
    private static boolean holdsCompass(Player player) {
        return isCompass(player.getInventory().getItemInMainHand())
                || isCompass(player.getInventory().getItemInOffHand());
    }

    private static boolean isCompass(@Nullable ItemStack stack) {
        return stack != null && stack.getType() == Material.COMPASS;
    }

    private static FeatureSettings features() {
        return Bedwars.getInstance().getFeatureSettings();
    }
}

package de.schnorrenbergers.bedwars.listener;

import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.game.timeline.SuddenDeath;
import de.schnorrenbergers.bedwars.util.Messages;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

/**
 * The rules that make a dragon and a wither belong to a team.
 * <p>
 * A summoned boss attacks whoever is closest, which in a bedwars base is the team that just paid for it.
 * So neither the dragons nor the withers hurt or hunt their own team - and because a breath, a fireball, a
 * wither skull and the cloud a fireball leaves behind are all separate entities, every one of them has to
 * be traced back to the boss that started it.
 */
public class SuddenDeathListener implements Listener {

    public SuddenDeathListener(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Keeps a boss from hurting the team it fights for.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Game game = game();
        if (game == null) return;

        GameTeam owner = SuddenDeath.ownerOf(game, sourceOf(event.getDamager()));
        if (owner == null) return;
        GamePlayer hurt = game.get(victim);
        if (hurt != null && owner.equals(hurt.getTeam())) event.setCancelled(true);
    }

    /**
     * Keeps a boss from picking somebody of its own team as its next target.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        Game game = game();
        if (game == null) return;
        GameTeam owner = SuddenDeath.ownerOf(game, event.getEntity());
        if (owner == null) return;
        if (!(event.getTarget() instanceof Player target)) {
            // players or nothing. A wither left to itself shoots at the shop villagers, which are
            // invulnerable, so it would spend the sudden death firing at something it cannot kill
            if (event.getTarget() != null) event.setTarget(null);
            return;
        }
        GamePlayer hunted = game.get(target);
        // no target rather than a cancelled event: cancelling would leave whatever it was hunting
        // before in place, and that is just as likely to be somebody of its own team
        if (hunted != null && owner.equals(hunted.getTeam())) event.setTarget(null);
    }

    /**
     * Says whose boss has been brought down - it took a team the whole sudden death to do it, and
     * without a line in chat nobody but the person who landed the last hit would know.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        Game game = game();
        GameTeam owner = SuddenDeath.ownerOf(game, event.getEntity());
        if (owner == null) return;
        event.getDrops().clear();
        event.setDroppedExp(0);
        Messages.broadcast(event.getEntity() instanceof EnderDragon ? "dragon.killed" : "wither.killed",
                "team", owner.getColor().getDisplayName());
    }

    /**
     * @param damager whatever did the damage
     * @return the boss behind it: itself, what it shot, or the cloud that shot left
     */
    private static @Nullable Entity sourceOf(Entity damager) {
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity shooter) {
            return shooter;
        }
        if (damager instanceof AreaEffectCloud cloud && cloud.getSource() instanceof LivingEntity source) {
            return source;
        }
        return damager;
    }

    private static @Nullable Game game() {
        Bedwars plugin = Bedwars.getInstance();
        return plugin == null ? null : plugin.getGame();
    }
}

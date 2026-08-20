package de.schnorrenbergers.bedwars.listener;

import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.game.timeline.Dragons;
import de.schnorrenbergers.bedwars.util.Messages;
import org.bukkit.entity.AreaEffectCloud;
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
 * The rules that make a dragon belong to a team.
 * <p>
 * A summoned dragon attacks whoever is closest, which in a bedwars base is the team that just paid five
 * emeralds for it. So a dragon neither hurts nor hunts its own team - and because its breath, its fireball
 * and the cloud they leave behind are three separate entities, all three have to be traced back to the
 * dragon that started them.
 */
public class DragonListener implements Listener {

    public DragonListener(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Keeps a dragon from hurting the team it fights for.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Dragons dragons = dragons();
        Game game = game();
        if (dragons == null || game == null || !dragons.isFlying()) return;

        GameTeam owner = dragons.ownerOf(sourceOf(event.getDamager()));
        if (owner == null) return;
        GamePlayer hurt = game.get(victim);
        if (hurt != null && owner.equals(hurt.getTeam())) event.setCancelled(true);
    }

    /**
     * Keeps a dragon from picking somebody of its own team as its next target.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        Dragons dragons = dragons();
        Game game = game();
        if (dragons == null || game == null || !dragons.isFlying()) return;
        GameTeam owner = dragons.ownerOf(event.getEntity());
        if (owner == null) return;
        if (!(event.getTarget() instanceof Player target)) return;
        GamePlayer hunted = game.get(target);
        // no target rather than a cancelled event: cancelling would leave whatever it was hunting
        // before in place, and that is just as likely to be somebody of its own team
        if (hunted != null && owner.equals(hunted.getTeam())) event.setTarget(null);
    }

    /**
     * Says whose dragon has been brought down - it took a team the whole sudden death to do it, and
     * without a line in chat nobody but the person who landed the last hit would know.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        Dragons dragons = dragons();
        if (dragons == null) return;
        GameTeam owner = dragons.ownerOf(event.getEntity());
        if (owner == null) return;
        event.getDrops().clear();
        event.setDroppedExp(0);
        Messages.broadcast("dragon.killed", "team", owner.getColor().getDisplayName());
    }

    /**
     * @param damager whatever did the damage
     * @return the dragon behind it: itself, the fireball it shot, or the cloud that fireball left
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

    private static @Nullable Dragons dragons() {
        Game game = game();
        return game == null ? null : game.getDragons();
    }

    private static @Nullable Game game() {
        Bedwars plugin = Bedwars.getInstance();
        return plugin == null ? null : plugin.getGame();
    }
}

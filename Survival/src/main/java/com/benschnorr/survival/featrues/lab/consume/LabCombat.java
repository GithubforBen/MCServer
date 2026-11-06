package com.benschnorr.survival.featrues.lab.consume;

import com.benschnorr.survival.featrues.lab.LabSettings;
import com.benschnorr.survival.featrues.lab.consume.LabConsumeRules.CombatClock;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

/**
 * Combat between players: a hit dealt or taken (also by arrow or other projectile of a player) counts, a hit
 * by a mob does not. When combat starts, Speed and Haste that Kokain gave above level II drop to II and keep
 * their remaining time.
 *
 * <p>Our effect is told from the same effect of a potion or a beacon by the record in the player's data: the
 * level must match and it must end when Kokain's duration ends (within two seconds). Anything else is left
 * alone, so a beacon's Haste II or a potion's Speed is never touched.
 */
final class LabCombat implements Listener {

    private final LabConsumeState state;
    private final CombatClock<UUID> clock;

    LabCombat(LabSettings settings, LabConsumeState state) {
        this.state = state;
        this.clock = new CombatClock<>(settings.combatSeconds());
    }

    boolean inCombat(Player player) {
        return clock.inCombat(player.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        Player victim = event.getEntity() instanceof Player p ? p : null;
        Player attacker = playerOf(event.getDamager());
        boolean same = victim != null && victim.equals(attacker);
        if (!LabConsumeRules.countsAsCombat(attacker != null, victim != null, same)) return;

        long now = System.currentTimeMillis();
        clock.hit(victim.getUniqueId(), now);
        clock.hit(attacker.getUniqueId(), now);
        capEffects(victim, now);
        capEffects(attacker, now);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clock.forget(event.getPlayer().getUniqueId());
    }

    private static Player playerOf(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    private void capEffects(Player player, long now) {
        cap(player, PotionEffectType.SPEED, true, now);
        cap(player, PotionEffectType.HASTE, false, now);
    }

    private void cap(Player player, PotionEffectType type, boolean speed, long now) {
        LabConsumeState.Recorded recorded = state.effect(player, speed);
        if (recorded == null || recorded.amplifier() + 1 <= LabConsumeRules.COMBAT_MAX_LEVEL) return;
        PotionEffect active = player.getPotionEffect(type);
        if (active == null || active.isInfinite()) return;
        long activeEnd = now + active.getDuration() * 50L;
        if (!LabConsumeRules.isOurs(active.getAmplifier(), activeEnd, recorded.amplifier(), recorded.endMillis(),
                LabConsume.TOLERANCE_MILLIS)) {
            return;
        }
        int capped = LabConsumeRules.cap(recorded.amplifier() + 1, true) - 1;
        player.removePotionEffect(type);
        player.addPotionEffect(new PotionEffect(type, active.getDuration(), capped));
        state.setEffect(player, speed, capped, recorded.endMillis());
    }
}

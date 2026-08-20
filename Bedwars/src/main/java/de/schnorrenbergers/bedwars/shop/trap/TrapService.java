package de.schnorrenbergers.bedwars.shop.trap;

import de.schnorrenbergers.bedwars.config.UpgradeSettings;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.game.TeamColor;
import de.schnorrenbergers.bedwars.map.TeamSpot;
import de.schnorrenbergers.bedwars.util.Messages;
import de.schnorrenbergers.bedwars.util.Text;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

/**
 * The traps: buying them into the queue, and setting them off.
 * <p>
 * A trap is the only thing in the shop that is bought for a moment that has not happened yet, which is why
 * it needs a watcher rather than a listener - nothing in bukkit fires when somebody walks into a base. The
 * watch runs every half second on the players who are actually in the round, which is a handful of
 * distance checks and cheap enough to do for the whole round.
 */
public class TrapService {

    /** How often the bases are checked for intruders, in ticks. */
    private static final int CHECK_INTERVAL = 10;

    private final UpgradeSettings settings;
    /** Team to the tick its last trap went off, so a queue is not emptied in one walk-through. */
    private final Map<TeamColor, Long> lastTrigger = new EnumMap<>(TeamColor.class);

    public TrapService(UpgradeSettings settings) {
        this.settings = settings;
    }

    public UpgradeSettings getSettings() {
        return settings;
    }

    // -------------------------------------------------------------------- buying

    /**
     * Puts a trap at the end of the buyer's team queue.
     *
     * @param game  the round
     * @param buyer who pays
     * @param trap  which trap
     * @return whether it worked
     */
    public boolean buy(Game game, GamePlayer buyer, Trap trap) {
        Player player = buyer.getPlayer();
        GameTeam team = buyer.getTeam();
        if (player == null || team == null || !buyer.isAlive()) return false;

        int queued = team.getTraps().size();
        if (queued >= settings.getQueueSize()) {
            Messages.send(player, "trap.queue-full", "maximum", String.valueOf(settings.getQueueSize()));
            return false;
        }
        int price = settings.getTrapPrice(queued);
        if (!settings.getTrapCurrency().take(player, price)) {
            Messages.send(player, "shop.cannot-afford",
                    "amount", String.valueOf(price - settings.getTrapCurrency().count(player)),
                    "currency", settings.getTrapCurrency().getDisplayName());
            player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }
        team.addTrap(trap.id(), settings.getQueueSize());
        for (GamePlayer member : team.getMembers()) {
            Player online = member.getPlayer();
            if (online == null) continue;
            Messages.send(online, "trap.bought",
                    "player", buyer.getName(),
                    "trap", Text.plain(trap.displayName()),
                    "position", String.valueOf(queued + 1));
        }
        player.playSound(player, Sound.BLOCK_TRIPWIRE_CLICK_ON, 1.0f, 1.0f);
        return true;
    }

    // ------------------------------------------------------------------ watching

    /**
     * Looks for somebody standing in a base that has a trap waiting.
     *
     * @param game  the round
     * @param ticks where the loop stands
     */
    public void tick(Game game, long ticks) {
        if (ticks % CHECK_INTERVAL != 0L) return;
        if (game.getArena() == null || game.getWorld() == null) return;

        for (GameTeam team : game.getTeams()) {
            if (!team.isAlive() || team.getTraps().isEmpty()) continue;
            if (waiting(team, ticks)) continue;
            TeamSpot spot = game.getArena().getTeam(team.getColor());
            if (spot == null || spot.getSpawn() == null) continue;
            Location base = spot.getSpawn().toLocation(game.getWorld());
            double radius = settings.getTrapRadius(spot.getProtection());
            if (radius <= 0.0d) continue;

            GamePlayer intruder = findIntruder(game, team, base, radius, ticks);
            if (intruder == null) continue;
            lastTrigger.put(team.getColor(), ticks);
            trigger(team, intruder);
        }
    }

    /**
     * @param team  whose queue
     * @param ticks where the loop stands
     * @return whether this team's last trap went off too recently for the next one
     */
    private boolean waiting(GameTeam team, long ticks) {
        Long last = lastTrigger.get(team.getColor());
        return last != null && ticks - last < settings.getTrapCooldownSeconds() * 20L;
    }

    /**
     * @return the first enemy standing in the base, or {@code null} when nobody is
     */
    private GamePlayer findIntruder(Game game, GameTeam team, Location base, double radius, long ticks) {
        for (GamePlayer participant : game.getOnlinePlayers()) {
            if (!participant.isAlive() || team.contains(participant)) continue;
            if (participant.getLoadout().isTrapImmune(ticks)) continue;
            Player player = participant.getPlayer();
            if (player == null || !player.getWorld().equals(base.getWorld())) continue;
            if (player.getLocation().distanceSquared(base) <= radius * radius) return participant;
        }
        return null;
    }

    /**
     * Sets the next trap of a team off.
     *
     * @param team     whose base was walked into
     * @param intruder who walked in
     */
    private void trigger(GameTeam team, GamePlayer intruder) {
        Trap trap = settings.getTrap(team.pollTrap());
        if (trap == null) return;
        Player victim = intruder.getPlayer();
        if (victim == null) return;
        int duration = trap.seconds() * 20;

        switch (trap.effect()) {
            case BLINDNESS -> {
                victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration,
                        trap.amplifier(), false, true, true));
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration,
                        trap.amplifier(), false, true, true));
            }
            case MINING_FATIGUE -> victim.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE,
                    duration, trap.amplifier(), false, true, true));
            case ALARM -> victim.removePotionEffect(PotionEffectType.INVISIBILITY);
            case COUNTER_OFFENSIVE -> defend(team, duration, trap.amplifier());
            case NONE -> {
            }
        }
        announce(team, trap, intruder);
    }

    /**
     * Gives the team at home what a counter-offensive trap is bought for.
     */
    private void defend(GameTeam team, int duration, int amplifier) {
        for (GamePlayer member : team.getAliveMembers()) {
            Player player = member.getPlayer();
            if (player == null) continue;
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, amplifier,
                    false, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration, amplifier,
                    false, true, true));
        }
    }

    /**
     * Tells the team what went off and, for the alarm trap, who set it off.
     */
    private void announce(GameTeam team, Trap trap, GamePlayer intruder) {
        boolean alarm = trap.effect() == Trap.Effect.ALARM;
        for (GamePlayer member : team.getMembers()) {
            Player player = member.getPlayer();
            if (player == null) continue;
            player.showTitle(Title.title(
                    Messages.get("trap.title"),
                    Messages.get("trap.subtitle", "trap", Text.plain(trap.displayName())),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1600), Duration.ofMillis(400))));
            Messages.send(player, alarm ? "trap.set-off.alarm" : "trap.set-off",
                    "trap", Text.plain(trap.displayName()),
                    "player", intruder.getName(),
                    "team", intruder.getTeam() == null
                            ? Messages.raw("chat.no-team")
                            : intruder.getTeam().getColor().getDisplayName());
            player.playSound(player, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.4f);
        }
    }
}

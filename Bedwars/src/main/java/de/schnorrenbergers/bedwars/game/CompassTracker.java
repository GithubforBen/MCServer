package de.schnorrenbergers.bedwars.game;

import de.schnorrenbergers.bedwars.shop.Cost;
import de.schnorrenbergers.bedwars.shop.Currency;
import de.schnorrenbergers.bedwars.util.Messages;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The compass that points at one team.
 * <p>
 * It is the honest version of the locator bar: the same knowledge, but somebody has to pay for it, hold it
 * in a hand that could be holding a sword, and pick <em>one</em> team at a time. Pointing it at another
 * team costs an emerald, and only the first time - a team you have paid to see stays visible for the rest
 * of the round, so the compass gets better the longer you carry it.
 */
public final class CompassTracker {

    /** What one more team on the compass costs. */
    private static final Cost PER_TEAM = new Cost(Currency.EMERALD, 1);

    /** Who is pointing at whom. */
    private static final Map<UUID, TeamColor> pointedAt = new HashMap<>();
    /** And which teams they have already paid to see. */
    private static final Map<UUID, Set<TeamColor>> paid = new HashMap<>();

    private CompassTracker() {
    }

    /**
     * Points a player's compass at the next team, taking an emerald for a team they have not seen yet.
     *
     * @param game   the round
     * @param player who is holding the compass
     */
    public static void cycle(Game game, Player player) {
        List<GameTeam> targets = enemyTeams(game, player);
        if (targets.isEmpty()) {
            Messages.send(player, "compass.nobody-left");
            return;
        }
        GameTeam next = after(targets, pointedAt.get(player.getUniqueId()));
        Set<TeamColor> already = paid.computeIfAbsent(player.getUniqueId(),
                key -> EnumSet.noneOf(TeamColor.class));

        if (!already.contains(next.getColor())) {
            if (Cost.shortfall(player, List.of(PER_TEAM)) != null) {
                Messages.send(player, "compass.cannot-afford",
                        "team", next.getColor().getDisplayName(),
                        "amount", String.valueOf(PER_TEAM.amount()),
                        "currency", PER_TEAM.currency().getDisplayName());
                player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            Cost.take(player, List.of(PER_TEAM));
            already.add(next.getColor());
            player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.6f);
        } else {
            player.playSound(player, Sound.UI_BUTTON_CLICK, 1.0f, 1.4f);
        }
        pointedAt.put(player.getUniqueId(), next.getColor());
        Messages.send(player, "compass.now-tracking", "team", next.getColor().getDisplayName());
    }

    /**
     * Turns the needle, once for every player who is holding one.
     *
     * @param game   the round
     * @param player who is holding the compass
     * @return the team it is pointing at, or {@code null} when it is pointing at nothing yet
     */
    public static @Nullable GameTeam aim(Game game, Player player) {
        TeamColor colour = pointedAt.get(player.getUniqueId());
        GameTeam team = colour == null ? null : game.getTeam(colour);
        if (team == null || !team.isAlive()) return null;
        Location target = nearestOf(game, team, player);
        if (target == null) return null;
        player.setCompassTarget(target);
        return team;
    }

    /**
     * @return the line the holder is shown while the compass is in their hand
     */
    public static Component actionBar(GameTeam team) {
        return team == null
                ? Messages.get("compass.idle")
                : Messages.get("compass.tracking", "team", team.getColor().getDisplayName());
    }

    /**
     * Forgets a player, for the end of a round.
     */
    public static void forget(UUID player) {
        pointedAt.remove(player);
        paid.remove(player);
    }

    /**
     * @return every team that is still alive and is not the holder's own
     */
    private static List<GameTeam> enemyTeams(Game game, Player player) {
        GamePlayer holder = game.get(player);
        GameTeam own = holder == null ? null : holder.getTeam();
        List<GameTeam> targets = new ArrayList<>();
        for (GameTeam team : game.getAliveTeams()) {
            if (own != null && own.equals(team)) continue;
            if (!team.isEmpty()) targets.add(team);
        }
        return targets;
    }

    /**
     * @param targets the teams to cycle through
     * @param current what the compass points at now
     * @return the next one, wrapping around
     */
    private static GameTeam after(List<GameTeam> targets, @Nullable TeamColor current) {
        for (int i = 0; i < targets.size(); i++) {
            if (targets.get(i).getColor() == current) return targets.get((i + 1) % targets.size());
        }
        return targets.getFirst();
    }

    /**
     * @return where the closest living member of a team is, or {@code null} when none is reachable
     */
    private static @Nullable Location nearestOf(Game game, GameTeam team, Player player) {
        Location closest = null;
        double distance = Double.MAX_VALUE;
        for (GamePlayer member : team.getMembers()) {
            if (!member.isAlive()) continue;
            Player target = member.getPlayer();
            if (target == null || !target.getWorld().equals(player.getWorld())) continue;
            double to = target.getLocation().distanceSquared(player.getLocation());
            if (to >= distance) continue;
            distance = to;
            closest = target.getLocation();
        }
        return closest;
    }
}

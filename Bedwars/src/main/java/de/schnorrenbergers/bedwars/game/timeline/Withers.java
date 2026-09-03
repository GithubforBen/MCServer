package de.schnorrenbergers.bedwars.game.timeline;

import de.schnorrenbergers.bedwars.config.TimelineSettings;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.shop.upgrade.Upgrade;
import de.schnorrenbergers.bedwars.util.Messages;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The withers of the sudden death.
 * <p>
 * The dragons are what the event opens with; the withers are what it does not stop doing. A few minutes
 * after sudden death every living team is given one, and another one every minute after that, so a round
 * that nobody can finish gets louder until it finishes itself. A team that bought the wither buff gets one
 * more per level and per wave, which is the only upgrade in the shop that is worth anything this late.
 * <p>
 * Like the dragons they belong to a team: a team's own withers neither shoot at it nor hurt it, which is
 * what makes buying them a decision rather than a threat to everybody equally.
 */
public final class Withers {

    /** How often the dead ones are cleared out and the next wave is checked for, in ticks. */
    private static final int CHECK_INTERVAL = 20;
    /** How far apart one wave is spread over the middle, in blocks. */
    private static final double SPREAD = 6.0d;

    private final TimelineSettings settings;
    /** Wither to the team it fights for. */
    private final Map<UUID, GameTeam> owners = new HashMap<>();
    private final List<Wither> spawned = new ArrayList<>();

    private Location centre;
    /** The tick the sudden death began on, -1 while it has not. */
    private long startTick = -1L;
    /** How many waves have gone out; also which one is next. */
    private int waves;

    public Withers(TimelineSettings settings) {
        this.settings = settings;
    }

    /**
     * Starts the countdown to the first wave. Nothing is spawned yet - that is the point of the delay.
     *
     * @param game  the round
     * @param ticks where the loop stands
     */
    public void start(Game game, long ticks) {
        remove();
        centre = game.getMiddle();
        startTick = ticks;
        waves = 0;
    }

    /**
     * Lets the next wave out when its minute has come.
     *
     * @param game  the round
     * @param ticks where the loop stands
     */
    public void tick(Game game, long ticks) {
        if (startTick < 0L || centre == null) return;
        if (ticks % CHECK_INTERVAL != 0L) return;
        spawned.removeIf(wither -> {
            if (wither.isValid()) return false;
            owners.remove(wither.getUniqueId());
            return true;
        });

        long due = startTick + (long) settings.getWitherDelaySeconds() * 20L
                + (long) waves * settings.getWitherIntervalSeconds() * 20L;
        if (ticks < due) return;
        waves++;
        release(game);
    }

    /**
     * Gives every living team its withers for this wave.
     *
     * @param game the round
     */
    private void release(Game game) {
        int total = 0;
        for (GameTeam team : game.getAliveTeams()) {
            int count = count(game, team);
            for (int i = 0; i < count; i++) {
                if (place(game, team, i, count)) total++;
            }
        }
        if (total == 0) return;
        Messages.broadcast("wither.wave", "amount", String.valueOf(total));
    }

    /**
     * @param game the round
     * @param team whose withers
     * @return how many that team gets this wave, one more per level of the wither buff it bought, and
     *         never more than the configured maximum however much it bought
     */
    private int count(Game game, GameTeam team) {
        int base = settings.getWithersPerTeam();
        if (game.getUpgrades() != null) {
            int buff = game.getUpgrades().levelOf(team, Upgrade.Effect.WITHER_BUFF);
            base += buff * settings.getWitherBuffWithers();
        }
        return Math.max(0, Math.min(settings.getWitherMaximum(), base));
    }

    /**
     * Puts one wither over the middle.
     *
     * @param game  the round
     * @param team  whose it is
     * @param index which one of the wave it is
     * @param count how many the wave holds, so fifteen of them do not all arrive in the same block
     * @return whether it made it into the world
     */
    private boolean place(Game game, GameTeam team, int index, int count) {
        World world = centre.getWorld();
        if (world == null) return false;
        double angle = count <= 1 ? 0.0d : 2.0d * Math.PI * index / count;
        Location at = centre.clone().add(
                Math.cos(angle) * SPREAD, settings.getDragonHeight(), Math.sin(angle) * SPREAD);

        Wither wither = world.spawn(at, Wither.class, entity -> {
            // a wither that is summoned rather than built has no business with the blue spawn phase, and
            // fifteen of them going invulnerable together would be fifteen explosions over the middle
            entity.setInvulnerableTicks(0);
            entity.setRemoveWhenFarAway(false);
            entity.setPersistent(false);
            entity.setCanTravelThroughPortals(false);
            entity.customName(Messages.get("wither.name", "team", team.getColor().getDisplayName()));
            entity.setCustomNameVisible(true);
            AttributeInstance health = entity.getAttribute(Attribute.MAX_HEALTH);
            if (health != null) health.setBaseValue(settings.getWitherHealth());
        });
        AttributeInstance maximum = wither.getAttribute(Attribute.MAX_HEALTH);
        if (maximum != null) wither.setHealth(maximum.getValue());
        // no bar. A wither carries its own, and fifteen of them per team would be sixty bars stacked
        // down the screen with the game somewhere behind them - the name tag says whose it is
        if (wither.getBossBar() != null) wither.getBossBar().setVisible(false);
        owners.put(wither.getUniqueId(), team);
        spawned.add(wither);

        // pointed at somebody straight away rather than left to find its own: a wither that spawns over an
        // empty middle would spend its first seconds looking for the nearest player, and the nearest
        // player is as often as not the team that paid for it
        Player enemy = nearestEnemy(game, wither, team);
        if (enemy != null) wither.setTarget(enemy);
        return true;
    }

    /**
     * @param game   the round
     * @param wither which one is looking
     * @param owner  the team it will not touch
     * @return the closest player it is allowed to go for, {@code null} when there is none in its world
     */
    private static @Nullable Player nearestEnemy(Game game, Wither wither, GameTeam owner) {
        Player closest = null;
        double best = Double.MAX_VALUE;
        for (GameTeam team : game.getAliveTeams()) {
            if (owner.equals(team)) continue;
            for (GamePlayer member : team.getAliveMembers()) {
                Player player = member.getPlayer();
                if (player == null || !player.getWorld().equals(wither.getWorld())) continue;
                double distance = player.getLocation().distanceSquared(wither.getLocation());
                if (distance >= best) continue;
                best = distance;
                closest = player;
            }
        }
        return closest;
    }

    // ----------------------------------------------------------------- lookups

    /**
     * @param entity something that hit or was hit
     * @return the team its wither fights for, or {@code null} when it is not one of ours
     */
    public @Nullable GameTeam ownerOf(@Nullable Entity entity) {
        return entity == null ? null : owners.get(entity.getUniqueId());
    }

    /**
     * @return whether the waves have started, which is not the same as any wither being alive
     */
    public boolean isRunning() {
        return startTick >= 0L;
    }

    /**
     * @return how many withers are out there right now
     */
    public int getAliveCount() {
        return spawned.size();
    }

    /**
     * Takes every wither out of the world and stops the waves.
     */
    public void remove() {
        for (Wither wither : spawned) {
            if (wither.isValid()) wither.remove();
        }
        spawned.clear();
        owners.clear();
        centre = null;
        startTick = -1L;
        waves = 0;
    }
}

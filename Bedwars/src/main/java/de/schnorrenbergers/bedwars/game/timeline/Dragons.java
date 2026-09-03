package de.schnorrenbergers.bedwars.game.timeline;

import de.schnorrenbergers.bedwars.config.TimelineSettings;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.shop.upgrade.Upgrade;
import de.schnorrenbergers.bedwars.util.Messages;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The dragons of the sudden death.
 * <p>
 * A dragon belongs to a team, which is the whole difference between this and summoning one: it does not
 * touch the people who paid for it, and it goes after the ones who did not. A vanilla dragon circles the
 * podium of its own end fight and only bothers whoever walks underneath it - in an arena that is a dragon
 * flying laps over an empty middle while everybody sits in their base. So the round moves the podium onto
 * whoever the dragon is hunting, and takes the map apart underneath it.
 */
public final class Dragons {

    /** How often the dragons are checked for having drifted off, in ticks. */
    private static final int CHECK_INTERVAL = 20;
    /** How often they pick who to go for, in ticks. */
    private static final int HUNT_INTERVAL = 40;
    /** How often they tear blocks out of what they are flying through, in ticks. */
    private static final int CARVE_INTERVAL = 5;
    /** How often their bars are redrawn, in ticks. A health bar that jumps once a second reads as broken. */
    private static final int BAR_INTERVAL = 4;

    private final TimelineSettings settings;
    /** Dragon to the team it fights for. */
    private final Map<UUID, GameTeam> owners = new HashMap<>();
    /** The bar over everybody's screen, per dragon. The vanilla one only exists in the End. */
    private final Map<UUID, BossBar> bars = new HashMap<>();
    /** Dragon to whoever it is going for right now. */
    private final Map<UUID, UUID> prey = new HashMap<>();
    private final List<EnderDragon> spawned = new ArrayList<>();

    private Location centre;

    public Dragons(TimelineSettings settings) {
        this.settings = settings;
    }

    /**
     * Puts a dragon over the map for every team that is still in the round.
     *
     * @param game the round
     * @return how many were let loose
     */
    public int spawn(Game game) {
        remove();
        centre = game.getMiddle();
        if (centre == null) return 0;
        for (GameTeam team : game.getAliveTeams()) {
            for (int i = 0; i < count(game, team); i++) {
                place(team);
            }
        }
        return spawned.size();
    }

    /**
     * @param game the round
     * @param team whose dragons
     * @return how many that team gets, which is one more per level of the dragon buff it bought
     */
    private int count(Game game, GameTeam team) {
        int base = settings.getDragonsPerTeam();
        if (game.getUpgrades() == null) return base;
        int buff = game.getUpgrades().levelOf(team, Upgrade.Effect.DRAGON_BUFF);
        return base + buff * settings.getDragonBuffDragons();
    }

    /**
     * Spawns one dragon over the middle.
     */
    private void place(GameTeam team) {
        World world = centre.getWorld();
        if (world == null) return;
        Location at = centre.clone().add(0.0d, settings.getDragonHeight(), 0.0d);
        EnderDragon dragon = world.spawn(at, EnderDragon.class, entity -> {
            // without a podium the dragon heads for where the exit portal of an end world would be, which
            // in an arena is a spot in the void nobody can follow it to
            entity.setPodium(centre);
            entity.setPhase(EnderDragon.Phase.CIRCLING);
            entity.setRemoveWhenFarAway(false);
            entity.setPersistent(false);
            entity.customName(Messages.get("dragon.name", "team", team.getColor().getDisplayName()));
            entity.setCustomNameVisible(true);
            AttributeInstance health = entity.getAttribute(Attribute.MAX_HEALTH);
            if (health != null) health.setBaseValue(settings.getDragonHealth());
        });
        // full, at whatever the config made the maximum: a dragon spawned with the vanilla 200 of health
        // would ignore a raised limit until something healed it
        AttributeInstance maximum = dragon.getAttribute(Attribute.MAX_HEALTH);
        if (maximum != null) dragon.setHealth(maximum.getValue());
        owners.put(dragon.getUniqueId(), team);
        spawned.add(dragon);
        showBar(dragon, team);
    }

    /**
     * Puts a bar for one dragon over everybody's screen.
     * <p>
     * A dragon carries its own bar in the End and nowhere else - the bar belongs to the End's dragon
     * fight, not to the mob. In an arena there is no fight to hang it off, so the round draws its own,
     * which is also the only way to say which team a dragon belongs to at a glance.
     */
    private void showBar(EnderDragon dragon, GameTeam team) {
        BossBar bar = BossBar.bossBar(
                Messages.get("dragon.bar", "team", team.getColor().getDisplayName()),
                1.0f, BossBar.Color.PURPLE, BossBar.Overlay.NOTCHED_10);
        bars.put(dragon.getUniqueId(), bar);
        Bukkit.getServer().showBossBar(bar);
    }

    /**
     * Redraws the bars from the health of the dragons they belong to.
     */
    private void updateBars() {
        for (EnderDragon dragon : spawned) {
            BossBar bar = bars.get(dragon.getUniqueId());
            if (bar == null || !dragon.isValid()) continue;
            AttributeInstance maximum = dragon.getAttribute(Attribute.MAX_HEALTH);
            double top = maximum == null ? dragon.getHealth() : maximum.getValue();
            if (top <= 0.0d) continue;
            bar.progress((float) Math.max(0.0d, Math.min(1.0d, dragon.getHealth() / top)));
        }
    }

    /**
     * Takes one dragon's bar off every screen.
     */
    private void hideBar(UUID dragon) {
        BossBar bar = bars.remove(dragon);
        if (bar != null) Bukkit.getServer().hideBossBar(bar);
    }

    /**
     * Keeps the dragons hunting, tears the map apart underneath them, and forgets the ones that died.
     *
     * @param game  the round
     * @param ticks where the loop stands
     */
    public void tick(Game game, long ticks) {
        if (spawned.isEmpty() || centre == null) return;
        if (ticks % BAR_INTERVAL == 0L) updateBars();
        if (ticks % CARVE_INTERVAL == 0L) carve(game);
        if (ticks % HUNT_INTERVAL == 0L) hunt(game);
        if (ticks % CHECK_INTERVAL != 0L) return;

        double radius = settings.getDragonRadius();
        spawned.removeIf(dragon -> {
            if (!dragon.isValid()) {
                owners.remove(dragon.getUniqueId());
                prey.remove(dragon.getUniqueId());
                hideBar(dragon.getUniqueId());
                return true;
            }
            keepFlying(dragon);
            // back to whatever it is going for, which is its prey while it has one and the middle of the
            // map while it has not. Measuring against the middle instead would drag a dragon off a base
            // in the corner of the map every time it got there
            Location anchor = anchorOf(dragon);
            Location at = dragon.getLocation();
            if (at.getWorld() != null && at.getWorld().equals(anchor.getWorld())
                    && at.distanceSquared(anchor) <= radius * radius) {
                return false;
            }
            dragon.teleport(anchor.clone().add(0.0d, settings.getDragonHeight(), 0.0d));
            dragon.setPhase(EnderDragon.Phase.CIRCLING);
            return false;
        });
    }

    // ------------------------------------------------------------------ hunting

    /**
     * Points every dragon at the nearest player who is not on its own team.
     * <p>
     * A dragon has no target the way an ordinary mob has one: it flies laps around its podium and picks up
     * whoever happens to be near it. So the podium is what gets moved - onto the head of whoever it is
     * hunting, once every two seconds. From the ground that is a dragon that comes for you and stays.
     *
     * @param game the round
     */
    private void hunt(Game game) {
        for (EnderDragon dragon : spawned) {
            if (!dragon.isValid()) continue;
            GameTeam owner = owners.get(dragon.getUniqueId());
            Player target = nearestEnemy(game, dragon, owner);
            if (target == null) {
                prey.remove(dragon.getUniqueId());
                dragon.setPodium(centre);
                continue;
            }
            prey.put(dragon.getUniqueId(), target.getUniqueId());
            // the podium is where the dragon wants to be; the phase is what it does when it gets there.
            // Both, because a dragon left circling would take a lap before it noticed it had moved
            dragon.setPodium(target.getLocation());
            dragon.setTarget(target);
            if (!hunting(dragon)) dragon.setPhase(EnderDragon.Phase.CHARGE_PLAYER);
        }
    }

    /**
     * @param game   the round
     * @param dragon which dragon is looking
     * @param owner  the team it will not touch
     * @return the closest player it is allowed to go for, {@code null} when there is none in its world
     */
    private static @Nullable Player nearestEnemy(Game game, EnderDragon dragon, @Nullable GameTeam owner) {
        Player closest = null;
        double best = Double.MAX_VALUE;
        for (GameTeam team : game.getAliveTeams()) {
            if (owner != null && owner.equals(team)) continue;
            for (GamePlayer member : team.getAliveMembers()) {
                Player player = member.getPlayer();
                if (player == null || !player.getWorld().equals(dragon.getWorld())) continue;
                double distance = player.getLocation().distanceSquared(dragon.getLocation());
                if (distance >= best) continue;
                best = distance;
                closest = player;
            }
        }
        return closest;
    }

    /**
     * @param dragon one of ours
     * @return where it belongs right now: over its prey, or over the middle while it has none
     */
    private Location anchorOf(EnderDragon dragon) {
        UUID hunted = prey.get(dragon.getUniqueId());
        Player player = hunted == null ? null : Bukkit.getPlayer(hunted);
        if (player == null || !player.getWorld().equals(centre.getWorld())) return centre;
        return player.getLocation();
    }

    /**
     * @param dragon one of ours
     * @return whether it is going for somebody rather than drifting
     */
    private static boolean hunting(EnderDragon dragon) {
        return dragon.getPhase() == EnderDragon.Phase.CHARGE_PLAYER
                || dragon.getPhase() == EnderDragon.Phase.STRAFING;
    }

    /**
     * Keeps a dragon out of everything it would do around a portal.
     * <p>
     * With no crystals left to guard, a vanilla dragon eventually lands on its podium and sits there
     * breathing at whoever comes close. On an arena that podium is wherever the dragon is hunting, and a
     * dragon parked on the ground for the rest of the round is not the event anybody bought.
     */
    private static void keepFlying(EnderDragon dragon) {
        switch (dragon.getPhase()) {
            case CIRCLING, STRAFING, CHARGE_PLAYER, DYING -> {
            }
            default -> dragon.setPhase(EnderDragon.Phase.CIRCLING);
        }
    }

    // ------------------------------------------------------------------ carving

    /**
     * Takes the map apart around every dragon.
     * <p>
     * A vanilla dragon already breaks what it flies through, but it leaves the whole {@code DRAGON_IMMUNE}
     * list standing - end stone, obsidian, iron bars - and an arena is built out of exactly that, so the
     * dragons would fly through the map without leaving a mark on it. This is the event doing what it is
     * for: the floor goes, and what is under the floor is the void.
     *
     * @param game the round
     */
    private void carve(Game game) {
        double radius = settings.getDragonCarveRadius();
        if (radius <= 0.0d) return;
        for (EnderDragon dragon : spawned) {
            if (!dragon.isValid()) continue;
            SuddenDeath.carve(game, settings, dragon.getLocation(), radius);
        }
    }

    // ----------------------------------------------------------------- lookups

    /**
     * @param entity something that hit or was hit
     * @return the team its dragon fights for, or {@code null} when it is not one of ours
     */
    public @Nullable GameTeam ownerOf(@Nullable Entity entity) {
        return entity == null ? null : owners.get(entity.getUniqueId());
    }

    /**
     * @return whether any dragon is in the air right now
     */
    public boolean isFlying() {
        return !spawned.isEmpty();
    }

    /**
     * Takes every dragon out of the world again.
     */
    public void remove() {
        for (EnderDragon dragon : spawned) {
            if (dragon.isValid()) dragon.remove();
        }
        for (BossBar bar : bars.values()) Bukkit.getServer().hideBossBar(bar);
        bars.clear();
        spawned.clear();
        owners.clear();
        prey.clear();
        centre = null;
    }
}

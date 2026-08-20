package de.schnorrenbergers.bedwars.game.timeline;

import de.schnorrenbergers.bedwars.config.TimelineSettings;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.shop.upgrade.Upgrade;
import de.schnorrenbergers.bedwars.util.Messages;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
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
 * touch the people who paid for it, and it stays over the middle of the map instead of flying off to where
 * a vanilla dragon expects its portal to be. Both of those are one line of api each, and without either
 * the event is a dragon disappearing into the void while everybody watches.
 */
public final class Dragons {

    /** How often the dragons are checked for having drifted off, in ticks. */
    private static final int CHECK_INTERVAL = 20;

    private final TimelineSettings settings;
    /** Dragon to the team it fights for. */
    private final Map<UUID, GameTeam> owners = new HashMap<>();
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
    }

    /**
     * Puts the dragons that have drifted too far back over the middle, and forgets the ones that died.
     *
     * @param ticks where the loop stands
     */
    public void tick(long ticks) {
        if (spawned.isEmpty() || ticks % CHECK_INTERVAL != 0L || centre == null) return;
        double radius = settings.getDragonRadius();
        spawned.removeIf(dragon -> {
            if (!dragon.isValid()) {
                owners.remove(dragon.getUniqueId());
                return true;
            }
            keepFlying(dragon);
            Location at = dragon.getLocation();
            if (at.getWorld() != null && at.getWorld().equals(centre.getWorld())
                    && at.distanceSquared(centre) <= radius * radius) {
                return false;
            }
            dragon.teleport(centre.clone().add(0.0d, settings.getDragonHeight(), 0.0d));
            dragon.setPhase(EnderDragon.Phase.CIRCLING);
            return false;
        });
    }

    /**
     * Keeps a dragon out of everything it would do around a portal.
     * <p>
     * With no crystals left to guard, a vanilla dragon eventually lands on its podium and sits there
     * breathing at whoever comes close. On an arena that podium is the middle of the map, and a dragon
     * parked on the ground for the rest of the round is not the event anybody bought.
     */
    private static void keepFlying(EnderDragon dragon) {
        switch (dragon.getPhase()) {
            case CIRCLING, STRAFING, CHARGE_PLAYER, DYING -> {
            }
            default -> dragon.setPhase(EnderDragon.Phase.CIRCLING);
        }
    }

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
        spawned.clear();
        owners.clear();
        centre = null;
    }
}

package de.schnorrenbergers.bedwars.game;

import de.schnorrenbergers.bedwars.config.Feature;
import de.schnorrenbergers.bedwars.config.FeatureSettings;
import de.schnorrenbergers.bedwars.map.ArenaMap;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Puts the switches of {@link Feature} onto the world and onto the players.
 * <p>
 * One place rather than a line here and a line there, because every one of these has to be applied three
 * times: when the arena loads, when somebody joins, and the moment an admin flips it in the menu. A switch
 * that is only read on startup is a switch that does nothing when it is used.
 */
public final class Rules {

    /** An attack speed nothing can recharge, which is what 1.8 combat is. */
    private static final double UNCAPPED_ATTACK_SPEED = 1024.0d;
    /** What the attack speed of a player is when the cooldown is left alone. */
    private static final double DEFAULT_ATTACK_SPEED = 4.0d;

    private Rules() {
    }

    /**
     * Sets up the world a round is played in.
     *
     * @param world    the arena
     * @param arena    the map it was loaded from, for the time of day it wants
     * @param features what is switched on
     */
    public static void applyTo(World world, @Nullable ArenaMap arena, FeatureSettings features) {
        boolean daylight = features.is(Feature.DAYLIGHT_CYCLE);
        rule(world, GameRule.DO_DAYLIGHT_CYCLE, daylight);
        if (!daylight) world.setTime(arena == null ? 6000L : arena.getFixedTime());
        rule(world, GameRule.DO_WEATHER_CYCLE, false);
        rule(world, GameRule.DO_FIRE_TICK, false);
        // on: a ghast fireball counts as mob griefing, so with this off a bought fireball explodes
        // without touching a single block. What an explosion is allowed to take is decided properly in
        // BuildListener, which only ever lets it have blocks a player put there this round
        rule(world, GameRule.MOB_GRIEFING, true);
        rule(world, GameRule.DO_MOB_SPAWNING, false);
        rule(world, GameRule.ANNOUNCE_ADVANCEMENTS, false);
        rule(world, GameRule.SHOW_DEATH_MESSAGES, false);
        // the round says who died and when they come back, and the belt and braces for a death that
        // slips past the cancelled hit is that minecraft never shows its screen either
        rule(world, GameRule.DO_IMMEDIATE_RESPAWN, true);
        rule(world, GameRule.NATURAL_REGENERATION, true);
        applyLocatorBar(world, features.is(Feature.LOCATOR_BAR));
        // an iron golem on easy deals no damage worth the name, and on peaceful it would not attack at
        // all. A dream defender costs a hundred and twenty iron, so it has to hit like one
        world.setDifficulty(Difficulty.HARD);
    }

    /**
     * Switches the locator bar on or off.
     * <p>
     * This is the bar over the hotbar that shows which direction every other player is in. It is off here
     * and that is the whole point: half of a bedwars round is not knowing where the other seven teams
     * are, and a bar that gives it away for free takes the rush, the flank and the sneak with it.
     *
     * @param world the arena
     * @param on    whether players see where everybody is
     */
    public static void applyLocatorBar(World world, boolean on) {
        rule(world, GameRule.LOCATOR_BAR, on);
    }

    /**
     * Puts the switches that live on a player onto one of them.
     *
     * @param player   who to set up
     * @param features what is switched on
     */
    public static void applyTo(Player player, FeatureSettings features) {
        AttributeInstance attack = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attack == null) return;
        attack.setBaseValue(features.is(Feature.OLD_PVP) ? UNCAPPED_ATTACK_SPEED : DEFAULT_ATTACK_SPEED);
    }

    /**
     * Puts them onto everybody who is here, for a switch that was flipped mid round.
     *
     * @param features what is switched on
     */
    public static void applyToEverybody(FeatureSettings features) {
        for (Player player : Bukkit.getOnlinePlayers()) applyTo(player, features);
    }

    /**
     * Applies everything again after a switch was flipped.
     *
     * @param game     the round, for the world it is played in
     * @param features what is switched on
     */
    public static void reapply(Game game, FeatureSettings features) {
        if (game.getWorld() != null) applyTo(game.getWorld(), game.getArena(), features);
        applyToEverybody(features);
    }

    private static <T> void rule(World world, GameRule<T> rule, T value) {
        world.setGameRule(rule, value);
    }
}

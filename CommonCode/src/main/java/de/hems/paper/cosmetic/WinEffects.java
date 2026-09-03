package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.CosmeticType;
import de.hems.types.cosmetic.Cosmetics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Which win effects this server can actually play.
 * <p>
 * The catalogue on the launcher says what exists and what it costs; this says what there is code for. The
 * two are matched by id and neither breaks when the other is ahead: an effect that is sold but not
 * implemented here simply does nothing, and one that is implemented but not in the catalogue is never
 * selected.
 */
public final class WinEffects {

    private static final Map<String, WinEffect> effects = new LinkedHashMap<>();

    private WinEffects() {
    }

    /**
     * @param plugin the plugin they run on
     * @deprecated the win effects are no longer the only kind there is - call
     *         {@link CosmeticEffects#init(Plugin)}, which switches all of them on. Kept because a plugin
     *         that only knows about win effects still has to end up with a working server.
     */
    @Deprecated
    public static void init(Plugin plugin) {
        CosmeticEffects.init(plugin);
    }

    /**
     * @param effect an effect this server can play
     */
    public static void register(WinEffect effect) {
        if (effect != null) effects.put(key(effect.getId()), effect);
    }

    /**
     * Plays what a player has on.
     * <p>
     * Everything about this is allowed to be missing - the player owns nothing, the effect is switched off,
     * this server has no code for it - and all of those mean the same thing: the round ends without
     * fireworks, rather than with a stack trace in the middle of it.
     *
     * @param winner  whose effect to play
     * @param context the round that was won
     * @return whether anything was played
     */
    public static boolean playFor(UUID winner, WinContext context) {
        CosmeticData chosen = CosmeticService.getSelected(winner, CosmeticType.WIN_EFFECT);
        if (chosen == null) chosen = fallback(winner);
        if (chosen == null) return false;
        WinEffect effect = effects.get(key(chosen.getId()));
        if (effect == null) return false;
        try {
            effect.play(new WinContext(context.plugin(), context.winners(), context.center(),
                    context.topY(), context.radius(), chosen));
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().warning("The win effect " + chosen.getId() + " failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * What somebody who never chose anything gets: the effect that is free for everybody.
     *
     * @param winner who won
     * @return the effect to play, or {@code null} when there is none
     */
    private static CosmeticData fallback(UUID winner) {
        CosmeticData rockets = CosmeticService.get(Cosmetics.WIN_ROCKETS);
        if (rockets != null && rockets.isEnabled() && CosmeticService.owns(winner, rockets.getId())) {
            return rockets;
        }
        return null;
    }

    /**
     * Plays what the winners have on, each effect only once.
     * <p>
     * Four players on one team with the same effect are one celebration, not four on top of each other -
     * which for something that rains explosions over the whole map is the difference between an effect and
     * a lag spike.
     *
     * @param winners whose effects to play
     * @param context the round that was won
     * @return how many different effects were played
     */
    public static int playForAll(java.util.Collection<UUID> winners, WinContext context) {
        java.util.Set<String> played = new java.util.HashSet<>();
        int count = 0;
        for (UUID winner : winners) {
            CosmeticData chosen = CosmeticService.getSelected(winner, CosmeticType.WIN_EFFECT);
            if (chosen == null) chosen = fallback(winner);
            if (chosen == null || !played.add(key(chosen.getId()))) continue;
            if (playFor(winner, context)) count++;
        }
        return count;
    }

    /**
     * @return the ids this server can play
     */
    public static List<String> registered() {
        return List.copyOf(effects.keySet());
    }

    private static String key(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT);
    }
}

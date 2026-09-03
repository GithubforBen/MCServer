package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.CosmeticType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Which kill effects this server can actually play.
 * <p>
 * The same split as the win effects: the launcher's catalogue says what exists and what it costs, this
 * says what there is code for, and the two are matched by id. Either may be ahead of the other - an effect
 * that is sold but not implemented here does nothing, one that is implemented but not sold is never
 * selected - because a network is never updated everywhere at the same minute.
 */
public final class KillEffects {

    private static final Map<String, KillEffect> effects = new LinkedHashMap<>();

    private KillEffects() {
    }

    /**
     * @param effect an effect this server can play
     */
    public static void register(KillEffect effect) {
        if (effect != null) effects.put(key(effect.getId()), effect);
    }

    /**
     * Plays what a killer has on.
     * <p>
     * Everything about this is allowed to be missing - no killer at all, the effect is switched off, this
     * server has no code for it - and all of those mean the same thing: nothing happens, rather than a
     * stack trace in the middle of a fight.
     *
     * @param plugin the plugin the effect runs on
     * @param killer whoever did it, may be {@code null} for a death nobody caused
     * @param victim whoever it happened to
     * @param where  where they fell
     * @return whether anything was played
     */
    public static boolean playFor(Plugin plugin, Player killer, Player victim, Location where) {
        if (plugin == null || killer == null || victim == null || where == null) return false;
        if (killer.equals(victim)) return false;
        CosmeticData chosen = CosmeticService.getSelected(killer.getUniqueId(), CosmeticType.KILL_EFFECT);
        if (chosen == null) return false;
        KillEffect effect = effects.get(key(chosen.getId()));
        if (effect == null) return false;
        try {
            effect.play(new KillContext(plugin, killer, victim, where, chosen));
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().warning("The kill effect " + chosen.getId() + " failed: " + e.getMessage());
            return false;
        }
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

package de.schnorrenbergers.bedwars.addon;

import de.schnorrenbergers.bedwars.game.Game;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Which addons exist, and which of them are on.
 * <p>
 * Three things may have an opinion, and they are asked in this order:
 * <ol>
 *   <li>the waiting lobby, where an operator switched one for this round,</li>
 *   <li>the event that started this server, which travels in the match settings,</li>
 *   <li>{@code addons.yml} on this server, which is the standard.</li>
 * </ol>
 * The most specific opinion wins, so the file is the rule, an event is the exception, and the menu is the
 * exception to the exception. Anything nobody has an opinion on falls back to what the addon itself says.
 */
public final class AddonRegistry {

    /**
     * Who decided that an addon is on or off, so the menu and {@code /bw addons} can say why.
     */
    public enum Source {
        /** Nobody said anything, the addon's own default applies. */
        DEFAULT("its default"),
        /** {@code addons.yml} on this server. */
        FILE("addons.yml"),
        /** The event that started this round. */
        EVENT("the event"),
        /** Somebody switched it in the waiting lobby. */
        SESSION("this round");

        private final String label;

        Source(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private final Map<String, Addon> addons = new LinkedHashMap<>();
    private final Map<String, Boolean> eventOverrides = new HashMap<>();
    private final Map<String, Boolean> sessionOverrides = new HashMap<>();
    private final Set<String> active = new HashSet<>();

    private final AddonSettings settings;

    public AddonRegistry(AddonSettings settings) {
        this.settings = settings;
    }

    /**
     * Makes an addon known and writes its block into the config.
     *
     * @param addon the addon
     */
    public void register(Addon addon) {
        addons.put(key(addon.getId()), addon);
        settings.define(addon);
    }

    /**
     * @param id the key
     * @return the addon, or {@code null} when there is none by that name
     */
    public Addon get(String id) {
        return id == null ? null : addons.get(key(id));
    }

    public Collection<Addon> all() {
        return addons.values();
    }

    public boolean has(String id) {
        return get(id) != null;
    }

    // ------------------------------------------------------------ opinions

    /**
     * @param id the addon
     * @return whether it should be on
     */
    public boolean isEnabled(String id) {
        Addon addon = get(id);
        if (addon == null) return false;
        Boolean session = sessionOverrides.get(key(id));
        if (session != null) return session;
        Boolean event = eventOverrides.get(key(id));
        if (event != null) return event;
        return settings.isEnabled(addon);
    }

    /**
     * @param id the addon
     * @return who decided its current state
     */
    public Source getSource(String id) {
        Addon addon = get(id);
        if (addon == null) return Source.DEFAULT;
        if (sessionOverrides.containsKey(key(id))) return Source.SESSION;
        if (eventOverrides.containsKey(key(id))) return Source.EVENT;
        if (settings.getFile().contains("addons." + addon.getId() + ".enabled")) return Source.FILE;
        return Source.DEFAULT;
    }

    /**
     * Takes what the event that started this server said. Unknown names are ignored rather than rejected -
     * an event may name an addon this server's version does not have yet.
     *
     * @param overrides addon id to state
     */
    public void applyEventOverrides(Map<String, Boolean> overrides) {
        eventOverrides.clear();
        for (Map.Entry<String, Boolean> entry : overrides.entrySet()) {
            if (has(entry.getKey())) eventOverrides.put(key(entry.getKey()), entry.getValue());
        }
    }

    /**
     * Switches one addon for this round only.
     *
     * @param id      the addon
     * @param enabled whether it should be on
     */
    public void setSessionOverride(String id, boolean enabled) {
        if (!has(id)) return;
        sessionOverrides.put(key(id), enabled);
    }

    /**
     * Takes back what the menu said, so the event and the file decide again.
     *
     * @param id the addon
     */
    public void clearSessionOverride(String id) {
        sessionOverrides.remove(key(id));
    }

    // ------------------------------------------------------------ switching

    /**
     * Brings the running addons in line with what should be on.
     * <p>
     * Called once when the round is set up and again whenever somebody switches an addon in the lobby, so
     * the same method covers both and no addon can end up half on.
     *
     * @param game the round
     */
    public void apply(Game game) {
        for (Addon addon : addons.values()) {
            String key = key(addon.getId());
            boolean shouldRun = isEnabled(addon.getId());
            boolean running = active.contains(key);
            if (shouldRun == running) continue;
            if (shouldRun) {
                addon.enable(game);
                active.add(key);
            } else {
                addon.disable(game);
                active.remove(key);
            }
        }
    }

    /**
     * Lets every addon read its own settings again.
     * <p>
     * Deliberately without switching anything off and on: that would be the simple way to make new
     * numbers stick, and it would also take back everything the addon has already handed out in a round
     * that is being played. An addon that has something registered elsewhere renews it in its own
     * {@link Addon#reload()}.
     */
    public void reloadAll() {
        for (Addon addon : addons.values()) {
            addon.reload();
        }
    }

    /**
     * Switches every addon off, for a plugin that is shutting down.
     *
     * @param game the round
     */
    public void disableAll(Game game) {
        for (Addon addon : addons.values()) {
            if (active.remove(key(addon.getId()))) addon.disable(game);
        }
    }

    /**
     * @param id the addon
     * @return whether it is running right now
     */
    public boolean isActive(String id) {
        return active.contains(key(id));
    }

    public AddonSettings getSettings() {
        return settings;
    }

    private static String key(String id) {
        return id.toLowerCase(Locale.ROOT);
    }
}

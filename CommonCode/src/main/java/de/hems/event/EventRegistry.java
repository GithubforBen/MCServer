package de.hems.event;

import de.hems.event.definitions.BedwarsEventDefinition;
import de.hems.event.definitions.CommunityEventDefinition;
import de.hems.event.definitions.SpeedrunEventDefinition;
import de.hems.event.definitions.TournamentEventDefinition;
import de.hems.types.FileType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Every kind of event that can be put into the calendar.
 * <p>
 * The built in kinds are registered here so that all servers know them and can show the same calendar. A
 * plugin that brings its own game registers its definition in {@code onEnable}:
 * <pre>{@code
 * EventRegistry.register(new MyGameEventDefinition());
 * }</pre>
 * Events whose kind is unknown on a server - because the plugin is not installed there - still show up in
 * the calendar, they just fall back to the data that was stored with them.
 */
public final class EventRegistry {

    private static final Map<String, EventDefinition> DEFINITIONS = new LinkedHashMap<>();
    private static final Map<String, EventDefinition> FALLBACKS = new ConcurrentHashMap<>();

    static {
        register(new BedwarsEventDefinition());
        register(new TournamentEventDefinition());
        register(new SpeedrunEventDefinition());
        register(new CommunityEventDefinition());
    }

    private EventRegistry() {
    }

    /**
     * Makes a kind of event usable in the calendar.
     *
     * @param definition the kind of event to add
     */
    public static synchronized void register(EventDefinition definition) {
        DEFINITIONS.put(definition.getId().toUpperCase(Locale.ROOT), definition);
        FALLBACKS.remove(definition.getId().toUpperCase(Locale.ROOT));
    }

    /**
     * @param id the id of a kind of event
     * @return the definition, or a stand in that carries the id if this server does not know it
     */
    public static EventDefinition get(String id) {
        if (id == null) return DEFINITIONS.get("COMMUNITY");
        String key = id.toUpperCase(Locale.ROOT);
        EventDefinition definition = DEFINITIONS.get(key);
        if (definition != null) return definition;
        return FALLBACKS.computeIfAbsent(key, UnknownEventDefinition::new);
    }

    /**
     * @param id the id of a kind of event
     * @return whether this server knows that kind of event
     */
    public static boolean isKnown(String id) {
        return id != null && DEFINITIONS.containsKey(id.toUpperCase(Locale.ROOT));
    }

    /**
     * @return every kind of event that can be created here
     */
    public static List<EventDefinition> all() {
        return new ArrayList<>(DEFINITIONS.values());
    }

    /**
     * Stands in for an event whose plugin is not installed on this server, so the calendar stays readable
     * everywhere.
     */
    private static final class UnknownEventDefinition extends EventDefinition {

        private final String id;

        private UnknownEventDefinition(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getDisplayName() {
            return id;
        }

        @Override
        public String getDescription() {
            return "Diese Event Art ist auf diesem Server nicht installiert";
        }

        @Override
        public FileType.PLUGIN getPlugin() {
            return null;
        }

        @Override
        public String getIconMaterial() {
            return "BARRIER";
        }
    }
}

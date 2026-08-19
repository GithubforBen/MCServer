package de.hems.types.team;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Everything a team can be configured with.
 * <p>
 * Held as a map rather than as fixed fields so a new setting is one enum constant and needs no change to
 * the storage, the network events or the launcher - the whole point of "everything adjustable" is that
 * adding the next knob stays cheap.
 */
public class TeamSettings implements Serializable {

    private static final long serialVersionUID = 4001L;

    /** One thing that can be set on a team. */
    public enum Key {
        /** How many players may be in the team. Capped by the network wide maximum. */
        MAX_MEMBERS("Maximale Mitglieder", Type.NUMBER, 8),
        /** Whether team members can hurt each other. */
        FRIENDLY_FIRE("Friendly Fire", Type.FLAG, false),
        /** Whether anybody may join without an invite. */
        PUBLIC_JOIN("Offener Beitritt", Type.FLAG, false),
        /** Whether members other than the leader may claim chunks for the team. */
        MEMBERS_MAY_CLAIM("Mitglieder dürfen claimen", Type.FLAG, false),
        /** Whether members other than the leader may invite. */
        MEMBERS_MAY_INVITE("Mitglieder dürfen einladen", Type.FLAG, false),
        /** Whether the shared backpack can be opened at all. */
        BACKPACK_ENABLED("Backpack aktiv", Type.FLAG, true),
        /** Whether members other than the leader may take things out of the backpack. */
        BACKPACK_MEMBERS_MAY_TAKE("Mitglieder dürfen entnehmen", Type.FLAG, true),
        /** Whether the team home may be used by members. */
        HOME_ENABLED("Team-Home aktiv", Type.FLAG, true),
        /** Whether joining and leaving is announced to the team. */
        ANNOUNCE_JOINS("Beitritte ankündigen", Type.FLAG, true);

        /** What kind of value a setting holds, which is what the interface needs to render it. */
        public enum Type {
            FLAG,
            NUMBER
        }

        private final String label;
        private final Type type;
        private final Object fallback;

        Key(String label, Type type, Object fallback) {
            this.label = label;
            this.type = type;
            this.fallback = fallback;
        }

        public String getLabel() {
            return label;
        }

        public Type getType() {
            return type;
        }

        public Object getFallback() {
            return fallback;
        }

        /**
         * @param name the name as it was stored
         * @return the matching key, or {@code null} if it is no longer known
         */
        public static Key of(String name) {
            for (Key key : values()) {
                if (key.name().equalsIgnoreCase(name)) return key;
            }
            return null;
        }
    }

    private final Map<String, Object> values = new LinkedHashMap<>();

    /**
     * @param key the setting to read
     * @return whether it is switched on, falling back to its default
     */
    public boolean getFlag(Key key) {
        Object value = values.get(key.name());
        if (value instanceof Boolean flag) return flag;
        return Boolean.TRUE.equals(key.getFallback());
    }

    /**
     * @param key the setting to read
     * @return its number, falling back to its default
     */
    public int getNumber(Key key) {
        Object value = values.get(key.name());
        if (value instanceof Number number) return number.intValue();
        return key.getFallback() instanceof Number number ? number.intValue() : 0;
    }

    /**
     * @param key   the setting to change
     * @param value what to set it to
     */
    public void set(Key key, Object value) {
        if (value == null) {
            values.remove(key.name());
            return;
        }
        values.put(key.name(), value);
    }

    /**
     * Flips a switch.
     *
     * @param key the setting to toggle
     * @return what it is now
     */
    public boolean toggle(Key key) {
        boolean next = !getFlag(key);
        set(key, next);
        return next;
    }

    /**
     * @return the raw values, as they are written to and read from storage
     */
    public Map<String, Object> asMap() {
        return values;
    }

    /**
     * @param stored the values as they came out of storage
     * @return the settings they describe
     */
    public static TeamSettings fromMap(Map<?, ?> stored) {
        TeamSettings settings = new TeamSettings();
        if (stored == null) return settings;
        for (Map.Entry<?, ?> entry : stored.entrySet()) {
            Key key = Key.of(String.valueOf(entry.getKey()));
            if (key != null) settings.values.put(key.name(), entry.getValue());
        }
        return settings;
    }
}

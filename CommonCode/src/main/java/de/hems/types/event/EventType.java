package de.hems.types.event;

import java.io.Serializable;

/**
 * The kinds of event the network knows.
 * <p>
 * Everything that needs its own rules lives in code and gets an entry here. The two open ones -
 * {@link #SIMPLE} and {@link #OTHER_WORLD} - carry no logic of their own, which is what lets an admin
 * announce something without a developer having to touch the plugin.
 */
public enum EventType implements Serializable {

    /** Just an announcement with a time frame. No mechanics attached. */
    SIMPLE("Einfaches Event", false, false),
    /** Everyone moves to another world for a while. */
    OTHER_WORLD("Andere Welt", false, false),
    /** Opens the End for good. Happens exactly once. */
    END("Das End öffnet", true, false),
    /** Ultra hardcore: kill every boss as fast as possible. */
    UHC_BOSSES("Alle Bosse (UHC)", false, true),
    /** Ultra hardcore: kill the dragon as fast as possible. */
    UHC_DRAGON("Enderdrache (UHC)", false, true),
    /**
     * A round of bedwars at a fixed time. The network puts a bedwars server up when it starts and takes
     * everybody who is in the lobby along; how big the teams are is a setting of the event.
     */
    BEDWARS("Bedwars", false, false);

    private final String title;
    private final boolean onlyOnce;
    private final boolean timed;

    EventType(String title, boolean onlyOnce, boolean timed) {
        this.title = title;
        this.onlyOnce = onlyOnce;
        this.timed = timed;
    }

    public String getTitle() {
        return title;
    }

    /**
     * @return whether the network may only ever hold one of these
     */
    public boolean isOnlyOnce() {
        return onlyOnce;
    }

    /**
     * @return whether runs of this event are raced against a clock, which is what needs a queue, a
     *         leaderboard and its own server
     */
    public boolean isTimed() {
        return timed;
    }

    /**
     * Whether the type carries mechanics that live in code. Scheduling is open to admins either way - what
     * this says is whether anything happens beyond the announcement.
     *
     * @return whether code reacts to this type
     */
    public boolean hasMechanics() {
        return this != SIMPLE;
    }

    /**
     * @param name the name as it arrived from a command or the website
     * @return the type, or {@code null} if there is none by that name
     */
    public static EventType byName(String name) {
        if (name == null) return null;
        for (EventType type : values()) {
            if (type.name().equalsIgnoreCase(name)) return type;
        }
        return null;
    }
}

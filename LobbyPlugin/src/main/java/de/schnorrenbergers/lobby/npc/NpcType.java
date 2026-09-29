package de.schnorrenbergers.lobby.npc;

import java.util.Locale;

/**
 * What a lobby npc does when somebody right-clicks it.
 */
public enum NpcType {

    /** Sends the player to a server, and shows over its head whether that server is up and who is on it. */
    WARP,
    /** Opens the event calendar, and shows over its head what is running or coming next. */
    EVENTS;

    /**
     * @param text what an admin typed
     * @return the type, or {@code null} when there is none by that name
     */
    public static NpcType parse(String text) {
        return switch (text.toLowerCase(Locale.ROOT)) {
            case "warp", "server" -> WARP;
            case "events", "event", "kalender" -> EVENTS;
            default -> null;
        };
    }
}

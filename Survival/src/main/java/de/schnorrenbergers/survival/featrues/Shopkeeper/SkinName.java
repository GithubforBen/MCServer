package de.schnorrenbergers.survival.featrues.Shopkeeper;

import java.util.regex.Pattern;

/**
 * What a typed answer for a shop's skin means. Pure, so it can be checked without a server.
 * <p>
 * The name format is the one {@code /npc skin} of the lobby accepts, and {@code aus} removes the skin there
 * too, in any case.
 */
public final class SkinName {

    private static final Pattern PLAYER_NAME = Pattern.compile("[A-Za-z0-9_]{3,16}");

    /** What the answer asks for. */
    public enum Kind {
        /** a valid Minecraft player name; {@link Result#name()} holds it */
        NAME,
        /** {@code aus}: the shop becomes a villager again */
        REMOVE,
        /** {@code abbrechen}: nothing changes */
        CANCEL,
        /** anything else: nothing changes and the member is told */
        INVALID
    }

    /**
     * @param kind what was asked for
     * @param name the player name for {@link Kind#NAME}, else {@code null}
     */
    public record Result(Kind kind, String name) {
    }

    private SkinName() {
    }

    /**
     * @param input the typed text, may be {@code null}
     * @return what it means
     */
    public static Result parse(String input) {
        String text = input == null ? "" : input.trim();
        if (text.equalsIgnoreCase("abbrechen") || text.equalsIgnoreCase("cancel")) {
            return new Result(Kind.CANCEL, null);
        }
        if (text.equalsIgnoreCase("aus") || text.equalsIgnoreCase("none")) {
            return new Result(Kind.REMOVE, null);
        }
        if (PLAYER_NAME.matcher(text).matches()) return new Result(Kind.NAME, text);
        return new Result(Kind.INVALID, null);
    }
}

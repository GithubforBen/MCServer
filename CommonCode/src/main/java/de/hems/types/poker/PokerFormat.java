package de.hems.types.poker;

import java.io.Serializable;

/**
 * How a poker night is played out.
 * <p>
 * The two formats are not variations of the same evening, they are two different evenings. A cash game is
 * a room somebody walks into and out of again; a tournament is one thing everybody starts together and
 * only one of them finishes. That difference decides whether a seat can be left with the money still on
 * it, whether the blinds stay where they are, and what the ranking at the end can even mean.
 */
public enum PokerFormat implements Serializable {

    /**
     * Fixed blinds, sit down and stand up whenever you like, buy more chips when you run out.
     * <p>
     * The chips on the table are bits the whole time - standing up hands them back. This is the format the
     * earnings ranking was built for, because "what went in" and "what came out" are both real numbers.
     */
    CASH("Cash Game", "Feste Blinds, jederzeit rein und raus, Chips sind Bits"),

    /**
     * One buy-in each, rising blinds, out when the chips are gone, one winner at the end.
     * <p>
     * Nobody cashes out in the middle: the chips only turn back into bits at the end, and only for the
     * places that are paid. So a stack here is not money yet, which is why it does not count towards the
     * ranking until the tournament is over.
     */
    TOURNAMENT("Turnier", "Ein Buy-in, steigende Blinds, bis einer übrig ist");

    private final String title;
    private final String description;

    PokerFormat(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    /**
     * @return whether a player may turn their chips back into bits while the evening is still running
     */
    public boolean allowsCashOut() {
        return this == CASH;
    }

    /**
     * @param name the name as it arrived from a setting or a command
     * @param fallback what to answer if there is none by that name
     * @return the format
     */
    public static PokerFormat byName(String name, PokerFormat fallback) {
        if (name == null) return fallback;
        for (PokerFormat format : values()) {
            if (format.name().equalsIgnoreCase(name.trim())) return format;
        }
        return fallback;
    }

    /**
     * @return the format after this one, so a button can cycle through them
     */
    public PokerFormat next() {
        return values()[(ordinal() + 1) % values().length];
    }
}

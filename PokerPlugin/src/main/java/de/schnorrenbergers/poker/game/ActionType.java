package de.schnorrenbergers.poker.game;

/**
 * What a player can do when it is their turn.
 * <p>
 * All-in is not a separate decision in the rules - it is a call or a raise that happens to use every chip
 * somebody has - but it is a separate button, because "call 400" when you only have 250 has to mean
 * something and pretending otherwise is how people lose stacks by accident.
 */
public enum ActionType {

    FOLD("Passen"),
    CHECK("Schieben"),
    CALL("Mitgehen"),
    BET("Setzen"),
    RAISE("Erhöhen"),
    ALL_IN("All-In");

    private final String german;

    ActionType(String german) {
        this.german = german;
    }

    public String getGerman() {
        return german;
    }

    /**
     * @return whether this is money going in on top of what is already there, which is what reopens the
     *         betting for everybody else
     */
    public boolean isAggressive() {
        return this == BET || this == RAISE || this == ALL_IN;
    }
}

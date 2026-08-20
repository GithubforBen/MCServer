package de.schnorrenbergers.bedwars.game.phase;

/**
 * The three states a round is ever in.
 * <p>
 * Listeners ask the game which of these it is in, instead of every one of them keeping its own boolean.
 */
public enum PhaseType {

    /** Everybody is waiting in the lobby, teams can still be picked. */
    LOBBY("phase.lobby"),
    /** The round is being played. */
    RUNNING("phase.running"),
    /** It is decided, the winners are celebrating. */
    ENDING("phase.ending");

    private final String messageKey;

    PhaseType(String messageKey) {
        this.messageKey = messageKey;
    }

    /**
     * @return the key in {@code messages.yml} that holds the name players are shown
     */
    public String getMessageKey() {
        return messageKey;
    }
}

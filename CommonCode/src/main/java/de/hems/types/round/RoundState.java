package de.hems.types.round;

/**
 * Where a round is in its life.
 */
public enum RoundState {

    /** The server has been ordered and is coming up. */
    PREPARING("wird vorbereitet"),
    /** The round is up and taking players. */
    WAITING("wartet auf Spieler"),
    /** It is being played. */
    RUNNING("läuft"),
    /** It is over; the entry is kept only until it is cleaned up. */
    ENDED("beendet");

    private final String description;

    RoundState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * @return whether players can still be sent there
     */
    public boolean isOpen() {
        return this == PREPARING || this == WAITING;
    }

    /**
     * @return whether the round still occupies a server
     */
    public boolean isAlive() {
        return this != ENDED;
    }
}

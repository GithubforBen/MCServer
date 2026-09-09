package de.schnorrenbergers.poker.game;

/**
 * The rules one table is dealt under.
 * <p>
 * Mutable in exactly one place - the blinds - because a tournament raises them while it runs. Everything
 * else about a table is decided when it is built.
 */
public final class TableRules {

    private int smallBlind;
    private final int actionSeconds;
    private final boolean tournament;
    private final RakePolicy rake;

    /**
     * @param smallBlind    the small blind in chips; the big blind is twice it
     * @param actionSeconds how long somebody has to make up their mind
     * @param tournament    whether chips can not be turned back into bits while the table runs
     * @param rake          what the house keeps
     */
    public TableRules(int smallBlind, int actionSeconds, boolean tournament, RakePolicy rake) {
        this.smallBlind = Math.max(1, smallBlind);
        this.actionSeconds = Math.max(5, actionSeconds);
        this.tournament = tournament;
        this.rake = rake == null ? RakePolicy.NONE : rake;
    }

    public int getSmallBlind() {
        return smallBlind;
    }

    public int getBigBlind() {
        return smallBlind * 2;
    }

    /**
     * Raises the blinds, which only a tournament does.
     *
     * @param smallBlind the new small blind
     */
    public void setSmallBlind(int smallBlind) {
        this.smallBlind = Math.max(1, smallBlind);
    }

    public int getActionSeconds() {
        return actionSeconds;
    }

    public boolean isTournament() {
        return tournament;
    }

    public RakePolicy getRake() {
        return rake;
    }
}

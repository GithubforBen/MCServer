package de.hems.communication.events.money;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;

/**
 * Asks the launcher to move money on one account.
 * <p>
 * Deliberately a delta and not a new total: the survival server, the lobby and the website all touch the
 * same accounts, and a total computed from a copy that is a second old silently eats whatever happened in
 * that second.
 */
public class ChangeBalanceEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4504L;

    private String holder;
    private int delta;
    private boolean requireCover;
    private String reason;

    public ChangeBalanceEvent() {
    }

    /**
     * @param holder       whose account, a player uuid as text or a team name
     * @param delta        how much to add, negative to take away
     * @param requireCover whether taking away must fail rather than go below zero
     * @param reason       what the money was for, for the log
     */
    public ChangeBalanceEvent(String holder, int delta, boolean requireCover, String reason) {
        super(ListenerAdapter.ServerName.HOST);
        this.holder = holder;
        this.delta = delta;
        this.requireCover = requireCover;
        this.reason = reason;
    }

    public String getHolder() {
        return holder;
    }

    public int getDelta() {
        return delta;
    }

    public boolean isRequireCover() {
        return requireCover;
    }

    public String getReason() {
        return reason;
    }
}

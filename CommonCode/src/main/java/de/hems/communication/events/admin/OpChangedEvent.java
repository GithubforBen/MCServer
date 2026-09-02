package de.hems.communication.events.admin;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.UUID;

/**
 * Announces that somebody was made an operator, or stopped being one.
 * <p>
 * The launcher writes the list that every new server is built with, but a server that is already running
 * was built from the old list and would keep it until it restarts. So the change is announced as well, and
 * every server applies it to itself right away - which is also what writes it into that server's own
 * {@code ops.json}, so it survives the restart it did not need.
 */
public class OpChangedEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4907L;

    private UUID playerId;
    private String playerName;
    private boolean operator;

    public OpChangedEvent() {
    }

    public OpChangedEvent(UUID playerId, String playerName, boolean operator) {
        super(ListenerAdapter.ServerName.ALL);
        this.playerId = playerId;
        this.playerName = playerName;
        this.operator = operator;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public boolean isOperator() {
        return operator;
    }
}

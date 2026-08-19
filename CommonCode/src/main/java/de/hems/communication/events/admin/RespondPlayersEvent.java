package de.hems.communication.events.admin;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.types.admin.PlayerSnapshot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

/**
 * The players of one server, the answer to a {@link RequestPlayersEvent}.
 */
public class RespondPlayersEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 3102L;

    /** How well the answering server is keeping up, so the overview does not need a second request. */
    private double tps;

    public RespondPlayersEvent(ListenerAdapter.ServerName receiver, ArrayList<PlayerSnapshot> players,
                               double tps, UUID requestId) {
        super(receiver, players, requestId);
        this.tps = tps;
    }

    public RespondPlayersEvent() {
    }

    /**
     * @return the ticks per second of the answering server
     */
    public double getTps() {
        return tps;
    }
}

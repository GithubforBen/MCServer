package de.hems.communication.events.round;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.UUID;

/** Removes a round from the list. The server it ran on is stopped separately. */
public class DeleteRoundEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4715L;

    private UUID roundId;

    public DeleteRoundEvent() {
    }

    public DeleteRoundEvent(UUID roundId) {
        super(ListenerAdapter.ServerName.HOST);
        this.roundId = roundId;
    }

    public UUID getRoundId() {
        return roundId;
    }
}

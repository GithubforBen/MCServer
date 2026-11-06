package com.benschnorr.communication.events.round;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;

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

package com.benschnorr.communication.events.round;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;
import com.benschnorr.types.round.RoundData;

import java.io.Serializable;
import java.util.UUID;

/** Announces that a round changed, so every lobby's list follows without polling. */
public class RoundUpdatedEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4716L;

    private UUID roundId;
    /** The new state, or {@code null} when the round was removed. */
    private RoundData round;

    public RoundUpdatedEvent() {
    }

    public RoundUpdatedEvent(UUID roundId, RoundData round) {
        super(ListenerAdapter.ServerName.ALL);
        this.roundId = roundId;
        this.round = round;
    }

    public UUID getRoundId() {
        return roundId;
    }

    public RoundData getRound() {
        return round;
    }

    public boolean isDeleted() {
        return round == null;
    }
}

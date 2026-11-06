package com.benschnorr.communication.events.round;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.RespondDataEvent;
import com.benschnorr.types.round.RoundSnapshot;

import java.io.Serializable;
import java.util.UUID;

/** The rounds and the rules, as the launcher has them. */
public class RespondRoundsEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4712L;

    public RespondRoundsEvent(ListenerAdapter.ServerName receiver, RoundSnapshot snapshot, UUID requestId) {
        super(receiver, snapshot, requestId);
    }

    public RespondRoundsEvent() {
    }
}

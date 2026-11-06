package com.benschnorr.communication.events.money;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.UUID;

public class RequestPlayerMoneyEvent extends EventFoundationData implements Event, Serializable {
    private static final long serialVersionUID = 3L;
    private UUID playerId;

    public RequestPlayerMoneyEvent() {
    }

    public RequestPlayerMoneyEvent(ListenerAdapter.ServerName receiver, UUID playerId) {
        super(receiver);
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }
}

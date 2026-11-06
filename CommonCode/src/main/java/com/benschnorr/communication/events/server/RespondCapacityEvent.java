package com.benschnorr.communication.events.server;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.RespondDataEvent;
import com.benschnorr.types.server.CapacityData;

import java.io.Serializable;
import java.util.UUID;

/** What the launcher has left, and what it suggests doing about it. */
public class RespondCapacityEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4604L;

    public RespondCapacityEvent(ListenerAdapter.ServerName receiver, CapacityData capacity, UUID requestId) {
        super(receiver, capacity, requestId);
    }

    public RespondCapacityEvent() {
    }
}

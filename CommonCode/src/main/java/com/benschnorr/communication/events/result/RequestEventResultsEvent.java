package com.benschnorr.communication.events.result;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.UUID;

/** Asks the launcher for the result lines of one event. */
public class RequestEventResultsEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4345L;

    private UUID eventUuid;

    public RequestEventResultsEvent(UUID eventUuid) {
        super(ListenerAdapter.ServerName.HOST);
        this.eventUuid = eventUuid;
    }

    public RequestEventResultsEvent() {
    }

    public UUID getEventUuid() {
        return eventUuid;
    }
}

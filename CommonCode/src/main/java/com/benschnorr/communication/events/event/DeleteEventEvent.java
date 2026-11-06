package com.benschnorr.communication.events.event;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.UUID;

/** Removes an event on the launcher. */
public class DeleteEventEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4305L;

    private UUID eventUuid;

    public DeleteEventEvent(UUID eventUuid) {
        super(ListenerAdapter.ServerName.HOST);
        this.eventUuid = eventUuid;
    }

    public DeleteEventEvent() {
    }

    public UUID getEventUuid() {
        return eventUuid;
    }
}

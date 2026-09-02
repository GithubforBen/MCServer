package de.hems.communication.events.server;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.types.server.CapacityData;

import java.io.Serializable;
import java.util.UUID;

/** Whether a server of the asked size may be started, and what the machine looks like either way. */
public class RespondServerSlotEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4606L;

    private boolean granted;

    public RespondServerSlotEvent(ListenerAdapter.ServerName receiver, boolean granted, CapacityData capacity,
                                  UUID requestId) {
        super(receiver, capacity, requestId);
        this.granted = granted;
    }

    public RespondServerSlotEvent() {
    }

    public boolean isGranted() {
        return granted;
    }
}

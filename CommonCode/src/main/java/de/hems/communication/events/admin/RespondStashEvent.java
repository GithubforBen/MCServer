package de.hems.communication.events.admin;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.types.admin.StashData;

import java.io.Serializable;
import java.util.UUID;

/** The contents of the admin stash. */
public class RespondStashEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 5003L;

    public RespondStashEvent(ListenerAdapter.ServerName receiver, StashData stash, UUID requestId) {
        super(receiver, stash, requestId);
    }

    public RespondStashEvent() {
    }
}

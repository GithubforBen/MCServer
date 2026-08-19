package de.hems.communication.events.admin;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;

/** Fetches the admin stash from the launcher. */
public class RequestStashEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 5002L;

    private String stashId;

    public RequestStashEvent(String stashId) {
        super(ListenerAdapter.ServerName.HOST);
        this.stashId = stashId;
    }

    public RequestStashEvent() {
    }

    public String getStashId() {
        return stashId;
    }
}

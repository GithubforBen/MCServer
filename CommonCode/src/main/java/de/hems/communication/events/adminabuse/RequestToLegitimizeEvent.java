package de.hems.communication.events.adminabuse;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;

public class RequestToLegitimizeEvent  extends EventFoundationData implements Event, Serializable {
    private static final long serialVersionUID = 509098650L;

    public RequestToLegitimizeEvent() {
    }

    public RequestToLegitimizeEvent(ListenerAdapter.ServerName receiver) {
        super(receiver);
    }
}

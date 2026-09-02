package de.hems.communication.events.discord;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;

/** Asks the launcher which minecraft accounts belong to which discord accounts. */
public class RequestAccountLinksEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4902L;

    public RequestAccountLinksEvent() {
        super(ListenerAdapter.ServerName.HOST);
    }
}

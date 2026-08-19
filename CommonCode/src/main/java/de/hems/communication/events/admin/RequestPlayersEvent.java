package de.hems.communication.events.admin;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;

/**
 * Asks every server who is on it. Broadcast, so the answers together make up the player list of the whole
 * network without the launcher having to know which servers exist.
 */
public class RequestPlayersEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 3101L;

    public RequestPlayersEvent() {
        super(ListenerAdapter.ServerName.ALL);
    }
}

package com.benschnorr.communication.events.admin;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;

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

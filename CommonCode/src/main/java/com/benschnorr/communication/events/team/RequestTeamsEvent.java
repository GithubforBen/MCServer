package com.benschnorr.communication.events.team;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;

import java.io.Serializable;

/** Asks the launcher for every team of the network. */
public class RequestTeamsEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4101L;

    public RequestTeamsEvent() {
        super(ListenerAdapter.ServerName.HOST);
    }
}

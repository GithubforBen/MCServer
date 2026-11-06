package com.benschnorr.communication.events.event;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;

import java.io.Serializable;

/** Asks the launcher for every event of the network. */
public class RequestEventsEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4301L;

    public RequestEventsEvent() {
        super(ListenerAdapter.ServerName.HOST);
    }
}

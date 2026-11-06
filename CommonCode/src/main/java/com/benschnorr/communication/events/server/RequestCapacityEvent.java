package com.benschnorr.communication.events.server;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;

import java.io.Serializable;

/** Asks the launcher how much room the machine still has. */
public class RequestCapacityEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4603L;

    public RequestCapacityEvent() {
        super(ListenerAdapter.ServerName.HOST);
    }
}

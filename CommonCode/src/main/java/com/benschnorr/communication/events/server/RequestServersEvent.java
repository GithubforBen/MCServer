package com.benschnorr.communication.events.server;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;

import java.io.Serializable;

public class RequestServersEvent extends EventFoundationData implements Event, Serializable {
    private static final long serialVersionUID = 101L;
    public RequestServersEvent() {
    }

    public RequestServersEvent(ListenerAdapter.ServerName receiver) {
        super(receiver);
    }

}

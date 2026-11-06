package com.benschnorr.communication.events.event;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;

import java.io.Serializable;

/** Asks the launcher for every run of every event. */
public class RequestRunsEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4311L;

    public RequestRunsEvent() {
        super(ListenerAdapter.ServerName.HOST);
    }
}

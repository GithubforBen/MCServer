package com.benschnorr.communication.events.restart;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;

import java.io.Serializable;

/** Asks the launcher whether a restart is scheduled. Answered with {@link RespondRestartEvent}. */
public class RequestRestartStatusEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4402L;

    public RequestRestartStatusEvent() {
        super(ListenerAdapter.ServerName.HOST);
    }
}

package com.benschnorr.communication.events.round;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;

import java.io.Serializable;

/** Asks the launcher for every self started round and for the rules they are started under. */
public class RequestRoundsEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4711L;

    public RequestRoundsEvent() {
        super(ListenerAdapter.ServerName.HOST);
    }
}

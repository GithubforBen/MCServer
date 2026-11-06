package com.benschnorr.communication.events.money;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;

import java.io.Serializable;

/** Asks the launcher for every balance it keeps. */
public class RequestBalancesEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4502L;

    public RequestBalancesEvent() {
        super(ListenerAdapter.ServerName.HOST);
    }
}

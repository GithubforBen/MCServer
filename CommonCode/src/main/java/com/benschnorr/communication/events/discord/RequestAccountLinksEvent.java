package com.benschnorr.communication.events.discord;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;

import java.io.Serializable;

/** Asks the launcher which minecraft accounts belong to which discord accounts. */
public class RequestAccountLinksEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4902L;

    public RequestAccountLinksEvent() {
        super(ListenerAdapter.ServerName.HOST);
    }
}

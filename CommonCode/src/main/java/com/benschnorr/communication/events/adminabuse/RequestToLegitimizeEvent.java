package com.benschnorr.communication.events.adminabuse;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;

import java.io.Serializable;

public class RequestToLegitimizeEvent  extends EventFoundationData implements Event, Serializable {
    private static final long serialVersionUID = 509098650L;

    public RequestToLegitimizeEvent() {
    }

    public RequestToLegitimizeEvent(ListenerAdapter.ServerName receiver) {
        super(receiver);
    }
}

package com.benschnorr.communication.events.admin;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;

import java.io.Serializable;

/** Fetches the admin stash from the launcher. */
public class RequestStashEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 5002L;

    private String stashId;

    public RequestStashEvent(String stashId) {
        super(ListenerAdapter.ServerName.HOST);
        this.stashId = stashId;
    }

    public RequestStashEvent() {
    }

    public String getStashId() {
        return stashId;
    }
}

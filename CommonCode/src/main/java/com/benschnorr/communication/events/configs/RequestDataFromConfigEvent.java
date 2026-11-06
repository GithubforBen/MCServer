package com.benschnorr.communication.events.configs;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.UUID;

public class RequestDataFromConfigEvent extends EventFoundationData implements Event, Serializable {
    private static final long serialVersionUID = 1L;
    private String key;

    public RequestDataFromConfigEvent(String key) {
        super(ListenerAdapter.ServerName.HOST);
        this.key = key;
    }

    public RequestDataFromConfigEvent() {
    }

    public String getKey() {
        return key;
    }
}

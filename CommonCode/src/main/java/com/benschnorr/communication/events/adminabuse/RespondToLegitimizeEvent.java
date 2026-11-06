package com.benschnorr.communication.events.adminabuse;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;
import com.benschnorr.communication.events.types.RespondDataEvent;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RespondToLegitimizeEvent extends RespondDataEvent implements Event, Serializable {
    private static final long serialVersionUID = 50965789080L;
    private Map<UUID, String> toLegitimize;

    public RespondToLegitimizeEvent() {
    }

    public RespondToLegitimizeEvent(ListenerAdapter.ServerName receiver, UUID requestId, Map<UUID, String> toLegitimize) {
        super(receiver, toLegitimize, requestId);
        this.toLegitimize = toLegitimize;
    }

    public Map<UUID, String> getToLegitimize() {
        if (toLegitimize == null) toLegitimize = (Map<UUID, String>) super.getData();
        return toLegitimize;
    }

    public void setToLegitimize(Map<UUID, String> toLegitimize) {
        this.toLegitimize = toLegitimize;
    }
}

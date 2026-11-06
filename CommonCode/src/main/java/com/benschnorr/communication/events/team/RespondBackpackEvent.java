package com.benschnorr.communication.events.team;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.RespondDataEvent;
import com.benschnorr.types.team.BackpackData;

import java.io.Serializable;
import java.util.UUID;

/** The contents of a team backpack. */
public class RespondBackpackEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4108L;

    public RespondBackpackEvent(ListenerAdapter.ServerName receiver, BackpackData backpack, UUID requestId) {
        super(receiver, backpack, requestId);
    }

    public RespondBackpackEvent() {
    }
}

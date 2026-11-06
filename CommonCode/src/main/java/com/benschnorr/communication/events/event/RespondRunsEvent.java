package com.benschnorr.communication.events.event;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.RespondDataEvent;
import com.benschnorr.types.event.RunData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

/** Every run the launcher knows. */
public class RespondRunsEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4312L;

    public RespondRunsEvent(ListenerAdapter.ServerName receiver, ArrayList<RunData> runs, UUID requestId) {
        super(receiver, runs, requestId);
    }

    public RespondRunsEvent() {
    }
}

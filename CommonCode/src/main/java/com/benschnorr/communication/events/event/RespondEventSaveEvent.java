package com.benschnorr.communication.events.event;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.RespondDataEvent;
import com.benschnorr.types.event.EventData;

import java.io.Serializable;
import java.util.UUID;

/** How a write ended. The data is the event as it is stored now, so the caller picks up the new revision. */
public class RespondEventSaveEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4304L;

    private boolean successful;
    private String message;

    public RespondEventSaveEvent(ListenerAdapter.ServerName receiver, boolean successful, String message,
                                 EventData event, UUID requestId) {
        super(receiver, event, requestId);
        this.successful = successful;
        this.message = message;
    }

    public RespondEventSaveEvent() {
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getMessage() {
        return message;
    }
}

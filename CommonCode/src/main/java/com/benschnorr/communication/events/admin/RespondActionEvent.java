package com.benschnorr.communication.events.admin;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.RespondDataEvent;

import java.io.Serializable;
import java.util.UUID;

/**
 * How an action or a write back ended. The data is the message shown in the browser, or {@code null} from a
 * server that was not concerned.
 */
public class RespondActionEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 3107L;

    private boolean successful;

    public RespondActionEvent(ListenerAdapter.ServerName receiver, boolean successful, String message,
                              UUID requestId) {
        super(receiver, message, requestId);
        this.successful = successful;
    }

    public RespondActionEvent() {
    }

    public boolean isSuccessful() {
        return successful;
    }
}

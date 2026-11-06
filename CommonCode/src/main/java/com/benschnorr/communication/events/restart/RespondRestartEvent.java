package com.benschnorr.communication.events.restart;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.RespondDataEvent;
import com.benschnorr.types.restart.RestartStatus;

import java.io.Serializable;
import java.util.UUID;

/** The restart as the launcher has it now, with what went wrong if a request was refused. */
public class RespondRestartEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4403L;

    private String error;

    public RespondRestartEvent(ListenerAdapter.ServerName receiver, RestartStatus status, String error,
                               UUID requestId) {
        super(receiver, status, requestId);
        this.error = error;
    }

    public RespondRestartEvent() {
    }

    /**
     * @return why a request was refused, or {@code null} when it went through
     */
    public String getError() {
        return error;
    }
}

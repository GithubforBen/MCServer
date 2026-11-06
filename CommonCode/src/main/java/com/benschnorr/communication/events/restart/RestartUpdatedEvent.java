package com.benschnorr.communication.events.restart;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;
import com.benschnorr.types.restart.RestartStatus;

import java.io.Serializable;

/**
 * Tells every server that a restart was scheduled, moved or called off - or, with {@code now} set, that it
 * is happening: players are sent off and the servers are about to be stopped.
 */
public class RestartUpdatedEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4404L;

    private RestartStatus status;
    private boolean now;

    public RestartUpdatedEvent(RestartStatus status, boolean now) {
        super(ListenerAdapter.ServerName.ALL);
        this.status = status;
        this.now = now;
    }

    public RestartUpdatedEvent() {
    }

    public RestartStatus getStatus() {
        return status;
    }

    /**
     * @return whether the restart is happening right now
     */
    public boolean isNow() {
        return now;
    }
}

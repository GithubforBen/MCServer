package com.benschnorr.communication.events.restart;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;
import com.benschnorr.types.restart.RestartMode;

import java.io.Serializable;

/**
 * Asks the launcher to schedule a restart of the network, replacing one that is already scheduled - or to
 * call it off, when {@code mode} is {@code null}. Answered with {@link RespondRestartEvent}.
 */
public class ScheduleRestartEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4401L;

    private int minutes;
    private RestartMode mode;
    private String requestedBy;

    /**
     * @param minutes     how long until it happens
     * @param mode        what happens, or {@code null} to call a scheduled restart off
     * @param requestedBy who asked, for the announcement and the log
     */
    public ScheduleRestartEvent(int minutes, RestartMode mode, String requestedBy) {
        super(ListenerAdapter.ServerName.HOST);
        this.minutes = minutes;
        this.mode = mode;
        this.requestedBy = requestedBy;
    }

    public ScheduleRestartEvent() {
    }

    public int getMinutes() {
        return minutes;
    }

    public RestartMode getMode() {
        return mode;
    }

    public String getRequestedBy() {
        return requestedBy;
    }
}

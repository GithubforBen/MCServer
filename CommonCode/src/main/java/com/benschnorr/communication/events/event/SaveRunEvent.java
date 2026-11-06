package com.benschnorr.communication.events.event;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;
import com.benschnorr.types.event.RunData;

import java.io.Serializable;

/**
 * Writes a run on the launcher.
 * <p>
 * Runs are written often - every boss kill is one - so this is fire and forget: the launcher stores it and
 * announces it, and nobody waits for an answer.
 */
public class SaveRunEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4313L;

    private RunData run;

    public SaveRunEvent(RunData run) {
        super(ListenerAdapter.ServerName.HOST);
        this.run = run;
    }

    public SaveRunEvent() {
    }

    public RunData getRun() {
        return run;
    }
}

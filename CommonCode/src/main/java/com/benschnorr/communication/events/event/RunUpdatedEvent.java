package com.benschnorr.communication.events.event;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;
import com.benschnorr.types.event.RunData;

import java.io.Serializable;
import java.util.UUID;

/**
 * Announces that a run changed, so the leaderboard on every server and on the website follows along while
 * a race is still going.
 */
public class RunUpdatedEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4314L;

    private UUID runId;
    /** The new state, or {@code null} when the run was removed. */
    private RunData run;

    public RunUpdatedEvent(UUID runId, RunData run) {
        super(ListenerAdapter.ServerName.ALL);
        this.runId = runId;
        this.run = run;
    }

    public RunUpdatedEvent() {
    }

    public UUID getRunId() {
        return runId;
    }

    public RunData getRun() {
        return run;
    }

    public boolean isDeleted() {
        return run == null;
    }
}

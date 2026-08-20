package de.hems.communication.events.event;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.types.event.RunData;

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

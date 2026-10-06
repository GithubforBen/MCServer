package de.hems.communication.events.event;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.types.event.RunData;

import java.io.Serializable;

/**
 * Asks the launcher for the ghost server of an event, the run server that is already standing ready.
 * <p>
 * The launcher answers with {@link RespondClaimGhostRunEvent}. Two teams can reset in the same second, so
 * the decision is made there and nowhere else: only the first one gets the ghost, the other one is told no
 * and builds a server of its own. A yes also writes the server into the run on the launcher, so a ghost
 * whose answer gets lost on the way still belongs to the run that claimed it.
 */
public class ClaimGhostRunEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4347L;

    private RunData run;

    /**
     * @param run the new run, not stored yet, that wants the ghost of its event
     */
    public ClaimGhostRunEvent(RunData run) {
        super(ListenerAdapter.ServerName.HOST);
        this.run = run;
    }

    public ClaimGhostRunEvent() {
    }

    public RunData getRun() {
        return run;
    }
}

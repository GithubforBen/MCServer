package de.hems.communication.events.round;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.types.round.RoundPolicy;

import java.io.Serializable;

/** Changes the rules self started rounds run under. Only an operator ever sends this. */
public class SaveRoundPolicyEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4717L;

    private RoundPolicy policy;

    public SaveRoundPolicyEvent() {
    }

    public SaveRoundPolicyEvent(RoundPolicy policy) {
        super(ListenerAdapter.ServerName.HOST);
        this.policy = policy;
    }

    public RoundPolicy getPolicy() {
        return policy;
    }
}

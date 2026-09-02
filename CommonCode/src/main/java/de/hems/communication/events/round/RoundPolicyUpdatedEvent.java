package de.hems.communication.events.round;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.types.round.RoundPolicy;

import java.io.Serializable;

/** Announces the new rules, so every server stops allowing what was just switched off. */
public class RoundPolicyUpdatedEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4718L;

    private RoundPolicy policy;

    public RoundPolicyUpdatedEvent() {
    }

    public RoundPolicyUpdatedEvent(RoundPolicy policy) {
        super(ListenerAdapter.ServerName.ALL);
        this.policy = policy;
    }

    public RoundPolicy getPolicy() {
        return policy;
    }
}

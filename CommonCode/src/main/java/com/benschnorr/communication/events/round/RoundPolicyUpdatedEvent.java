package com.benschnorr.communication.events.round;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;
import com.benschnorr.types.round.RoundPolicy;

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

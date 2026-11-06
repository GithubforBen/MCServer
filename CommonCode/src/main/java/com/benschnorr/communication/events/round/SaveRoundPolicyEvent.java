package com.benschnorr.communication.events.round;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;
import com.benschnorr.types.round.RoundPolicy;

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

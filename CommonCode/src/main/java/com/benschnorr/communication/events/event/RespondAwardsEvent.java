package com.benschnorr.communication.events.event;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.RespondDataEvent;
import com.benschnorr.types.event.AwardData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

/** Everything a player still has to collect. */
public class RespondAwardsEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4323L;

    public RespondAwardsEvent(ListenerAdapter.ServerName receiver, ArrayList<AwardData> awards, UUID requestId) {
        super(receiver, awards, requestId);
    }

    public RespondAwardsEvent() {
    }
}

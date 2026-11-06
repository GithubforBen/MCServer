package com.benschnorr.communication.events.money;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.RespondDataEvent;
import com.benschnorr.types.money.BalanceResult;

import java.io.Serializable;
import java.util.UUID;

/** What the launcher made of a {@link ChangeBalanceEvent}. */
public class RespondBalanceChangeEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4505L;

    public RespondBalanceChangeEvent(ListenerAdapter.ServerName receiver, BalanceResult result, UUID requestId) {
        super(receiver, result, requestId);
    }

    public RespondBalanceChangeEvent() {
    }
}

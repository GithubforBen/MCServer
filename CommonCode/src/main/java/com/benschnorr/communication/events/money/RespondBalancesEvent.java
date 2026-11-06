package com.benschnorr.communication.events.money;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.RespondDataEvent;

import java.io.Serializable;
import java.util.HashMap;
import java.util.UUID;

/**
 * Every balance of the network, keyed the way the accounts are named: a player's uuid as text, a team's
 * name as it is written.
 */
public class RespondBalancesEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4503L;

    public RespondBalancesEvent(ListenerAdapter.ServerName receiver, HashMap<String, Integer> balances,
                                UUID requestId) {
        super(receiver, balances, requestId);
    }

    public RespondBalancesEvent() {
    }
}

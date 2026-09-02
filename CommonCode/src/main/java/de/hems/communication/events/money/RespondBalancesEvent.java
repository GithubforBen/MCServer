package de.hems.communication.events.money;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;

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

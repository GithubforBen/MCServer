package de.hems.communication.events.money;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.types.money.BalanceResult;

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

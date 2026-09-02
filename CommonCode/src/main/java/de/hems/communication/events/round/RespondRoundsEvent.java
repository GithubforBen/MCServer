package de.hems.communication.events.round;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.types.round.RoundSnapshot;

import java.io.Serializable;
import java.util.UUID;

/** The rounds and the rules, as the launcher has them. */
public class RespondRoundsEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4712L;

    public RespondRoundsEvent(ListenerAdapter.ServerName receiver, RoundSnapshot snapshot, UUID requestId) {
        super(receiver, snapshot, requestId);
    }

    public RespondRoundsEvent() {
    }
}

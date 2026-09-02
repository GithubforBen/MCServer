package de.hems.communication.events.round;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.types.round.RoundData;

import java.io.Serializable;

/** Writes a round down, creating it when the launcher has never seen its id. */
public class SaveRoundEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4713L;

    private RoundData round;

    public SaveRoundEvent() {
    }

    public SaveRoundEvent(RoundData round) {
        super(ListenerAdapter.ServerName.HOST);
        this.round = round;
    }

    public RoundData getRound() {
        return round;
    }
}

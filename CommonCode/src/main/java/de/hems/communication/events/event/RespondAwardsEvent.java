package de.hems.communication.events.event;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.types.event.AwardData;

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

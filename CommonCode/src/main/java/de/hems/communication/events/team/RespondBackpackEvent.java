package de.hems.communication.events.team;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.types.team.BackpackData;

import java.io.Serializable;
import java.util.UUID;

/** The contents of a team backpack. */
public class RespondBackpackEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4108L;

    public RespondBackpackEvent(ListenerAdapter.ServerName receiver, BackpackData backpack, UUID requestId) {
        super(receiver, backpack, requestId);
    }

    public RespondBackpackEvent() {
    }
}

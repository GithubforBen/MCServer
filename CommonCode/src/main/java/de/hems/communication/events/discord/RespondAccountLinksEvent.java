package de.hems.communication.events.discord;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.types.discord.AccountLink;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

/** Every link the launcher has. */
public class RespondAccountLinksEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4903L;

    public RespondAccountLinksEvent(ListenerAdapter.ServerName receiver, ArrayList<AccountLink> links,
                                    UUID requestId) {
        super(receiver, links, requestId);
    }

    public RespondAccountLinksEvent() {
    }
}

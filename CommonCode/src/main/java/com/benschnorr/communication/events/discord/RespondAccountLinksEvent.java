package com.benschnorr.communication.events.discord;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.RespondDataEvent;
import com.benschnorr.types.discord.AccountLink;

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

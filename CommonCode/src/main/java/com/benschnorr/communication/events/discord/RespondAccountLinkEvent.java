package com.benschnorr.communication.events.discord;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.RespondDataEvent;
import com.benschnorr.types.discord.AccountLink;

import java.io.Serializable;
import java.util.UUID;

/** What the launcher made of a code somebody typed in. */
public class RespondAccountLinkEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4905L;

    private boolean successful;
    private String message;

    public RespondAccountLinkEvent(ListenerAdapter.ServerName receiver, boolean successful, String message,
                                   AccountLink link, UUID requestId) {
        super(receiver, link, requestId);
        this.successful = successful;
        this.message = message;
    }

    public RespondAccountLinkEvent() {
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getMessage() {
        return message;
    }
}

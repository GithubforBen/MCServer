package de.hems.communication.events.discord;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.types.discord.AccountLink;

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

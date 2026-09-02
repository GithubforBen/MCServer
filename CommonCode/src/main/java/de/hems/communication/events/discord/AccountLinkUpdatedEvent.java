package de.hems.communication.events.discord;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.types.discord.AccountLink;

import java.io.Serializable;
import java.util.UUID;

/** Announces a new or removed link, so every server can answer "who is that on discord" right away. */
public class AccountLinkUpdatedEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4906L;

    private UUID minecraftId;
    /** The new state, or {@code null} when the link was removed. */
    private AccountLink link;

    public AccountLinkUpdatedEvent() {
    }

    public AccountLinkUpdatedEvent(UUID minecraftId, AccountLink link) {
        super(ListenerAdapter.ServerName.ALL);
        this.minecraftId = minecraftId;
        this.link = link;
    }

    public UUID getMinecraftId() {
        return minecraftId;
    }

    public AccountLink getLink() {
        return link;
    }

    public boolean isRemoved() {
        return link == null;
    }
}

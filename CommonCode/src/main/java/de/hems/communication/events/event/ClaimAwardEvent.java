package de.hems.communication.events.event;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.UUID;

/**
 * Marks a prize as collected.
 * <p>
 * Sent only once the items are actually in the player's inventory, so a prize that could not be handed over
 * stays waiting rather than quietly disappearing.
 */
public class ClaimAwardEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4324L;

    private UUID awardId;

    public ClaimAwardEvent(UUID awardId) {
        super(ListenerAdapter.ServerName.HOST);
        this.awardId = awardId;
    }

    public ClaimAwardEvent() {
    }

    public UUID getAwardId() {
        return awardId;
    }
}

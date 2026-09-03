package de.hems.communication.events.cosmetic;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.UUID;

/**
 * Asks the launcher what one player owns.
 * <p>
 * This is what a server asks when somebody joins it. The whole ownership map used to travel with the
 * catalogue, which works and stops working at the same moment: the answer grows with every player who has
 * ever bought anything, while a game server only ever needs the twenty people standing on it.
 */
public class RequestPlayerCosmeticsEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4819L;

    private UUID playerId;

    public RequestPlayerCosmeticsEvent() {
    }

    public RequestPlayerCosmeticsEvent(UUID playerId) {
        super(ListenerAdapter.ServerName.HOST);
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }
}

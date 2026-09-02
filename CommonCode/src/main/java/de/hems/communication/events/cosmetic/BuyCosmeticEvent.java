package de.hems.communication.events.cosmetic;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.UUID;

/**
 * Buys a cosmetic for a player.
 * <p>
 * The price is not in here on purpose. A buying server that sends the price it last saw is a buying server
 * that can be a second out of date, and the launcher would have no way to tell that from a lie.
 */
public class BuyCosmeticEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4815L;

    private UUID playerId;
    private String cosmeticId;

    public BuyCosmeticEvent() {
    }

    public BuyCosmeticEvent(UUID playerId, String cosmeticId) {
        super(ListenerAdapter.ServerName.HOST);
        this.playerId = playerId;
        this.cosmeticId = cosmeticId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getCosmeticId() {
        return cosmeticId;
    }
}

package de.hems.communication.events.cosmetic;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.types.cosmetic.CosmeticType;

import java.io.Serializable;
import java.util.UUID;

/** Puts a cosmetic on, or takes it off when the id is {@code null}. */
public class SelectCosmeticEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4817L;

    private UUID playerId;
    private CosmeticType type;
    private String cosmeticId;

    public SelectCosmeticEvent() {
    }

    public SelectCosmeticEvent(UUID playerId, CosmeticType type, String cosmeticId) {
        super(ListenerAdapter.ServerName.HOST);
        this.playerId = playerId;
        this.type = type;
        this.cosmeticId = cosmeticId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public CosmeticType getType() {
        return type;
    }

    public String getCosmeticId() {
        return cosmeticId;
    }
}

package de.hems.communication.events.cosmetic;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.types.cosmetic.CosmeticType;
import de.hems.types.cosmetic.GadgetSlot;

import java.io.Serializable;
import java.util.UUID;

/** Puts a cosmetic on, or takes it off when the id is {@code null}. */
public class SelectCosmeticEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4817L;

    private UUID playerId;
    private CosmeticType type;
    /** Which slot, for gadgets; {@code null} for the kinds that are worn in one place. */
    private GadgetSlot slot;
    private String cosmeticId;

    public SelectCosmeticEvent() {
    }

    public SelectCosmeticEvent(UUID playerId, CosmeticType type, String cosmeticId) {
        this(playerId, type, null, cosmeticId);
    }

    public SelectCosmeticEvent(UUID playerId, CosmeticType type, GadgetSlot slot, String cosmeticId) {
        super(ListenerAdapter.ServerName.HOST);
        this.playerId = playerId;
        this.type = type;
        this.slot = slot;
        this.cosmeticId = cosmeticId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public CosmeticType getType() {
        return type;
    }

    public GadgetSlot getSlot() {
        return slot;
    }

    public String getCosmeticId() {
        return cosmeticId;
    }
}

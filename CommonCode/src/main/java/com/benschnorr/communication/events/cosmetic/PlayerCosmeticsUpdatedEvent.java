package com.benschnorr.communication.events.cosmetic;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;
import com.benschnorr.types.cosmetic.PlayerCosmetics;

import java.io.Serializable;

/** Announces that one player's cosmetics changed, so the round they are about to win already knows. */
public class PlayerCosmeticsUpdatedEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4818L;

    private PlayerCosmetics cosmetics;

    public PlayerCosmeticsUpdatedEvent() {
    }

    public PlayerCosmeticsUpdatedEvent(PlayerCosmetics cosmetics) {
        super(ListenerAdapter.ServerName.ALL);
        this.cosmetics = cosmetics;
    }

    public PlayerCosmetics getCosmetics() {
        return cosmetics;
    }
}

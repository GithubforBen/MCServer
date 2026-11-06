package com.benschnorr.communication.events.cosmetic;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;
import com.benschnorr.types.cosmetic.CosmeticData;

import java.io.Serializable;

/** Announces that a cosmetic changed, so a price is the same everywhere a moment later. */
public class CosmeticUpdatedEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4814L;

    private CosmeticData cosmetic;

    public CosmeticUpdatedEvent() {
    }

    public CosmeticUpdatedEvent(CosmeticData cosmetic) {
        super(ListenerAdapter.ServerName.ALL);
        this.cosmetic = cosmetic;
    }

    public CosmeticData getCosmetic() {
        return cosmetic;
    }
}

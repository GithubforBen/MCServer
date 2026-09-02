package de.hems.communication.events.cosmetic;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.types.cosmetic.CosmeticData;

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

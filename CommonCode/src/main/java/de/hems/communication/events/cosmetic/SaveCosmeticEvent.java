package de.hems.communication.events.cosmetic;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.types.cosmetic.CosmeticData;

import java.io.Serializable;

/** Changes what an admin decides about a cosmetic: whether it exists, whether it sells, what it costs. */
public class SaveCosmeticEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4813L;

    private CosmeticData cosmetic;

    public SaveCosmeticEvent() {
    }

    public SaveCosmeticEvent(CosmeticData cosmetic) {
        super(ListenerAdapter.ServerName.HOST);
        this.cosmetic = cosmetic;
    }

    public CosmeticData getCosmetic() {
        return cosmetic;
    }
}

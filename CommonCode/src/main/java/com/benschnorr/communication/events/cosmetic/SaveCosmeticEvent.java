package com.benschnorr.communication.events.cosmetic;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventFoundationData;
import com.benschnorr.types.cosmetic.CosmeticData;

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

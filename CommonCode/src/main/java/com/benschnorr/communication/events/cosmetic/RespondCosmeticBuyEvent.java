package com.benschnorr.communication.events.cosmetic;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.RespondDataEvent;
import com.benschnorr.types.cosmetic.CosmeticPurchase;

import java.io.Serializable;
import java.util.UUID;

/** What the launcher made of a {@link BuyCosmeticEvent}. */
public class RespondCosmeticBuyEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4816L;

    public RespondCosmeticBuyEvent(ListenerAdapter.ServerName receiver, CosmeticPurchase purchase, UUID requestId) {
        super(receiver, purchase, requestId);
    }

    public RespondCosmeticBuyEvent() {
    }
}

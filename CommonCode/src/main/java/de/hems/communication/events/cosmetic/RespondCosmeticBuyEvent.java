package de.hems.communication.events.cosmetic;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.types.cosmetic.CosmeticPurchase;

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

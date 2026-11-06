package com.benschnorr.communication.events.cosmetic;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.RespondDataEvent;
import com.benschnorr.types.cosmetic.CosmeticSnapshot;

import java.io.Serializable;
import java.util.UUID;

/** The catalogue and the ownership, as the launcher has them. */
public class RespondCosmeticsEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4812L;

    public RespondCosmeticsEvent(ListenerAdapter.ServerName receiver, CosmeticSnapshot snapshot, UUID requestId) {
        super(receiver, snapshot, requestId);
    }

    public RespondCosmeticsEvent() {
    }
}

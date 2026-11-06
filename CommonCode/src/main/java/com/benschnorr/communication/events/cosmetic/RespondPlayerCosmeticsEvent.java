package com.benschnorr.communication.events.cosmetic;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.RespondDataEvent;
import com.benschnorr.types.cosmetic.PlayerCosmetics;

import java.io.Serializable;
import java.util.UUID;

/** What one player owns, as the launcher has it. */
public class RespondPlayerCosmeticsEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4820L;

    public RespondPlayerCosmeticsEvent(ListenerAdapter.ServerName receiver, PlayerCosmetics cosmetics,
                                       UUID requestId) {
        super(receiver, cosmetics, requestId);
    }

    public RespondPlayerCosmeticsEvent() {
    }
}

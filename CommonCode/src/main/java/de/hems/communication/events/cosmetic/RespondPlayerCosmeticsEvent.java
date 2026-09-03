package de.hems.communication.events.cosmetic;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.types.cosmetic.PlayerCosmetics;

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

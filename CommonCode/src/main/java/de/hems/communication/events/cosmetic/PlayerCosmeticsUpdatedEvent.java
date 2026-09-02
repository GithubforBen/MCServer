package de.hems.communication.events.cosmetic;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.types.cosmetic.PlayerCosmetics;

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

package de.hems.communication.events.money;

import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.util.UUID;

public class RequestPlayerMoneyEvent extends EventFoundationData implements Event {
    private final UUID playerId;
    public RequestPlayerMoneyEvent(String sender, String receiver, UUID playerId) {
        super(sender, receiver, UUID.randomUUID());
        this.playerId = playerId;
    }
    public UUID getPlayerId() {
        return playerId;
    }
}

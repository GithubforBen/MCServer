package de.hems.communication.events.money;

import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.util.UUID;

public class RespondPlayerMoneyEvent extends EventFoundationData implements Event {
    private final UUID playerId;
    private final int money;
    public RespondPlayerMoneyEvent(String sender, String receiver, UUID playerId, int money) {
        super(sender, receiver, UUID.randomUUID());
        this.playerId = playerId;
        this.money = money;
    }
    public UUID getPlayerId() {
        return playerId;
    }

    public int getMoney() {
        return money;
    }
}

package de.hems.communication.events.money;

import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.UUID;

public class RespondPlayerMoneyEvent extends EventFoundationData implements Event, Serializable {
    private static final long serialVersionUID = 4L;
    private  UUID playerId;
    private  int money;
    public RespondPlayerMoneyEvent(String sender, String receiver, UUID playerId, int money) {
        super(sender, receiver, UUID.randomUUID());
        this.playerId = playerId;
        this.money = money;
    }

    public RespondPlayerMoneyEvent() {
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public int getMoney() {
        return money;
    }
}

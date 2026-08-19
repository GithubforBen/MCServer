package de.hems.communication.events.event;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.UUID;

/** Asks the launcher what a player still has to collect. */
public class RequestAwardsEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4322L;

    private UUID player;

    public RequestAwardsEvent(UUID player) {
        super(ListenerAdapter.ServerName.HOST);
        this.player = player;
    }

    public RequestAwardsEvent() {
    }

    public UUID getPlayer() {
        return player;
    }
}

package de.hems.communication.events.admin;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.types.admin.InventoryData;

import java.io.Serializable;
import java.util.UUID;

/**
 * The contents of a container, or {@code null} data from a server that does not have the player.
 */
public class RespondInventoryEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 3104L;

    public RespondInventoryEvent(ListenerAdapter.ServerName receiver, InventoryData inventory, UUID requestId) {
        super(receiver, inventory, requestId);
    }

    public RespondInventoryEvent() {
    }
}

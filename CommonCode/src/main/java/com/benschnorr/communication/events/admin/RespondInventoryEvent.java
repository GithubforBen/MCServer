package com.benschnorr.communication.events.admin;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.RespondDataEvent;
import com.benschnorr.types.admin.InventoryData;

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

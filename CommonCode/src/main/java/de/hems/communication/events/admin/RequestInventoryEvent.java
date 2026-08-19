package de.hems.communication.events.admin;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.types.admin.InventoryData;

import java.io.Serializable;
import java.util.UUID;

/**
 * Asks for one container of a player. Broadcast, because the launcher does not track which server a player
 * is on - the server that has them answers with the contents, the others answer with nothing.
 */
public class RequestInventoryEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 3103L;

    private UUID playerId;
    private InventoryData.Kind kind;
    private String containerId;

    public RequestInventoryEvent(UUID playerId, InventoryData.Kind kind, String containerId) {
        super(ListenerAdapter.ServerName.ALL);
        this.playerId = playerId;
        this.kind = kind;
        this.containerId = containerId;
    }

    public RequestInventoryEvent() {
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public InventoryData.Kind getKind() {
        return kind;
    }

    public String getContainerId() {
        return containerId;
    }
}

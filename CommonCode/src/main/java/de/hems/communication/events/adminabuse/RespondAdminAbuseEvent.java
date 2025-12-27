package de.hems.communication.events.adminabuse;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.communication.events.types.RespondDataEvent;

import java.io.Serializable;
import java.util.UUID;

public class RespondAdminAbuseEvent extends RespondDataEvent implements Event, Serializable {
    private static final long serialVersionUID = 509080L;
    private UUID uuid;

    public RespondAdminAbuseEvent() {
    }

    public RespondAdminAbuseEvent(ListenerAdapter.ServerName receiver, UUID requestId, UUID uuid) {
        super(receiver, uuid, requestId);
        this.uuid = uuid;
    }

    public UUID getUuid() {
        if (uuid == null) {
            uuid = ((UUID) super.getData());
        }
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
}

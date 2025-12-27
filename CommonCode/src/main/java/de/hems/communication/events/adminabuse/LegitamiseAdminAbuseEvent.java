package de.hems.communication.events.adminabuse;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.UUID;

public class LegitamiseAdminAbuseEvent extends EventFoundationData implements Event, Serializable {
    private static final long serialVersionUID = 509090080L;
    private UUID adminAbuse;
    private String reason;

    public LegitamiseAdminAbuseEvent() {
    }

    public LegitamiseAdminAbuseEvent(ListenerAdapter.ServerName receiver, UUID adminAbuse, String reason) {
        super(receiver);
        this.adminAbuse = adminAbuse;
        this.reason = reason;
    }

    public UUID getAdminAbuse() {
        return adminAbuse;
    }

    public void setAdminAbuse(UUID adminAbuse) {
        this.adminAbuse = adminAbuse;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

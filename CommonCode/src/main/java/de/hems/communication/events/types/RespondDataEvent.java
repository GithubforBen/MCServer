package de.hems.communication.events.types;

import de.hems.communication.ListenerAdapter;

import java.io.Serializable;
import java.util.UUID;

public class RespondDataEvent extends EventFoundationData implements Event, Serializable {
    private static final long serialVersionUID = 14L;
    private Object data;
    private UUID requestId;

    public RespondDataEvent(ListenerAdapter.ServerName receiver, Object data, UUID requestId) {
        super(receiver);
        this.data = data;
        this.requestId = requestId;
    }

    public RespondDataEvent() {
    }

    public Object getData() {
        return data;
    }

    public UUID getRequestId() {
        return requestId;
    }
}

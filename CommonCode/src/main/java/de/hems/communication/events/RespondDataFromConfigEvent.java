package de.hems.communication.events;

import java.util.UUID;

public class RespondDataFromConfigEvent extends EventFoundationData implements Event{
    private Object data;
    private String key;
    private UUID requestId;

    public RespondDataFromConfigEvent(String sender, String receiver, Object data, String key, UUID requestId) {
        super(sender, receiver, UUID.randomUUID());
        this.data = data;
        this.key = key;
        this.requestId = requestId;
    }

    public Object getData() {
        return data;
    }

    public String getKey() {
        return key;
    }

    public UUID getRequestId() {
        return requestId;
    }
}

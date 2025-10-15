package de.hems.communication.events;

import java.util.UUID;

public class RequestDataFromConfigEvent extends EventFoundationData implements Event {
    private final String key;

    public RequestDataFromConfigEvent(String sender, String receiver, String key) {
        super(sender, receiver, UUID.randomUUID());
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}

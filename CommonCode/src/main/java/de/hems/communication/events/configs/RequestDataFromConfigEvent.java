package de.hems.communication.events.configs;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.UUID;

public class RequestDataFromConfigEvent extends EventFoundationData implements Event, Serializable {
    private static final long serialVersionUID = 1L;
    private String key;

    public RequestDataFromConfigEvent(ListenerAdapter.ServerName sender, UUID eventId, String key) {
        super(sender, ListenerAdapter.ServerName.HOST, eventId);
        this.key = key;
    }

    public RequestDataFromConfigEvent() {
    }

    public String getKey() {
        return key;
    }
}

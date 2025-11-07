package de.hems.communication.events.configs;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.communication.events.types.RespondDataEvent;

import java.io.Serializable;
import java.util.UUID;

public class RespondDataFromConfigEvent extends RespondDataEvent implements Event, Serializable {
    private static final long serialVersionUID = 2L;
    private String key;

    public RespondDataFromConfigEvent(ListenerAdapter.ServerName receiver, Object data, String key, UUID requestId) {
        super(receiver, data, requestId);
        this.key = key;
    }

    public RespondDataFromConfigEvent() {
    }
}

package de.hems.communication.events.server;

import de.hems.FileType;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.UUID;

public class RequestServerStartEvent extends EventFoundationData implements Event, Serializable {
    private static final long serialVersionUID = 5L;
    private String serverName;
    private FileType.SERVER type;
    private Integer port;
    private Integer memory;

    public RequestServerStartEvent(ListenerAdapter.ServerName sender, ListenerAdapter.ServerName receiver, UUID eventId, String serverName, FileType.SERVER type, Integer port, Integer memory) {
        super(sender, receiver, eventId);
        this.serverName = serverName;
        this.type = type;
        this.port = port;
        this.memory = memory;
    }

    public RequestServerStartEvent() {
    }


    public String getServerName() {
        return serverName;
    }

    public FileType.SERVER getType() {
        return type;
    }

    public Integer getPort() {
        return port;
    }

    public Integer getMemory() {
        return memory;
    }
}

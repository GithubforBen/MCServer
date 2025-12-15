package de.hems.communication.events.server;

import de.hems.types.FileType;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.UUID;

public class RequestServerStartEvent extends EventFoundationData implements Event, Serializable {
    private static final long serialVersionUID = 5L;
    private ListenerAdapter.ServerName serverName;
    private FileType.SERVER type;
    private Integer memory;
    private FileType.PLUGIN[] plugins;

    public RequestServerStartEvent(ListenerAdapter.ServerName receiver, ListenerAdapter.ServerName serverName, FileType.SERVER type, Integer memory, FileType.PLUGIN[] plugins) {
        super(receiver);
        this.serverName = serverName;
        this.type = type;
        this.memory = memory;
        this.plugins = plugins;
    }

    public RequestServerStartEvent() {
    }


    public ListenerAdapter.ServerName getServerName() {
        return serverName;
    }

    public FileType.SERVER getType() {
        return type;
    }

    public Integer getMemory() {
        return memory;
    }
    public FileType.PLUGIN[] getPlugins() {
        return plugins;
    }
}

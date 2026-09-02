package de.hems.communication.events.server;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;

/**
 * Changes how much heap a server is started with next time.
 * <p>
 * Next time, not now: a running jvm's heap is fixed when it starts, so this writes the number down and the
 * server picks it up when it comes up again. Saying otherwise would be a lie an admin only finds out about
 * when the machine is full anyway.
 */
public class SetServerMemoryEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4607L;

    private String serverName;
    private int memoryMB;

    public SetServerMemoryEvent() {
    }

    public SetServerMemoryEvent(String serverName, int memoryMB) {
        super(ListenerAdapter.ServerName.HOST);
        this.serverName = serverName;
        this.memoryMB = memoryMB;
    }

    public String getServerName() {
        return serverName;
    }

    public int getMemoryMB() {
        return memoryMB;
    }
}

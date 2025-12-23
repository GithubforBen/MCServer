package de.hems.communication.events.adminabuse;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;

public class RequestAdminAbuseEvent extends EventFoundationData implements Event, Serializable {
    private static final long serialVersionUID = 5090980L;
    private String command;
    private String player;
    private long time;

    public RequestAdminAbuseEvent() {
    }

    public RequestAdminAbuseEvent(ListenerAdapter.ServerName receiver, String command, String player) {
        super(receiver);
        this.command = command;
        this.player = player;
        this.time = System.currentTimeMillis();
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getPlayer() {
        return player;
    }

    public void setPlayer(String player) {
        this.player = player;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}

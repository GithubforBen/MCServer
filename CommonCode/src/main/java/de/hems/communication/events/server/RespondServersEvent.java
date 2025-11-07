package de.hems.communication.events.server;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.types.Server;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public class RespondServersEvent extends RespondDataEvent implements Event, Serializable {
    private static final long serialVersionUID = 90L;

    public RespondServersEvent() {
    }

    public RespondServersEvent(ListenerAdapter.ServerName receiver, Server[] data, UUID requestId) {
        super(receiver, data, requestId);
    }




    public Server[] getData() {
        if (!(super.getData() instanceof Server[])) {
            throw new ClassCastException("The data of this event is not a list of servers");
        }
        return (Server[]) super.getData();
    }
}

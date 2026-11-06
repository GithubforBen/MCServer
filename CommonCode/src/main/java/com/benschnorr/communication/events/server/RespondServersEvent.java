package com.benschnorr.communication.events.server;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.RespondDataEvent;
import com.benschnorr.types.Server;

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

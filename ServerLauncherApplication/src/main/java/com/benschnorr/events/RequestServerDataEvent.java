package com.benschnorr.events;

import com.benschnorr.Main;
import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.server.RequestServersEvent;
import com.benschnorr.communication.events.server.RespondServersEvent;
import com.benschnorr.types.Server;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventHandler;

public class RequestServerDataEvent implements EventHandler<RequestServersEvent> {
    public RequestServerDataEvent() {
        ListenerAdapter.register(RequestServersEvent.class, this);
    }
    @Override
    public void onEvent(Event event) throws Exception {
        // asked once a second by everyone waiting for a server to come up, so this stays quiet
        if (!(event instanceof RequestServersEvent request)) {
            return;
        }
        ListenerAdapter.sendListeners(new RespondServersEvent(request.getSender(),
                Main.getInstance().getServerHandler().collectToServer(), request.getEventId()));
    }
}

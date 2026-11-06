package com.benschnorr.events;

import com.benschnorr.Main;
import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.server.RequestServerStartEvent;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventHandler;

/**
 * Starts servers other nodes ask for. The name does not have to be known before, which is what lets the
 * network grow to any number of servers - the handler simply creates it.
 */
public class StartServerEvent implements EventHandler<RequestServerStartEvent> {
    public StartServerEvent() {
        ListenerAdapter.register(RequestServerStartEvent.class, this);
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof RequestServerStartEvent request)) {
            return;
        }
        try {
            Main.getInstance().getServerHandler().startNewInstance(
                    request.getServerName(),
                    request.getMemory(),
                    request.getType(),
                    request.getPlugins(),
                    request.getTemplate());
        } catch (Exception e) {
            System.out.println("Could not start " + request.getServerName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}

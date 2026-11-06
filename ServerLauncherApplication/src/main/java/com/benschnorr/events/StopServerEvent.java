package com.benschnorr.events;

import com.benschnorr.Main;
import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.server.RequestServerStopEvent;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventHandler;

/**
 * Stops servers other nodes ask for.
 */
public class StopServerEvent implements EventHandler<RequestServerStopEvent> {
    public StopServerEvent() {
        ListenerAdapter.register(RequestServerStopEvent.class, this);
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof RequestServerStopEvent request)) {
            return;
        }
        try {
            Main.getInstance().getServerHandler().stop(request.getServerName());
        } catch (RuntimeException e) {
            System.out.println("Could not stop " + request.getServerName() + ": " + e.getMessage());
        }
    }
}

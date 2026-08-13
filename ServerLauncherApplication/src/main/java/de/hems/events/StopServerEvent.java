package de.hems.events;

import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.server.RequestServerStopEvent;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventHandler;

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

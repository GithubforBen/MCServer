package de.hems.events;

import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.server.RequestServerStartEvent;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventHandler;

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

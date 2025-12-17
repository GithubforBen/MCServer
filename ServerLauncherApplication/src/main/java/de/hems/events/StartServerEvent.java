package de.hems.events;

import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.server.RequestServerStartEvent;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventHandler;

import java.io.IOException;

public class StartServerEvent implements EventHandler<RequestServerStartEvent> {
    public StartServerEvent() {
        ListenerAdapter.register(RequestServerStartEvent.class, this);
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof RequestServerStartEvent)) {
            return;
        }
        event = (RequestServerStartEvent) event;
        try {
            Main.getInstance().getServerHandler().startNewInstance(((RequestServerStartEvent) event).getServerName(),
                    ((RequestServerStartEvent) event).getMemory(), ((RequestServerStartEvent) event).getType(), ((RequestServerStartEvent) event).getPlugins());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

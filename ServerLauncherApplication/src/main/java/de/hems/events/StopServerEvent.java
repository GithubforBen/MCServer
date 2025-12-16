package de.hems.events;

import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.server.RequestServerStartEvent;
import de.hems.communication.events.server.RequestServerStopEvent;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventHandler;

public class StopServerEvent implements EventHandler<RequestServerStopEvent> {
    public StopServerEvent() {
        ListenerAdapter.register(RequestServerStopEvent.class, this);
    }
    @Override
    public void onEvent(Event event) throws Exception {
        if (!(event instanceof RequestServerStopEvent)) {
            return;
        }
        event = event;
        Main.getInstance().getServerHandler().stop(((RequestServerStopEvent) event).getServerName());
    }
}

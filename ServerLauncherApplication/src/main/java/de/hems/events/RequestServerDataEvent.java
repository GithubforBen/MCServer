package de.hems.events;

import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.server.RequestServersEvent;
import de.hems.communication.events.server.RespondServersEvent;
import de.hems.types.Server;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventHandler;

public class RequestServerDataEvent implements EventHandler<RequestServersEvent> {
    public RequestServerDataEvent() {
        ListenerAdapter.register(RequestServersEvent.class, this);
    }
    @Override
    public void onEvent(Event event) throws Exception {
        System.out.println("EVENT");
        if (!(event instanceof RequestServersEvent)) {
            return;
        }
        event = (RequestServersEvent) event;
        System.out.println("EVENT");
        ListenerAdapter.sendListeners(new RespondServersEvent(((RequestServersEvent) event).getSender(), Main.getInstance().getServerHandler().collectToServer(), ((RequestServersEvent) event).getEventId()));
    }
}

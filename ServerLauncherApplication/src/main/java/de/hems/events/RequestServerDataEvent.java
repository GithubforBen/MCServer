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
        // asked once a second by everyone waiting for a server to come up, so this stays quiet
        if (!(event instanceof RequestServersEvent request)) {
            return;
        }
        ListenerAdapter.sendListeners(new RespondServersEvent(request.getSender(),
                Main.getInstance().getServerHandler().collectToServer(), request.getEventId()));
    }
}

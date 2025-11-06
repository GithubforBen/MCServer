package de.hems.events;

import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.server.RequestServerRestartEvent;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventHandler;

public class RestartServerEvent  implements EventHandler<RequestServerRestartEvent> {
    public RestartServerEvent() {
        ListenerAdapter.register(RequestServerRestartEvent.class, this);
    }

    @Override
    public void onEvent(Event event) throws Exception {
        if (!(event instanceof RequestServerRestartEvent)) {
            return;
        }
        Main.getInstance().getServerHandler().stop(((RequestServerRestartEvent) event).getServerName());
    }
}

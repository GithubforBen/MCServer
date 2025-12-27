package de.hems.utils.bot.adminabuse;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.adminabuse.RequestToLegitimizeEvent;
import de.hems.communication.events.adminabuse.RespondToLegitimizeEvent;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RequestToLegitimise implements EventHandler<RequestToLegitimizeEvent> {
    public RequestToLegitimise() {
        ListenerAdapter.register(RequestToLegitimizeEvent.class, this);
    }
    @Override
    public void onEvent(Event event) throws Exception {
        if (!(event instanceof RequestToLegitimizeEvent request)) {
            return;
        }
        System.out.println(2);
        Map<UUID, String> commands = new HashMap<UUID, String>();
        for (AdminAbuse adminAbus : AdminAbuseHandler.adminAbuses) {
            if (adminAbus.getReason() != null) continue;
            commands.put(adminAbus.getUuid(), adminAbus.getCommand());
        }
        System.out.println(1);
        ListenerAdapter.sendListeners(new RespondToLegitimizeEvent(request.getSender(), request.getEventId(), commands));
        System.out.println(4);
    }
}

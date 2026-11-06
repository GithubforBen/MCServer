package com.benschnorr.utils.bot.adminabuse;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.adminabuse.RequestToLegitimizeEvent;
import com.benschnorr.communication.events.adminabuse.RespondToLegitimizeEvent;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventHandler;

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
        Map<UUID, String> commands = new HashMap<UUID, String>();
        for (AdminAbuse adminAbus : AdminAbuseHandler.adminAbuses) {
            if (adminAbus.getReason() != null) continue;
            commands.put(adminAbus.getUuid(), adminAbus.getCommand());
        }
        ListenerAdapter.sendListeners(new RespondToLegitimizeEvent(request.getSender(), request.getEventId(), commands));
    }
}

package com.benschnorr.utils.bot.adminabuse;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.adminabuse.RequestAdminAbuseEvent;
import com.benschnorr.communication.events.adminabuse.RespondAdminAbuseEvent;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventHandler;

import java.util.UUID;

public class RequestAdminAbuse implements EventHandler<RequestAdminAbuseEvent> {
    public RequestAdminAbuse() {
        ListenerAdapter.register(RequestAdminAbuseEvent.class, this);
    }
    @Override
    public void onEvent(Event event) throws Exception {
        if (!(event instanceof RequestAdminAbuseEvent)) {
            return;
        }
        RequestAdminAbuseEvent abuseEvent = ((RequestAdminAbuseEvent) event);
        UUID uuid = AdminAbuseHandler.addAdminAbuse(abuseEvent.getCommand(), abuseEvent.getPlayer(), abuseEvent.getTime());
        ListenerAdapter.sendListeners(new RespondAdminAbuseEvent(((RequestAdminAbuseEvent) event).getSender(), ((RequestAdminAbuseEvent) event).getEventId(), uuid));
    }
}

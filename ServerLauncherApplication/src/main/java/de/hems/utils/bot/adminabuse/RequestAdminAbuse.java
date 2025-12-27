package de.hems.utils.bot.adminabuse;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.adminabuse.RequestAdminAbuseEvent;
import de.hems.communication.events.adminabuse.RespondAdminAbuseEvent;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventHandler;

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

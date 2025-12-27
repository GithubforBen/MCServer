package de.hems.utils.bot.adminabuse;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.adminabuse.LegitamiseAdminAbuseEvent;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventHandler;

public class LegitimiseAdminAbuse implements EventHandler<LegitamiseAdminAbuseEvent> {
    public LegitimiseAdminAbuse() {
        ListenerAdapter.register(LegitamiseAdminAbuseEvent.class, this);
    }

    @Override
    public void onEvent(Event event) throws Exception {
        if (!(event instanceof LegitamiseAdminAbuseEvent)) {
            return;
        }
        LegitamiseAdminAbuseEvent abuseEvent = ((LegitamiseAdminAbuseEvent) event);
        AdminAbuseHandler.reasonAdminAbuse(abuseEvent.getAdminAbuse(), abuseEvent.getReason());
    }
}

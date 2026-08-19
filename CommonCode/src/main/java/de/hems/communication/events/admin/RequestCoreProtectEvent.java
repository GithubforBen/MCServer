package de.hems.communication.events.admin;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.types.admin.LookupQuery;

import java.io.Serializable;

/**
 * Runs a CoreProtect lookup on one server.
 * <p>
 * Unlike the other admin events this one is addressed, because CoreProtect keeps a separate database per
 * server and the answer only makes sense together with the server it came from.
 */
public class RequestCoreProtectEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 3108L;

    private LookupQuery query;

    public RequestCoreProtectEvent(ListenerAdapter.ServerName receiver, LookupQuery query) {
        super(receiver);
        this.query = query;
    }

    public RequestCoreProtectEvent() {
    }

    public LookupQuery getQuery() {
        return query;
    }
}

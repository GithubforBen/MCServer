package de.hems.communication.events.cosmetic;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;

/** Asks the launcher for the cosmetic catalogue and who owns what. */
public class RequestCosmeticsEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4811L;

    public RequestCosmeticsEvent() {
        super(ListenerAdapter.ServerName.HOST);
    }
}

package de.hems.communication.events.cosmetic;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;

/**
 * Asks the launcher for the cosmetic catalogue, and optionally for who owns what.
 * <p>
 * The flag is written the way round it is - "catalogue only" rather than "with the players" - so that an
 * older server, whose events do not have the field at all, deserialises to {@code false} and still gets
 * everything. A field that changes what an old caller gets is a field that breaks a rolling update.
 */
public class RequestCosmeticsEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4811L;

    private boolean catalogOnly;

    public RequestCosmeticsEvent() {
        this(false);
    }

    /**
     * @param catalogOnly whether the ownership of every player on the network may be left out
     */
    public RequestCosmeticsEvent(boolean catalogOnly) {
        super(ListenerAdapter.ServerName.HOST);
        this.catalogOnly = catalogOnly;
    }

    /**
     * @return whether the answer may leave the players out
     */
    public boolean isCatalogOnly() {
        return catalogOnly;
    }
}

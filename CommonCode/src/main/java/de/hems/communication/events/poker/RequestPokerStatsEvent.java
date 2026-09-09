package de.hems.communication.events.poker;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;

/**
 * Asks the launcher for every row of every poker night.
 * <p>
 * Deliberately not per event: the lobby wants the ranking of the night that is running, the casino server
 * wants the rows of its own night, and both of them keep the whole list anyway because it is a handful of
 * rows per evening. One request that everybody can use beats three that each need their own answer.
 */
public class RequestPokerStatsEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4331L;

    public RequestPokerStatsEvent() {
        super(ListenerAdapter.ServerName.HOST);
    }
}

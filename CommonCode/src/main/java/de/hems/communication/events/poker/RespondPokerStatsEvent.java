package de.hems.communication.events.poker;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.types.poker.PokerStatsData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

/** Every poker row the launcher knows. */
public class RespondPokerStatsEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4334L;

    public RespondPokerStatsEvent(ListenerAdapter.ServerName receiver, ArrayList<PokerStatsData> rows,
                                  UUID requestId) {
        super(receiver, rows, requestId);
    }

    public RespondPokerStatsEvent() {
    }
}

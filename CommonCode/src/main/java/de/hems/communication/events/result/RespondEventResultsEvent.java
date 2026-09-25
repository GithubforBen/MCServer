package de.hems.communication.events.result;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.types.event.EventResultData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

/** The result lines of one event. */
public class RespondEventResultsEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4346L;

    public RespondEventResultsEvent(ListenerAdapter.ServerName receiver, ArrayList<EventResultData> rows,
                                    UUID requestId) {
        super(receiver, rows, requestId);
    }

    public RespondEventResultsEvent() {
    }
}

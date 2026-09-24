package de.hems.events;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.result.RequestEventResultsEvent;
import de.hems.communication.events.result.RespondEventResultsEvent;
import de.hems.communication.events.result.SaveEventResultsEvent;
import de.hems.utils.event.EventResultStore;

import java.util.ArrayList;

/**
 * Serves the results of the events that rank people by where they finished.
 */
public class EventResultEvents {

    private final EventResultStore results;

    public EventResultEvents(EventResultStore results) {
        this.results = results;
        ListenerAdapter.register(SaveEventResultsEvent.class,
                event -> results.put(((SaveEventResultsEvent) event).getRows()));
        ListenerAdapter.register(RequestEventResultsEvent.class,
                event -> onRequest((RequestEventResultsEvent) event));
    }

    private void onRequest(RequestEventResultsEvent request) throws Exception {
        ListenerAdapter.sendListeners(new RespondEventResultsEvent(request.getSender(),
                new ArrayList<>(results.getRowsOf(request.getEventUuid())), request.getEventId()));
    }
}

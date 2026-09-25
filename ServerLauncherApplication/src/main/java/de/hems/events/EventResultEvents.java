package de.hems.events;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.result.RequestEventResultsEvent;
import de.hems.communication.events.result.RespondEventResultsEvent;
import de.hems.communication.events.result.SaveEventResultsEvent;
import de.hems.types.event.EventData;
import de.hems.types.event.EventResultData;
import de.hems.utils.event.EventResultStore;
import de.hems.utils.event.EventStore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Serves the results of the events that rank people by where they finished.
 */
public class EventResultEvents {

    private final EventResultStore results;
    private final EventStore events;

    public EventResultEvents(EventResultStore results, EventStore events) {
        this.results = results;
        this.events = events;
        ListenerAdapter.register(SaveEventResultsEvent.class,
                event -> onSave((SaveEventResultsEvent) event));
        ListenerAdapter.register(RequestEventResultsEvent.class,
                event -> onRequest((RequestEventResultsEvent) event));
    }

    /**
     * Takes lines in - but only for an event that still waits to be settled. A line that arrives after the
     * settlement, or for an event that was deleted, would lie in the file forever and pay nobody.
     */
    private void onSave(SaveEventResultsEvent request) {
        List<EventResultData> accepted = new ArrayList<>();
        for (EventResultData row : request.getRows()) {
            EventData event = row == null ? null : events.getEvent(row.getEventId());
            if (event == null || event.isApplied()) continue;
            accepted.add(row);
        }
        if (accepted.size() < request.getRows().size()) {
            System.out.println("Dropped " + (request.getRows().size() - accepted.size())
                    + " result lines for events that are settled or gone.");
        }
        results.put(accepted);
        UUID over = request.getFinishedEvent();
        EventData finishedEvent = over == null ? null : events.getEvent(over);
        if (finishedEvent != null && !finishedEvent.isApplied()) {
            results.markFinished(over);
            System.out.println("The game of " + finishedEvent.getName() + " is over - it is settled next.");
        }
    }

    private void onRequest(RequestEventResultsEvent request) throws Exception {
        ListenerAdapter.sendListeners(new RespondEventResultsEvent(request.getSender(),
                new ArrayList<>(results.getRowsOf(request.getEventUuid())), request.getEventId()));
    }
}

package de.hems.events;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.event.DeleteEventEvent;
import de.hems.communication.events.event.EventUpdatedEvent;
import de.hems.communication.events.event.RequestEventsEvent;
import de.hems.communication.events.event.RespondEventSaveEvent;
import de.hems.communication.events.event.RespondEventsEvent;
import de.hems.communication.events.event.SaveEventEvent;
import de.hems.communication.events.event.ClaimAwardEvent;
import de.hems.communication.events.event.RespondClaimAwardEvent;
import de.hems.communication.events.event.RequestAwardsEvent;
import de.hems.communication.events.event.RequestRunsEvent;
import de.hems.communication.events.event.RespondAwardsEvent;
import de.hems.communication.events.event.RespondRunsEvent;
import de.hems.communication.events.event.RunUpdatedEvent;
import de.hems.communication.events.event.SaveRunEvent;
import de.hems.types.event.EventData;
import de.hems.types.event.RunData;
import de.hems.utils.event.AwardStore;
import de.hems.utils.event.EventSettlement;
import de.hems.utils.event.EventStore;
import de.hems.utils.event.RunStore;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Serves the events of the network.
 * <p>
 * The launcher is the only node that writes them, which is what makes an event created in the lobby show up
 * on survival and on the website: after every write the new state is announced, so nobody has to poll.
 */
public class EventEvents {

    private final EventStore events;
    private final RunStore runs;
    private final AwardStore awards;
    private final EventSettlement settlement;

    public EventEvents(EventStore events, RunStore runs, AwardStore awards, EventSettlement settlement) {
        this.events = events;
        this.runs = runs;
        this.awards = awards;
        this.settlement = settlement;
        ListenerAdapter.register(RequestEventsEvent.class, event -> onRequestEvents((RequestEventsEvent) event));
        ListenerAdapter.register(SaveEventEvent.class, event -> onSaveEvent((SaveEventEvent) event));
        ListenerAdapter.register(DeleteEventEvent.class, event -> onDeleteEvent((DeleteEventEvent) event));
        ListenerAdapter.register(RequestRunsEvent.class, event -> onRequestRuns((RequestRunsEvent) event));
        ListenerAdapter.register(SaveRunEvent.class, event -> onSaveRun((SaveRunEvent) event));
        ListenerAdapter.register(RequestAwardsEvent.class, event -> onRequestAwards((RequestAwardsEvent) event));
        ListenerAdapter.register(ClaimAwardEvent.class, event -> onClaimAward((ClaimAwardEvent) event));
    }

    private void onRequestAwards(RequestAwardsEvent request) throws Exception {
        ListenerAdapter.sendListeners(new RespondAwardsEvent(request.getSender(),
                new ArrayList<>(awards.getUnclaimed(request.getPlayer())), request.getEventId()));
    }

    /**
     * Reserves a prize for the server that asked, or takes a reservation back. The answer is what the game
     * server waits for before it hands anything over, so only the first server ever gets a yes.
     *
     * @param request the prize
     */
    private void onClaimAward(ClaimAwardEvent request) throws Exception {
        if (request.isRelease()) {
            awards.release(request.getAwardId(), String.valueOf(request.getSender()));
            return;
        }
        boolean claimed = awards.claim(request.getAwardId(), String.valueOf(request.getSender()));
        ListenerAdapter.sendListeners(new RespondClaimAwardEvent(request.getSender(), claimed,
                request.getEventId()));
    }

    private void onRequestRuns(RequestRunsEvent request) throws Exception {
        ListenerAdapter.sendListeners(new RespondRunsEvent(
                request.getSender(), new ArrayList<>(runs.getRuns()), request.getEventId()));
    }

    /**
     * Takes a run in and tells everyone. Nobody waits for an answer, so a boss kill never stalls the
     * server it happened on.
     *
     * @param request the run to store
     */
    private void onSaveRun(SaveRunEvent request) throws Exception {
        RunData stored = runs.put(request.getRun());
        if (stored == null) return;
        try {
            ListenerAdapter.sendListeners(new RunUpdatedEvent(stored.getId(), stored));
        } catch (Exception e) {
            System.out.println("Could not announce the run " + stored.getId() + ": " + e.getMessage());
        }
    }

    private void onRequestEvents(RequestEventsEvent request) throws Exception {
        ListenerAdapter.sendListeners(new RespondEventsEvent(
                request.getSender(), new ArrayList<>(events.getEvents()), request.getEventId()));
    }

    private void onSaveEvent(SaveEventEvent request) throws Exception {
        EventStore.Result result = events.put(request.getEvent(), request.isCreateIfMissing());
        ListenerAdapter.sendListeners(new RespondEventSaveEvent(
                request.getSender(), result.successful(), result.message(), result.event(), request.getEventId()));
        if (result.successful()) announce(result.event().getId(), result.event());
    }

    private void onDeleteEvent(DeleteEventEvent request) throws Exception {
        // the runs, results, open poker stacks and the server belong to the event and go with it
        settlement.delete(request.getEventUuid());
    }

    /**
     * Tells the whole network about an event, so every server and the website follow along.
     *
     * @param id    the event that changed
     * @param event its new state, or {@code null} if it was deleted
     */
    private void announce(UUID id, EventData event) {
        try {
            ListenerAdapter.sendListeners(new EventUpdatedEvent(id, event));
        } catch (Exception e) {
            System.out.println("Could not announce the event " + id + ": " + e.getMessage());
        }
    }
}

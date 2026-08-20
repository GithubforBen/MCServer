package de.hems.utils.webconsole.modules;

import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.event.EventUpdatedEvent;
import de.hems.types.event.EventData;
import de.hems.types.event.EventType;
import de.hems.utils.event.EventStore;
import de.hems.utils.webconsole.ApiContext;
import de.hems.utils.webconsole.WebModule;
import de.hems.utils.webconsole.WebServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.UUID;

/**
 * The event calendar on the website. Shows the same events the in-game calendar does, but as time spans
 * rather than as an inventory.
 */
public class EventModule implements WebModule {

    @Override
    public String getId() {
        return "events";
    }

    @Override
    public String getTitle() {
        return "Events";
    }

    @Override
    public String getDescription() {
        return "Zeigt den Eventkalender und legt neue Events an.";
    }

    @Override
    public void register(WebServer server) {
        server.get("/api/events", ctx -> ctx.ok("events", listEvents()));
        server.get("/api/events/types", ctx -> ctx.ok("types", listTypes()));
        server.post("/api/events", this::create);
        server.post("/api/events/{event}/cancel", this::cancel);
        server.delete("/api/events/{event}", this::delete);
    }

    /**
     * @return every event, soonest first, with the fields the timeline needs
     */
    private static JSONArray listEvents() {
        JSONArray array = new JSONArray();
        for (EventData event : store().getEvents()) {
            JSONObject json = new JSONObject()
                    .put("id", event.getId().toString())
                    .put("name", event.getName())
                    .put("type", event.getType().name())
                    .put("typeTitle", event.getType().getTitle())
                    .put("description", event.getDescription() == null ? "" : event.getDescription())
                    .put("startsAt", event.getStartsAt())
                    .put("endsAt", event.getEndsAt())
                    .put("state", event.getState().name())
                    .put("stateTitle", event.getState().getTitle())
                    .put("cancelled", event.isCancelled())
                    .put("revision", event.getRevision());
            JSONObject settings = new JSONObject();
            for (Map.Entry<String, String> setting : event.getSettings().entrySet()) {
                settings.put(setting.getKey(), setting.getValue());
            }
            array.put(json.put("settings", settings));
        }
        return array;
    }

    /**
     * @return the kinds of event that can be created
     */
    private static JSONArray listTypes() {
        JSONArray array = new JSONArray();
        for (EventType type : EventType.values()) {
            array.put(new JSONObject()
                    .put("name", type.name())
                    .put("title", type.getTitle())
                    .put("onlyOnce", type.isOnlyOnce())
                    .put("timed", type.isTimed())
                    .put("hasMechanics", type.hasMechanics()));
        }
        return array;
    }

    /**
     * Creates an event.
     *
     * @param ctx the request being answered
     */
    private void create(ApiContext ctx) {
        String name = ctx.string("name", "").trim();
        if (name.isEmpty()) {
            ctx.error(400, "Es fehlt der Name des Events.");
            return;
        }
        EventType type = EventType.byName(ctx.string("type", "SIMPLE"));
        if (type == null) {
            ctx.error(400, "Unbekannter Eventtyp.");
            return;
        }
        long startsAt = (long) ctx.integer("startsAt", 0);
        long endsAt = (long) ctx.integer("endsAt", 0);
        // the browser sends milliseconds, which does not survive an int - read them as strings instead
        try {
            startsAt = Long.parseLong(ctx.string("startsAt", String.valueOf(startsAt)));
            endsAt = Long.parseLong(ctx.string("endsAt", String.valueOf(endsAt)));
        } catch (NumberFormatException e) {
            ctx.error(400, "Anfang und Ende müssen Zeitstempel sein.");
            return;
        }
        if (endsAt <= startsAt) {
            ctx.error(400, "Das Event endet vor seinem Anfang.");
            return;
        }
        EventData event = new EventData(name, type, startsAt, endsAt);
        event.setDescription(ctx.string("description", ""));
        EventStore.Result result = store().put(event, true);
        if (!result.successful()) {
            ctx.error(409, result.message());
            return;
        }
        announce(result.event().getId(), result.event());
        ctx.ok(name + " wurde angelegt.");
    }

    /**
     * Calls an event off, or puts it back on.
     *
     * @param ctx the request being answered
     */
    private void cancel(ApiContext ctx) {
        EventData event = resolve(ctx);
        if (event == null) return;
        EventData edited = event.copy();
        edited.setCancelled(!event.isCancelled());
        EventStore.Result result = store().put(edited, false);
        if (!result.successful()) {
            ctx.error(409, result.message());
            return;
        }
        announce(result.event().getId(), result.event());
        ctx.ok(edited.getName() + (edited.isCancelled() ? " wurde abgesagt." : " findet wieder statt."));
    }

    /**
     * Removes an event.
     *
     * @param ctx the request being answered
     */
    private void delete(ApiContext ctx) {
        EventData event = resolve(ctx);
        if (event == null) return;
        if (!store().delete(event.getId())) {
            ctx.error(404, "Dieses Event gibt es nicht.");
            return;
        }
        announce(event.getId(), null);
        ctx.ok(event.getName() + " wurde gelöscht.");
    }

    /**
     * @param ctx the request being answered
     * @return the event it names, or {@code null} after an error was already sent
     */
    private static EventData resolve(ApiContext ctx) {
        UUID id;
        try {
            id = UUID.fromString(ctx.pathParam("event"));
        } catch (IllegalArgumentException e) {
            ctx.error(400, "Das ist keine gültige Event-Id.");
            return null;
        }
        EventData event = store().getEvent(id);
        if (event == null) {
            ctx.error(404, "Dieses Event gibt es nicht.");
            return null;
        }
        return event;
    }

    /**
     * Tells the game servers about a change made on the website, so the calendar there follows along.
     *
     * @param id    the event that changed
     * @param event its new state, or {@code null} if it was deleted
     */
    private static void announce(UUID id, EventData event) {
        try {
            ListenerAdapter.sendListeners(new EventUpdatedEvent(id, event));
        } catch (Exception e) {
            System.out.println("Could not announce the event " + id + ": " + e.getMessage());
        }
    }

    private static EventStore store() {
        return Main.getInstance().getEventStore();
    }
}

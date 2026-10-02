package de.hems.utils.webconsole.modules;

import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.event.EventUpdatedEvent;
import de.hems.types.event.EventData;
import de.hems.types.event.EventType;
import de.hems.utils.event.EventForm;
import de.hems.utils.event.EventStore;
import de.hems.utils.webconsole.AdminNetwork;
import de.hems.utils.webconsole.ApiContext;
import de.hems.utils.webconsole.WebModule;
import de.hems.utils.webconsole.WebServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
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
        return "Zeigt den Eventkalender, legt Events an und bearbeitet sie - mit Einstellungen und Belohnungen.";
    }

    @Override
    public void register(WebServer server) {
        server.get("/api/events", ctx -> ctx.ok("events", listEvents()));
        server.get("/api/events/types", ctx -> ctx.ok("types", listTypes()));
        server.post("/api/events", this::create);
        server.post("/api/events/{event}", this::edit);
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
                    .put("applied", event.isApplied())
                    .put("started", System.currentTimeMillis() >= event.getStartsAt())
                    .put("ranked", event.getType().isRanked())
                    .put("countsKills", event.getType().countsKills())
                    // as a string: a long does not survive the trip through a javascript number unharmed
                    .put("revision", String.valueOf(event.getRevision()))
                    .put("fields", EventForm.describeSettings(event))
                    .put("rewards", EventForm.describeRewards(event));
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
                    .put("ranked", type.isRanked())
                    .put("countsKills", type.countsKills())
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
        long[] times = times(ctx, 0L, 0L);
        if (times == null) return;
        EventData event = new EventData(name, type, times[0], times[1]);
        event.setDescription(ctx.string("description", ""));
        // the same defaults the create panel in the game writes, so the form shows what is stored
        EventForm.applyDefaults(event);
        EventStore.Result result = store().put(event, true);
        if (!result.successful()) {
            ctx.error(409, result.message());
            return;
        }
        announce(result.event().getId(), result.event());
        ctx.ok(new JSONObject()
                .put("ok", true)
                .put("message", name + " wurde angelegt.")
                .put("id", result.event().getId().toString()));
    }

    /**
     * Changes an event: its name, description and times, its settings and its rewards, all in one save.
     * <p>
     * The form says which revision it was opened on. If the event was changed in the meantime - in the game,
     * or in another tab - the save is refused instead of quietly undoing that change.
     *
     * @param ctx the request being answered
     */
    private void edit(ApiContext ctx) {
        EventData stored = resolve(ctx);
        if (stored == null) return;
        if (stored.isApplied()) {
            ctx.error(409, stored.getName() + " ist schon abgerechnet - eine Änderung hätte keine Wirkung mehr.");
            return;
        }
        long revision;
        try {
            revision = Long.parseLong(ctx.string("revision", ""));
        } catch (NumberFormatException e) {
            ctx.error(400, "Es fehlt die Revision, auf der die Änderung beruht.");
            return;
        }
        String name = ctx.string("name", "").trim();
        if (name.isEmpty()) {
            ctx.error(400, "Es fehlt der Name des Events.");
            return;
        }
        long[] times = times(ctx, stored.getStartsAt(), stored.getEndsAt());
        if (times == null) return;
        // a game that is already being played cannot start later or earlier than it did. The end can still
        // move - that is how an event is extended, or cut short
        if (times[0] != stored.getStartsAt() && System.currentTimeMillis() >= stored.getStartsAt()) {
            ctx.error(409, "Das Event hat schon angefangen - der Anfang lässt sich nicht mehr verschieben.");
            return;
        }

        EventData edited = stored.copy();
        edited.setRevision(revision);
        edited.setName(name);
        edited.setDescription(ctx.string("description", ""));
        edited.setStartsAt(times[0]);
        edited.setEndsAt(times[1]);

        String problem = EventForm.applySettings(edited, ctx.body().optJSONObject("settings"));
        if (problem == null) {
            problem = EventForm.applyRewards(edited, ctx.body().optJSONArray("rewards"),
                    EventModule::knownMaterials);
        }
        if (problem != null) {
            ctx.error(400, problem);
            return;
        }
        EventStore.Result result = store().put(edited, false);
        if (!result.successful()) {
            ctx.error(409, result.message());
            return;
        }
        announce(result.event().getId(), result.event());
        ctx.ok(name + " wurde gespeichert.");
    }

    /**
     * Reads the start and the end of an event from a request.
     *
     * @param ctx            the request being answered
     * @param startsFallback what to use when no start was sent
     * @param endsFallback   what to use when no end was sent
     * @return start and end, or {@code null} after an error was already sent
     */
    private static long[] times(ApiContext ctx, long startsFallback, long endsFallback) {
        long startsAt;
        long endsAt;
        // the browser sends milliseconds, which does not survive an int - read them as strings instead
        try {
            startsAt = Long.parseLong(ctx.string("startsAt", String.valueOf(startsFallback)));
            endsAt = Long.parseLong(ctx.string("endsAt", String.valueOf(endsFallback)));
        } catch (NumberFormatException e) {
            ctx.error(400, "Anfang und Ende müssen Zeitstempel sein.");
            return null;
        }
        if (endsAt <= startsAt) {
            ctx.error(400, "Das Event endet vor seinem Anfang.");
            return null;
        }
        return new long[]{startsAt, endsAt};
    }

    /**
     * @return every item name, or an empty list when no game server could be asked - the name is then only
     *         checked for its form, and a game server skips an item it does not know when it hands it out
     */
    private static List<String> knownMaterials() {
        try {
            return AdminNetwork.materials();
        } catch (Exception e) {
            return List.of();
        }
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
        // through the settlement, like a delete from the game: a deleted poker night hands the chips on its
        // tables back first, and runs, results and servers go with the event
        if (!Main.getInstance().getEventSettlement().delete(event.getId())) {
            ctx.error(404, "Dieses Event gibt es nicht.");
            return;
        }
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

package de.hems.paper.event;

import de.hems.paper.NetworkSync;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.result.RequestEventResultsEvent;
import de.hems.communication.events.result.SaveEventResultsEvent;
import de.hems.paper.PaperContext;
import de.hems.types.event.EventResultData;
import org.bukkit.Bukkit;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Sends result lines to the launcher and reads them back.
 * <p>
 * The game server of an event that ranks by where people finished reports every change through here; the
 * launcher keeps the lines and pays the rewards out of them when the event is over. Nothing is cached - the
 * lines are read when somebody opens the result, which is rare enough not to matter.
 */
public final class EventResultService {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private EventResultService() {
    }

    /**
     * Sends lines off without waiting. A death in the arena must never stall on the network.
     *
     * @param rows the lines that changed
     */
    public static void report(List<EventResultData> rows) {
        if (rows.isEmpty() || !PaperContext.hasPlugin()) return;
        List<EventResultData> copy = new ArrayList<>(rows);
        PaperContext.async(() -> {
            try {
                if (!ListenerAdapter.isInitialized()) return;
                ListenerAdapter.sendListeners(new SaveEventResultsEvent(copy));
            } catch (Exception e) {
                Bukkit.getLogger().warning("Could not report " + copy.size() + " result lines: " + e.getMessage());
            }
        });
    }

    /**
     * Tells the launcher the game of an event is over, with its last lines. Until this arrives - or the
     * game server is gone - the launcher does not settle the event, however late it gets: an event ends
     * when its game does, not when its clock runs out.
     *
     * @param eventId the event
     * @param rows    the last lines, may be empty
     */
    public static void finish(UUID eventId, List<EventResultData> rows) {
        if (eventId == null || !PaperContext.hasPlugin()) return;
        List<EventResultData> copy = new ArrayList<>(rows);
        PaperContext.async(() -> {
            try {
                if (!ListenerAdapter.isInitialized()) return;
                ListenerAdapter.sendListeners(new SaveEventResultsEvent(copy, eventId));
            } catch (Exception e) {
                Bukkit.getLogger().warning("Could not report the end of the game: " + e.getMessage()
                        + " - the event is settled once this server is gone.");
            }
        });
    }

    /**
     * Reads the lines of one event and hands them over on the main thread.
     *
     * @param eventId the event
     * @param then    what to do with them, empty if they could not be read
     */
    public static void fetchAsync(UUID eventId, Consumer<List<EventResultData>> then) {
        PaperContext.async(() -> {
            List<EventResultData> rows = fetchBlocking(eventId);
            PaperContext.sync(() -> then.accept(rows));
        });
    }

    /**
     * @param eventId the event
     * @return its lines, empty if the launcher did not answer. Blocks.
     */
    public static List<EventResultData> fetchBlocking(UUID eventId) {
        RequestEventResultsEvent request = new RequestEventResultsEvent(eventId);
        List<EventResultData> list = NetworkSync.fetchList(request, TIMEOUT, EventResultData.class);
        if (list == null) return List.of();
        return list;
    }
}

package de.hems.event;

import de.hems.api.ServerApi;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.calendar.CancelEventRequest;
import de.hems.communication.events.calendar.EventScoreRequest;
import de.hems.communication.events.calendar.JoinEventTeamRequest;
import de.hems.communication.events.calendar.ScheduleEventRequest;
import de.hems.types.FileType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The programmatic side of the event system.
 * <p>
 * Everything the calendar UI does is available here as well, so events can be planned automatically:
 * <pre>{@code
 * // a bedwars round next saturday
 * ScheduledEvent event = EventApi.create("BEDWARS", "Sommer Cup");
 * event.getDays().add(LocalDate.now().with(DayOfWeek.SATURDAY));
 * event.setTeamCount(4);
 * EventApi.schedule(event);
 *
 * // the plugin of the event feeds its results back into the ranking
 * EventApi.addScore(event.getId(), team.getId(), 5);
 * EventApi.getRanking(event.getId());
 * }</pre>
 * A new kind of event is a new {@link EventDefinition} handed to
 * {@link EventRegistry#register(EventDefinition)}; nothing here has to change for it.
 */
public final class EventApi {

    private EventApi() {
    }

    /**
     * Builds a new event of the given kind. It is not in the calendar until {@link #schedule(ScheduledEvent)}
     * is called.
     *
     * @param definitionId the kind of event, e.g. {@code "BEDWARS"}
     * @param name         the name of the event
     * @return the new event, already filled with the defaults of its kind
     */
    public static ScheduledEvent create(String definitionId, String name) {
        return EventRegistry.get(definitionId).createEvent(name);
    }

    /**
     * Builds a new event and plans it on the given days in one go.
     *
     * @param definitionId the kind of event
     * @param name         the name of the event
     * @param days         the days it takes place on
     * @return the event that was scheduled
     */
    public static ScheduledEvent createAndSchedule(String definitionId, String name, LocalDate... days) throws Exception {
        ScheduledEvent event = create(definitionId, name);
        event.setDays(List.of(days));
        schedule(event);
        return event;
    }

    /**
     * Stores an event in the calendar. An event that is already there is replaced, so this also saves
     * changes.
     *
     * @param event the event to store
     */
    public static void schedule(ScheduledEvent event) throws Exception {
        if (event.getDays().isEmpty()) {
            throw new IllegalArgumentException("An event needs at least one day");
        }
        ListenerAdapter.sendListeners(new ScheduleEventRequest(ListenerAdapter.ServerName.HOST, event));
    }

    /**
     * Takes an event out of the calendar.
     *
     * @param eventId the id of the event
     */
    public static void cancel(UUID eventId) throws Exception {
        ListenerAdapter.sendListeners(new CancelEventRequest(ListenerAdapter.ServerName.HOST, eventId));
    }

    /**
     * Puts a player into a team of an event.
     *
     * @param eventId the event
     * @param teamId  the team, {@code null} to leave the event
     * @param player  the player
     */
    public static void join(UUID eventId, UUID teamId, UUID player) throws Exception {
        ListenerAdapter.sendListeners(new JoinEventTeamRequest(ListenerAdapter.ServerName.HOST, eventId, teamId, player));
    }

    /**
     * Takes a player out of every team of an event.
     *
     * @param eventId the event
     * @param player  the player
     */
    public static void leave(UUID eventId, UUID player) throws Exception {
        join(eventId, null, player);
    }

    /**
     * Adds points to a team, the usual way for an event plugin to report results.
     *
     * @param eventId the event
     * @param teamId  the team
     * @param points  the points to add, may be negative
     */
    public static void addScore(UUID eventId, UUID teamId, double points) throws Exception {
        ListenerAdapter.sendListeners(new EventScoreRequest(ListenerAdapter.ServerName.HOST, eventId, teamId, points, true));
    }

    /**
     * Sets the score of a team, e.g. the finishing time of a speedrun.
     *
     * @param eventId the event
     * @param teamId  the team
     * @param score   the new score
     */
    public static void setScore(UUID eventId, UUID teamId, double score) throws Exception {
        ListenerAdapter.sendListeners(new EventScoreRequest(ListenerAdapter.ServerName.HOST, eventId, teamId, score, false));
    }

    /**
     * @return every event of the calendar as this server knows it
     */
    public static List<ScheduledEvent> getEvents() {
        return EventCalendar.getEvents();
    }

    /**
     * @param day the day to look at
     * @return every event of that day
     */
    public static List<ScheduledEvent> getEventsOn(LocalDate day) {
        return EventCalendar.getEventsOn(day);
    }

    /**
     * @return every event that takes place today
     */
    public static List<ScheduledEvent> getEventsToday() {
        return EventCalendar.getEventsOn(LocalDate.now());
    }

    /**
     * @param eventId the id of an event
     * @return the event, or {@code null} if the calendar does not know it
     */
    public static ScheduledEvent getEvent(UUID eventId) {
        return EventCalendar.getEvent(eventId);
    }

    /**
     * @param eventId the id of an event
     * @return the leaderboard of that event, or {@code null} if it is unknown
     */
    public static de.hems.event.ranking.Ranking getRanking(UUID eventId) {
        ScheduledEvent event = getEvent(eventId);
        return event == null ? null : event.getRanking();
    }

    /**
     * Fetches the calendar from the host. Blocks, so it belongs off the main thread.
     *
     * @return every event of the calendar
     */
    public static List<ScheduledEvent> refresh() throws Exception {
        return EventCalendar.refresh();
    }

    /**
     * Fetches the calendar from the host without blocking the caller.
     *
     * @return a future that completes with every event of the calendar
     */
    public static CompletableFuture<List<ScheduledEvent>> refreshAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return EventCalendar.refresh();
            } catch (Exception e) {
                throw new IllegalStateException("Could not load the event calendar", e);
            }
        });
    }

    /**
     * Starts the server of an event right away, with the plugin of the event installed.
     *
     * @param event the event whose server should run
     * @return the name of the server that was started
     */
    public static ListenerAdapter.ServerName startServer(ScheduledEvent event) throws Exception {
        List<FileType.PLUGIN> plugins = new ArrayList<>(event.getPlugins());
        for (FileType.PLUGIN plugin : event.getDefinition().getAllPlugins()) {
            if (!plugins.contains(plugin)) plugins.add(plugin);
        }
        return ServerApi.createServer(event.getServerName(), event.getTemplate(), event.getMemoryMB(), plugins);
    }
}

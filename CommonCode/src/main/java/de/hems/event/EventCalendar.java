package de.hems.event;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.calendar.EventCalendarUpdatedEvent;
import de.hems.communication.events.calendar.RequestEventCalendarEvent;
import de.hems.communication.events.calendar.RespondEventCalendarEvent;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventHandler;
import de.hems.communication.events.types.RespondDataEvent;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The event calendar as every server sees it.
 * <p>
 * The host keeps the real list; this is the copy that is kept in sync. It is refreshed when the server
 * starts, whenever the host announces a change and whenever somebody opens the calendar, so all servers
 * show the same events.
 */
public final class EventCalendar {

    /** How many days the calendar shows. */
    public static final int DAYS = 27;

    private static final List<ScheduledEvent> events = new CopyOnWriteArrayList<>();
    private static final List<Runnable> updateListeners = new CopyOnWriteArrayList<>();
    private static boolean listening = false;

    private EventCalendar() {
    }

    /**
     * Starts listening for calendar changes and asks the host for the current state. Call this once when
     * the plugin starts.
     */
    public static synchronized void init() {
        if (listening) return;
        listening = true;
        ListenerAdapter.register(EventCalendarUpdatedEvent.class, new EventHandler<EventCalendarUpdatedEvent>() {
            @Override
            public void onEvent(Event event) {
                if (!(event instanceof EventCalendarUpdatedEvent updated)) return;
                update(List.of(updated.getEvents()));
            }
        });
        // fetch the calendar once in the background so it is there before anybody opens it
        Thread loader = new Thread(() -> {
            try {
                Thread.sleep(3000); // give the node a moment to find the rest of the cluster
                refresh();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.out.println("[EventCalendar] Could not load the calendar yet: " + e.getMessage());
            }
        }, "event-calendar-loader");
        loader.setDaemon(true);
        loader.start();
    }

    /**
     * Replaces the local copy with what the host sent.
     *
     * @param updated every event the host knows
     */
    public static void update(List<ScheduledEvent> updated) {
        events.clear();
        if (updated != null) events.addAll(updated);
        for (Runnable listener : updateListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Runs something whenever the calendar changed, e.g. to refresh an open UI.
     *
     * @param listener what to run
     */
    public static void onUpdate(Runnable listener) {
        updateListeners.add(listener);
    }

    /**
     * Asks the host for the calendar and waits for the answer. Blocks, so it belongs off the main thread.
     *
     * @return every event of the calendar
     */
    public static List<ScheduledEvent> refresh() throws Exception {
        RequestEventCalendarEvent request = new RequestEventCalendarEvent(ListenerAdapter.ServerName.HOST);
        ListenerAdapter.sendListeners(request);
        RespondDataEvent response = ListenerAdapter.waitForEvent(request.getEventId(), de.hems.api.ServerApi.TIMEOUT);
        if (!(response instanceof RespondEventCalendarEvent calendar)) return getEvents();
        update(List.of(calendar.getData()));
        return getEvents();
    }

    /**
     * @return every event the calendar knows, sorted by their next day
     */
    public static List<ScheduledEvent> getEvents() {
        List<ScheduledEvent> copy = new ArrayList<>(events);
        copy.sort((a, b) -> {
            LocalDate dayA = a.getNextDay();
            LocalDate dayB = b.getNextDay();
            if (dayA == null && dayB == null) return a.getName().compareToIgnoreCase(b.getName());
            if (dayA == null) return 1;
            if (dayB == null) return -1;
            int compared = dayA.compareTo(dayB);
            return compared != 0 ? compared : a.getName().compareToIgnoreCase(b.getName());
        });
        return copy;
    }

    /**
     * @param day the day to look at
     * @return every event that takes place on that day
     */
    public static List<ScheduledEvent> getEventsOn(LocalDate day) {
        List<ScheduledEvent> onDay = new ArrayList<>();
        for (ScheduledEvent event : events) {
            if (event.isOn(day)) onDay.add(event);
        }
        onDay.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return onDay;
    }

    /**
     * @param id the id of an event
     * @return the event, or {@code null} if the calendar does not know it
     */
    public static ScheduledEvent getEvent(UUID id) {
        for (ScheduledEvent event : events) {
            if (event.getId().equals(id)) return event;
        }
        return null;
    }

    /**
     * @return the days the calendar shows, starting today
     */
    public static List<LocalDate> getWindow() {
        return getWindow(LocalDate.now());
    }

    /**
     * @param start the first day
     * @return {@link #DAYS} days starting at that day
     */
    public static List<LocalDate> getWindow(LocalDate start) {
        List<LocalDate> window = new ArrayList<>(DAYS);
        for (int i = 0; i < DAYS; i++) window.add(start.plusDays(i));
        return Collections.unmodifiableList(window);
    }

    /**
     * @param day a day
     * @return whether that day is inside the shown month
     */
    public static boolean isInWindow(LocalDate day) {
        LocalDate today = LocalDate.now();
        return !day.isBefore(today) && day.isBefore(today.plusDays(DAYS));
    }
}

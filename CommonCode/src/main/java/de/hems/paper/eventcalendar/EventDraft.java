package de.hems.paper.eventcalendar;

import de.hems.event.EventDefinition;
import de.hems.event.ScheduledEvent;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The event an admin is currently putting together. It wraps the {@link ScheduledEvent} itself, so
 * everything that can be configured on an event can be configured while creating it - and editing an event
 * that already exists works with exactly the same screens.
 */
public class EventDraft {

    private static final Map<UUID, EventDraft> drafts = new ConcurrentHashMap<>();

    private final ScheduledEvent event;
    private final boolean editing;

    private EventDraft(ScheduledEvent event, boolean editing) {
        this.event = event;
        this.editing = editing;
    }

    /**
     * Starts a new event of the given kind.
     *
     * @param player     the admin
     * @param definition the kind of event
     * @param name       the suggested name
     * @return the draft
     */
    public static EventDraft start(Player player, EventDefinition definition, String name) {
        ScheduledEvent event = definition.createEvent(name);
        event.setCreatedBy(player.getUniqueId(), player.getName());
        EventDraft draft = new EventDraft(event, false);
        drafts.put(player.getUniqueId(), draft);
        return draft;
    }

    /**
     * Opens an event that is already in the calendar for editing. The draft works on a copy, so nothing
     * changes until it is saved.
     *
     * @param player the admin
     * @param event  the event to edit
     * @return the draft
     */
    public static EventDraft edit(Player player, ScheduledEvent event) {
        EventDraft draft = new EventDraft(event.copy(), true);
        drafts.put(player.getUniqueId(), draft);
        return draft;
    }

    /**
     * @param player the admin
     * @return the event they are working on, or {@code null} if there is none
     */
    public static EventDraft of(Player player) {
        return drafts.get(player.getUniqueId());
    }

    public static void clear(Player player) {
        drafts.remove(player.getUniqueId());
    }

    public ScheduledEvent getEvent() {
        return event;
    }

    /**
     * @return whether this draft changes an event that is already in the calendar
     */
    public boolean isEditing() {
        return editing;
    }

    /**
     * Adds or removes a day of the event - what a click on a day in the planning view does.
     *
     * @param day the day that was clicked
     * @return whether the event takes place on that day now
     */
    public boolean toggleDay(LocalDate day) {
        return event.toggleDay(day);
    }

    /**
     * @return whether the event can be saved, which needs at least one day
     */
    public boolean isComplete() {
        return !event.getDays().isEmpty() && event.getName() != null && !event.getName().isBlank();
    }
}

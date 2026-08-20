package de.schnorrenbergers.bedwars.api;

import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.timeline.TimelineEvent;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Something on the round's timeline has happened.
 * <p>
 * Fired for every entry of {@code timeline.yml}, including the ones whose action is {@code ANNOUNCE} and
 * which therefore do nothing by themselves. That is what makes the timeline usable by an addon: a server
 * can write an entry with a name and a time, and an addon can act on it without either of them knowing
 * about the other.
 */
public class BedwarsTimelineEvent extends BedwarsEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final TimelineEvent event;

    /**
     * @param game  the round
     * @param event what happened
     */
    public BedwarsTimelineEvent(Game game, TimelineEvent event) {
        super(game);
        this.event = event;
    }

    public TimelineEvent getEvent() {
        return event;
    }

    /**
     * @return the id it has in {@code timeline.yml}
     */
    public String getId() {
        return event.id();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

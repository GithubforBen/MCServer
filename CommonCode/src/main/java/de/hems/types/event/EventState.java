package de.hems.types.event;

import java.io.Serializable;

/**
 * Where an event stands right now. Worked out from its time frame, except for {@link #CANCELLED}, which an
 * admin sets by hand.
 */
public enum EventState implements Serializable {

    /** Announced, but its start has not come yet. */
    PLANNED("Geplant"),
    /** Between start and end. */
    RUNNING("Läuft"),
    /** Its end has passed. */
    FINISHED("Vorbei"),
    /** Called off before it ended. */
    CANCELLED("Abgesagt");

    private final String title;

    EventState(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}

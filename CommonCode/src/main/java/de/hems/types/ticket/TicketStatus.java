package de.hems.types.ticket;

import java.io.Serializable;

/** Where a ticket stands. */
public enum TicketStatus implements Serializable {

    OPEN("Offen"),
    IN_PROGRESS("In Bearbeitung"),
    CLOSED("Geschlossen");

    private final String title;

    TicketStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    /**
     * @param name a status name, including the ones of the old ticket system
     * @return the status, {@link #OPEN} for anything unknown
     */
    public static TicketStatus byName(String name) {
        if (name == null) return OPEN;
        for (TicketStatus status : values()) {
            if (status.name().equalsIgnoreCase(name)) return status;
        }
        return name.equalsIgnoreCase("InProgress") ? IN_PROGRESS : OPEN;
    }
}

package de.hems.types.ticket;

import java.io.Serializable;

/** What a ticket is about. */
public enum TicketType implements Serializable {

    BUG("Bug"),
    RULES("Regelverstoß"),
    SUGGESTION("Vorschlag"),
    QUESTION("Frage"),
    /** Asked from the admin abuse log: a question about why an admin did something. */
    ADMIN_ACTION("Frage zu einer Admin-Aktion"),
    OTHER("Sonstiges");

    private final String title;

    TicketType(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    /**
     * @param name a type name, including the ones of the old ticket system
     * @return the type, {@link #OTHER} for anything unknown
     */
    public static TicketType byName(String name) {
        if (name == null) return OTHER;
        return switch (name.toUpperCase()) {
            case "REGELN" -> RULES;
            case "NACHFRAGE" -> QUESTION;
            default -> {
                for (TicketType type : values()) {
                    if (type.name().equalsIgnoreCase(name)) yield type;
                }
                yield OTHER;
            }
        };
    }
}

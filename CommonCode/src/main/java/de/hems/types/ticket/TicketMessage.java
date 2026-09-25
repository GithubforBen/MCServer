package de.hems.types.ticket;

import java.io.Serializable;

/**
 * One message in a ticket: the first one is what the ticket is about, the rest is the conversation.
 */
public class TicketMessage implements Serializable {

    private static final long serialVersionUID = 4501L;

    private String author;
    /** Whether an admin wrote it - which decides whose side of the conversation it is on. */
    private boolean staff;
    private TicketSource source;
    private String text;
    private long at;

    public TicketMessage() {
    }

    public TicketMessage(String author, boolean staff, TicketSource source, String text, long at) {
        this.author = author;
        this.staff = staff;
        this.source = source;
        this.text = text;
        this.at = at;
    }

    public String getAuthor() {
        return author;
    }

    public boolean isStaff() {
        return staff;
    }

    public TicketSource getSource() {
        return source;
    }

    public String getText() {
        return text;
    }

    public long getAt() {
        return at;
    }
}

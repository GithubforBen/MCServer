package de.hems.communication.events.ticket;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.types.ticket.TicketData;

import java.io.Serializable;

/**
 * Tells every server that a ticket changed, so the author hears about an answer and the admins about a new
 * ticket or a reply - whoever of them is online, wherever.
 */
public class TicketUpdatedEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4504L;

    /** What happened, for the message. */
    public enum Change {
        CREATED, STAFF_REPLY, PLAYER_REPLY, STATUS, CLAIMED
    }

    private TicketData ticket;
    private Change change;
    /** Whether an admin made the change, so the author is not told what they did themselves. */
    private boolean byStaff;

    public TicketUpdatedEvent(TicketData ticket, Change change, boolean byStaff) {
        super(ListenerAdapter.ServerName.ALL);
        this.ticket = ticket;
        this.change = change;
        this.byStaff = byStaff;
    }

    public TicketUpdatedEvent() {
    }

    public TicketData getTicket() {
        return ticket;
    }

    public Change getChange() {
        return change;
    }

    public boolean isByStaff() {
        return byStaff;
    }
}

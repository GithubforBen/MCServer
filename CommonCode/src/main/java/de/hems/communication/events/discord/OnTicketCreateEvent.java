package de.hems.communication.events.discord;

import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;

public class OnTicketCreateEvent extends EventFoundationData implements Event, Serializable {
    private static final long serialVersionUID = 4265478L;
    private String ticketId;
}

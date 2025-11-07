package de.hems.communication.events.types;

import de.hems.communication.ListenerAdapter;

import java.util.UUID;

public class EventFoundationData implements Event {
    private ListenerAdapter.ServerName sender;
    private ListenerAdapter.ServerName receiver;
    private UUID eventId;

    public EventFoundationData(ListenerAdapter.ServerName receiver) {
        this.sender = ListenerAdapter.getName();
        this.receiver = receiver;
        this.eventId = UUID.randomUUID();
    }

    public EventFoundationData() {}

    public ListenerAdapter.ServerName getSender() {
        return sender;
    }

    public ListenerAdapter.ServerName getReceiver() {
        return receiver;
    }

    public UUID getEventId() {
        return eventId;
    }
}

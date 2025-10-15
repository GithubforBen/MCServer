package de.hems.communication.events;

import java.util.UUID;

public class EventFoundationData {
    private final String sender;
    private final String receiver;
    private final UUID eventId;

    public EventFoundationData(String sender, String receiver, UUID eventId) {
        this.sender = sender;
        this.receiver = receiver;
        this.eventId = eventId;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public UUID getEventId() {
        return eventId;
    }
}

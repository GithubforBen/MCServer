package de.hems.communication.events.types;

import java.util.UUID;

public class EventFoundationData {
    private String sender;
    private String receiver;
    private UUID eventId;

    public EventFoundationData(String sender, String receiver, UUID eventId) {
        this.sender = sender;
        this.receiver = receiver;
        this.eventId = eventId;
    }

    public EventFoundationData() {}

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

package de.hems.communication.events.types;

public interface EventHandler<T extends Event> {
    void onEvent(Event event);
}

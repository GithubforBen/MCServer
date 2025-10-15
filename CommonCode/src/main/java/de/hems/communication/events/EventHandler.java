package de.hems.communication.events;

public interface EventHandler<T extends Event> {
    void onEvent(Event event);
}

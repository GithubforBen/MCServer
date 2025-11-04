package de.hems.communication.events.types;

import java.io.IOException;

public interface EventHandler<T extends Event> {
    void onEvent(Event event) throws Exception;
}

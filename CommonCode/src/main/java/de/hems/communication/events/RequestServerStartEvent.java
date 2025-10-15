package de.hems.communication.events;

import de.hems.communication.Event;

public class RequestServerStartEvent implements Event {
    public String serverName;
    public double ram;
}

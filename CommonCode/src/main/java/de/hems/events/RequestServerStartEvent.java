package de.hems.events;

import de.hems.Event;

public class RequestServerStartEvent implements Event {
    public String serverName;
    public double ram;
}

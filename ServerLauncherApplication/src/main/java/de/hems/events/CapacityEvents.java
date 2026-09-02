package de.hems.events;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.server.RequestCapacityEvent;
import de.hems.communication.events.server.RequestServerSlotEvent;
import de.hems.communication.events.server.RespondCapacityEvent;
import de.hems.communication.events.server.RespondServerSlotEvent;
import de.hems.types.server.CapacityData;
import de.hems.utils.server.MemoryWatch;

/**
 * Answers the two questions the rest of the network has about the machine: how full is it, and may I start
 * one more server on it.
 * <p>
 * The second one is asked before anything is created, and it is answered here rather than by the asking
 * server, because two players pressing "start a round" in the same second see the same free memory and
 * would both start. Here they arrive one after the other.
 */
public class CapacityEvents {

    private final MemoryWatch memory;

    public CapacityEvents(MemoryWatch memory) {
        this.memory = memory;
        ListenerAdapter.register(RequestCapacityEvent.class, event -> onCapacity((RequestCapacityEvent) event));
        ListenerAdapter.register(RequestServerSlotEvent.class, event -> onSlot((RequestServerSlotEvent) event));
    }

    private void onCapacity(RequestCapacityEvent request) throws Exception {
        ListenerAdapter.sendListeners(new RespondCapacityEvent(
                request.getSender(), memory.snapshot(), request.getEventId()));
    }

    private synchronized void onSlot(RequestServerSlotEvent request) throws Exception {
        boolean granted = memory.fits(request.getMemoryMB());
        if (!granted) {
            memory.recordRefusal(request.getMemoryMB(), request.getPlayerName(), request.getPurpose());
        }
        CapacityData capacity = memory.snapshot();
        ListenerAdapter.sendListeners(new RespondServerSlotEvent(
                request.getSender(), granted, capacity, request.getEventId()));
    }
}

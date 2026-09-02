package de.hems.events;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.server.RequestCapacityEvent;
import de.hems.communication.events.server.RequestServerSlotEvent;
import de.hems.communication.events.server.RespondCapacityEvent;
import de.hems.communication.events.server.RespondServerMemoryEvent;
import de.hems.communication.events.server.RespondServerSlotEvent;
import de.hems.communication.events.server.SetServerMemoryEvent;
import de.hems.types.server.CapacityData;
import de.hems.Main;
import de.hems.utils.server.MemoryLimits;
import de.hems.utils.server.MemoryWatch;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Answers the two questions the rest of the network has about the machine: how full is it, and may I start
 * one more server on it.
 * <p>
 * The second one is asked before anything is created, and it is answered here rather than by the asking
 * server, because two players pressing "start a round" in the same second see the same free memory and
 * would both start. Here they arrive one after the other.
 */
public class CapacityEvents {

    /** The largest heap the manager will write down for one server. */
    private static final int MAX_MEMORY_MB = 65536;

    private final MemoryWatch memory;

    public CapacityEvents(MemoryWatch memory) {
        this.memory = memory;
        ListenerAdapter.register(RequestCapacityEvent.class, event -> onCapacity((RequestCapacityEvent) event));
        ListenerAdapter.register(RequestServerSlotEvent.class, event -> onSlot((RequestServerSlotEvent) event));
        ListenerAdapter.register(SetServerMemoryEvent.class, event -> onSetMemory((SetServerMemoryEvent) event));
    }

    /**
     * Writes down what a server gets the next time it starts.
     * <p>
     * The number goes into the same key the launcher reads when it brings the autostart servers up, so a
     * recommendation acted on here survives a restart of the whole network rather than only of one server.
     */
    private void onSetMemory(SetServerMemoryEvent request) throws Exception {
        String name = request.getServerName();
        int wanted = request.getMemoryMB();
        if (name == null || name.isBlank()) {
            respond(request, false, "Kein Server angegeben.", 0);
            return;
        }
        if (wanted < MemoryLimits.FLOOR_MB || wanted > MAX_MEMORY_MB) {
            respond(request, false, "Zwischen " + MemoryLimits.FLOOR_MB + " und " + MAX_MEMORY_MB
                    + " MB, sonst kommt der Server gar nicht erst hoch.", 0);
            return;
        }
        ListenerAdapter.ServerName server = ListenerAdapter.ServerName.valueOf(name);
        YamlConfiguration config = Main.getInstance().getConfiguration().getConfig();
        config.set("servers." + server + ".memory", wanted);
        Main.getInstance().getConfiguration().save();
        respond(request, true, null, wanted);
        System.out.println("Server " + server + " will start with " + wanted + " MB from now on.");
    }

    private void respond(SetServerMemoryEvent request, boolean successful, String message, int memoryMB)
            throws Exception {
        ListenerAdapter.sendListeners(new RespondServerMemoryEvent(
                request.getSender(), successful, message, memoryMB, request.getEventId()));
    }

    private void onCapacity(RequestCapacityEvent request) throws Exception {
        ListenerAdapter.sendListeners(new RespondCapacityEvent(
                request.getSender(), memory.snapshot(), request.getEventId()));
    }

    private synchronized void onSlot(RequestServerSlotEvent request) throws Exception {
        boolean granted = memory.fits(request.getMemoryMB());
        if (granted) {
            // held until the server actually exists, so the next request in the same second sees it
            memory.hold(request.getMemoryMB());
        } else {
            memory.recordRefusal(request.getMemoryMB(), request.getPlayerName(), request.getPurpose());
        }
        CapacityData capacity = memory.snapshot();
        ListenerAdapter.sendListeners(new RespondServerSlotEvent(
                request.getSender(), granted, capacity, request.getEventId()));
    }
}

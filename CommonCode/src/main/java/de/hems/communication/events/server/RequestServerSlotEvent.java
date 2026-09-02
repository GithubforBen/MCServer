package de.hems.communication.events.server;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.UUID;

/**
 * Asks whether a server of this size may still be started, before anything is created.
 * <p>
 * Deliberately a question to the launcher rather than a sum the asking server works out for itself. Two
 * players pressing "start a round" in the same second both see the same free memory and both start; the
 * launcher sees them one after another. It is also the place that counts the refusals, and a count kept in
 * one place is a count that means something.
 */
public class RequestServerSlotEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4605L;

    private int memoryMB;
    private UUID playerId;
    private String playerName;
    private String purpose;

    public RequestServerSlotEvent() {
    }

    /**
     * @param memoryMB   the heap the new server would want
     * @param playerId   who is asking, may be {@code null} for an automatic start
     * @param playerName their name, for the log
     * @param purpose    what the server is for, e.g. {@code "bedwars"}
     */
    public RequestServerSlotEvent(int memoryMB, UUID playerId, String playerName, String purpose) {
        super(ListenerAdapter.ServerName.HOST);
        this.memoryMB = memoryMB;
        this.playerId = playerId;
        this.playerName = playerName;
        this.purpose = purpose;
    }

    public int getMemoryMB() {
        return memoryMB;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getPurpose() {
        return purpose;
    }
}

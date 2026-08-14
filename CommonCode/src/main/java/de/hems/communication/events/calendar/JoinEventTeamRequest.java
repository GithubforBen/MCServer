package de.hems.communication.events.calendar;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.UUID;

/**
 * Asks the host to put a player into a team of an event, or to take them out of it.
 */
public class JoinEventTeamRequest extends EventFoundationData implements Event, Serializable {
    private static final long serialVersionUID = 325L;

    private UUID eventId;
    private UUID teamId;
    private UUID player;

    public JoinEventTeamRequest() {
    }

    /**
     * @param receiver the host
     * @param eventId  the event the player signs up for
     * @param teamId   the team to join, {@code null} to leave the event
     * @param player   the player
     */
    public JoinEventTeamRequest(ListenerAdapter.ServerName receiver, UUID eventId, UUID teamId, UUID player) {
        super(receiver);
        this.eventId = eventId;
        this.teamId = teamId;
        this.player = player;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public UUID getPlayer() {
        return player;
    }
}

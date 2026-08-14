package de.hems.communication.events.calendar;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.UUID;

/**
 * Asks the host to change the score of a team. This is how the plugin of an event feeds its results into
 * the ranking, from whatever server it runs on.
 */
public class EventScoreRequest extends EventFoundationData implements Event, Serializable {
    private static final long serialVersionUID = 326L;

    private UUID eventId;
    private UUID teamId;
    private double score;
    private boolean relative;

    public EventScoreRequest() {
    }

    /**
     * @param receiver the host
     * @param eventId  the event
     * @param teamId   the team whose score changes
     * @param score    the points
     * @param relative whether the points are added to the score or replace it
     */
    public EventScoreRequest(ListenerAdapter.ServerName receiver, UUID eventId, UUID teamId, double score, boolean relative) {
        super(receiver);
        this.eventId = eventId;
        this.teamId = teamId;
        this.score = score;
        this.relative = relative;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public double getScore() {
        return score;
    }

    public boolean isRelative() {
        return relative;
    }
}

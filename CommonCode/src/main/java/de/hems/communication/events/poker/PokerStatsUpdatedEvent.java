package de.hems.communication.events.poker;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.types.poker.PokerStatsData;

import java.io.Serializable;
import java.util.UUID;

/**
 * Announces that a poker row changed, so the ranking board in the lobby follows the table while the night
 * is still being played.
 */
public class PokerStatsUpdatedEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4333L;

    private UUID eventId;
    private UUID playerId;
    /** The new state, or {@code null} when the row was removed. */
    private PokerStatsData stats;

    public PokerStatsUpdatedEvent(UUID eventId, UUID playerId, PokerStatsData stats) {
        super(ListenerAdapter.ServerName.ALL);
        this.eventId = eventId;
        this.playerId = playerId;
        this.stats = stats;
    }

    public PokerStatsUpdatedEvent() {
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public PokerStatsData getStats() {
        return stats;
    }

    public boolean isDeleted() {
        return stats == null;
    }
}

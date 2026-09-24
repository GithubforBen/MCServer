package de.hems.types.event;

import java.io.Serializable;
import java.util.UUID;

/**
 * One player's line in the result of an event that ranks people by where they finished, not by a time or
 * a score: hunger games, and whatever comes after it that works the same way.
 * <p>
 * Kept on the launcher, written by the game server whenever the line changes - a kill, a death, the end.
 * That is what lets the rewards be settled when the event's clock runs out, which is almost never a moment
 * the game server is still up: by then it has usually been switched off for a while.
 * <p>
 * Exactly one server writes the lines of one event, so a line is sent whole and simply replaces the one
 * before it.
 */
public class EventResultData implements Serializable {

    private static final long serialVersionUID = 4343L;

    private UUID eventId;
    private UUID playerId;
    private String playerName;
    /** Where they finished, one being the winner, {@link EventStanding#UNRANKED} while still in the game. */
    private int place;
    private int kills;
    private long updatedAt;

    public EventResultData() {
    }

    public EventResultData(UUID eventId, UUID playerId, String playerName) {
        this.eventId = eventId;
        this.playerId = playerId;
        this.playerName = playerName;
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * @return the line as rewards read it
     */
    public EventStanding toStanding() {
        return new EventStanding(playerId, place, kills);
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getPlace() {
        return place;
    }

    public void setPlace(int place) {
        this.place = Math.max(EventStanding.UNRANKED, place);
        this.updatedAt = System.currentTimeMillis();
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = Math.max(0, kills);
        this.updatedAt = System.currentTimeMillis();
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}

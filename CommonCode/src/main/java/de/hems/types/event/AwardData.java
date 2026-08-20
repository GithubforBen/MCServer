package de.hems.types.event;

import java.io.Serializable;
import java.util.UUID;

/**
 * A prize somebody has coming to them.
 * <p>
 * An event runs for days and ends whenever its clock says so, which is almost never a moment when the
 * winners happen to be online. So prizes are not handed out, they are put aside here and collected the
 * next time the player joins.
 */
public class AwardData implements Serializable {

    private static final long serialVersionUID = 4321L;

    /** The place used for everybody who simply took part. */
    public static final int PARTICIPATION = 0;

    private UUID id;
    private UUID player;
    private UUID eventId;
    /** Kept as text, because the event itself may be long gone by the time this is collected. */
    private String eventName;
    private int place;
    private PrizeData prize;
    private long awardedAt;
    private boolean claimed;

    public AwardData() {
    }

    /**
     * @param player    who earned it
     * @param event     the event it came from
     * @param place     the placing, or {@link #PARTICIPATION}
     * @param prize     what they get
     */
    public AwardData(UUID player, EventData event, int place, PrizeData prize) {
        this.id = UUID.randomUUID();
        this.player = player;
        this.eventId = event.getId();
        this.eventName = event.getName();
        this.place = place;
        this.prize = prize;
        this.awardedAt = System.currentTimeMillis();
    }

    /**
     * @return how the placing reads, for a message
     */
    public String getPlaceTitle() {
        return switch (place) {
            case PARTICIPATION -> "Teilnahme";
            case 1 -> "1. Platz";
            case 2 -> "2. Platz";
            case 3 -> "3. Platz";
            default -> place + ". Platz";
        };
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPlayer() {
        return player;
    }

    public void setPlayer(UUID player) {
        this.player = player;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public int getPlace() {
        return place;
    }

    public void setPlace(int place) {
        this.place = place;
    }

    public PrizeData getPrize() {
        return prize == null ? new PrizeData() : prize;
    }

    public void setPrize(PrizeData prize) {
        this.prize = prize;
    }

    public long getAwardedAt() {
        return awardedAt;
    }

    public void setAwardedAt(long awardedAt) {
        this.awardedAt = awardedAt;
    }

    public boolean isClaimed() {
        return claimed;
    }

    public void setClaimed(boolean claimed) {
        this.claimed = claimed;
    }
}

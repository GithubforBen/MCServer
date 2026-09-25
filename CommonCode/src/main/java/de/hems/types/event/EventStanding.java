package de.hems.types.event;

import java.io.Serializable;
import java.util.UUID;

/**
 * How one player did at one event, in the only terms rewards are handed out in: a placing and a number of
 * kills.
 * <p>
 * Every kind of event keeps its own record - runs with times, poker rows with profits, a hunger games
 * result with the order people fell in - and turns it into these at the end. That translation is the only
 * thing a new kind of event has to write to get rewards; {@link EventRewards} does the rest the same way for
 * all of them.
 */
public class EventStanding implements Serializable {

    private static final long serialVersionUID = 4341L;

    /** The placing of somebody who took part but was not ranked. */
    public static final int UNRANKED = 0;

    private final UUID player;
    private final int place;
    private final int kills;

    /**
     * @param player who
     * @param place  their placing, one being the best, or {@link #UNRANKED}
     * @param kills  how many they took out, zero on events that do not count them
     */
    public EventStanding(UUID player, int place, int kills) {
        this.player = player;
        this.place = Math.max(UNRANKED, place);
        this.kills = Math.max(0, kills);
    }

    public UUID getPlayer() {
        return player;
    }

    public int getPlace() {
        return place;
    }

    public boolean isRanked() {
        return place > UNRANKED;
    }

    public int getKills() {
        return kills;
    }
}

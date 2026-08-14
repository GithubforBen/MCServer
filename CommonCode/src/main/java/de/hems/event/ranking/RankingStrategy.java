package de.hems.event.ranking;

import de.hems.event.EventTeam;

import java.util.Comparator;

/**
 * How the teams of an event are compared against each other.
 * <p>
 * This is the piece that decides whether an event has a leaderboard at all and what "better" means: more
 * points, fewer points, a faster time or nothing at all. A new kind of comparison is a new implementation
 * that is handed to {@link RankingStrategies#register(RankingStrategy)} - events can then use it without a
 * single change to the calendar or the UI.
 */
public interface RankingStrategy {

    /**
     * @return the id this strategy is stored and synchronised with, e.g. {@code HIGHEST_SCORE}
     */
    String getId();

    /**
     * @return the name shown in the UI
     */
    String getDisplayName();

    /**
     * @return a short explanation shown as lore
     */
    String getDescription();

    /**
     * @return whether the teams are put into an order at all
     */
    boolean isRanked();

    /**
     * @return the comparison between two teams, the better team first
     */
    Comparator<EventTeam> comparator();

    /**
     * @param team the team whose score should be shown
     * @return the score of the team as it is shown to players
     */
    String formatScore(EventTeam team);
}

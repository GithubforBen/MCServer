package de.hems.event.ranking;

import de.hems.event.EventTeam;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The leaderboard of an event: the teams in the order the {@link RankingStrategy} puts them, with equal
 * teams sharing a place.
 */
public class Ranking implements Serializable {
    private static final long serialVersionUID = 301L;

    private final RankingStrategy strategy;
    private final List<Entry> entries;

    private Ranking(RankingStrategy strategy, List<Entry> entries) {
        this.strategy = strategy;
        this.entries = entries;
    }

    /**
     * Builds the leaderboard of a set of teams.
     *
     * @param strategy how the teams are compared
     * @param teams    the teams of the event
     * @return the ranking, unranked strategies keep the teams in their original order
     */
    public static Ranking of(RankingStrategy strategy, List<EventTeam> teams) {
        if (strategy == null) strategy = RankingStrategies.NONE;
        List<EventTeam> sorted = new ArrayList<>(teams == null ? List.of() : teams);
        if (strategy.isRanked()) sorted.sort(strategy.comparator());
        List<Entry> entries = new ArrayList<>();
        int place = 0;
        EventTeam previous = null;
        for (int i = 0; i < sorted.size(); i++) {
            EventTeam team = sorted.get(i);
            if (!strategy.isRanked()) {
                entries.add(new Entry(0, team, strategy.formatScore(team)));
                continue;
            }
            // teams that compare equal share the place
            if (previous == null || strategy.comparator().compare(previous, team) != 0) {
                place = i + 1;
            }
            entries.add(new Entry(place, team, strategy.formatScore(team)));
            previous = team;
        }
        return new Ranking(strategy, entries);
    }

    public RankingStrategy getStrategy() {
        return strategy;
    }

    /**
     * @return whether the teams are put into an order at all
     */
    public boolean isRanked() {
        return strategy.isRanked();
    }

    public List<Entry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * @return the team on the first place, or {@code null} if there is no ranking or no team
     */
    public EventTeam getWinner() {
        if (!isRanked() || entries.isEmpty()) return null;
        return entries.get(0).getTeam();
    }

    /**
     * One line of the leaderboard.
     */
    public static class Entry implements Serializable {
        private static final long serialVersionUID = 302L;

        private final int place;
        private final EventTeam team;
        private final String score;

        Entry(int place, EventTeam team, String score) {
            this.place = place;
            this.team = team;
            this.score = score;
        }

        /**
         * @return the place of the team, {@code 0} if the event has no ranking
         */
        public int getPlace() {
            return place;
        }

        public EventTeam getTeam() {
            return team;
        }

        /**
         * @return the score of the team, already formatted by the strategy
         */
        public String getScore() {
            return score;
        }

        @Override
        public String toString() {
            return (place > 0 ? place + ". " : "") + team.getName() + " - " + score;
        }
    }
}

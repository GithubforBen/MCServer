package de.hems.event.ranking;

import de.hems.event.EventTeam;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * All comparisons that events can use. The built in ones cover the usual cases; a plugin that needs
 * something else registers its own and can use it right away.
 */
public final class RankingStrategies {

    private static final Map<String, RankingStrategy> STRATEGIES = new LinkedHashMap<>();

    /** Most points win - the usual leaderboard. */
    public static final RankingStrategy HIGHEST_SCORE = new SimpleRankingStrategy(
            "HIGHEST_SCORE", "Meiste Punkte", "Das Team mit den meisten Punkten gewinnt",
            true, Comparator.comparingDouble(EventTeam::getScore).reversed()) {
        @Override
        public String formatScore(EventTeam team) {
            return format(team.getScore()) + " Punkte";
        }
    };

    /** Fewest points win, e.g. for penalty points. */
    public static final RankingStrategy LOWEST_SCORE = new SimpleRankingStrategy(
            "LOWEST_SCORE", "Wenigste Punkte", "Das Team mit den wenigsten Punkten gewinnt",
            true, Comparator.comparingDouble(EventTeam::getScore)) {
        @Override
        public String formatScore(EventTeam team) {
            return format(team.getScore()) + " Punkte";
        }
    };

    /** The score is a time in seconds, the fastest team wins. */
    public static final RankingStrategy FASTEST_TIME = new SimpleRankingStrategy(
            "FASTEST_TIME", "Schnellste Zeit", "Das Team mit der kürzesten Zeit gewinnt",
            true, Comparator.comparingDouble((EventTeam team) -> team.getScore() <= 0 ? Double.MAX_VALUE : team.getScore())) {
        @Override
        public String formatScore(EventTeam team) {
            if (team.getScore() <= 0) return "keine Zeit";
            long total = (long) team.getScore();
            return String.format(Locale.ROOT, "%d:%02d min", total / 60, total % 60);
        }
    };

    /** Teams exist, but nobody wins - for events that are just played together. */
    public static final RankingStrategy NONE = new SimpleRankingStrategy(
            "NONE", "Keine Rangliste", "Die Teams werden nicht verglichen",
            false, Comparator.comparing(EventTeam::getName, Comparator.nullsLast(String::compareTo))) {
        @Override
        public String formatScore(EventTeam team) {
            return team.getSize() + " Spieler";
        }
    };

    static {
        register(HIGHEST_SCORE);
        register(LOWEST_SCORE);
        register(FASTEST_TIME);
        register(NONE);
    }

    private RankingStrategies() {
    }

    /**
     * Makes a comparison usable for events. Call this before events that use it are loaded, normally in the
     * {@code onEnable} of the plugin that brings it.
     *
     * @param strategy the comparison to add
     */
    public static void register(RankingStrategy strategy) {
        STRATEGIES.put(strategy.getId(), strategy);
    }

    /**
     * @param id the id of a comparison
     * @return the comparison, or {@link #NONE} if it is unknown here
     */
    public static RankingStrategy get(String id) {
        if (id == null) return NONE;
        return STRATEGIES.getOrDefault(id, NONE);
    }

    /**
     * @param id the id of a comparison
     * @return whether that comparison is known on this server
     */
    public static boolean isKnown(String id) {
        return id != null && STRATEGIES.containsKey(id);
    }

    /**
     * @return every registered comparison
     */
    public static List<RankingStrategy> all() {
        return new ArrayList<>(STRATEGIES.values());
    }

    /**
     * @param current the comparison that is selected right now
     * @return the next one, wrapping around - used by the UI to cycle through them
     */
    public static RankingStrategy next(RankingStrategy current) {
        List<RankingStrategy> all = all();
        int index = all.indexOf(get(current == null ? null : current.getId()));
        return all.get(Math.floorMod(index + 1, all.size()));
    }

    static String format(double value) {
        if (value == Math.rint(value)) return String.valueOf((long) value);
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /**
     * A comparison that is fully described by a comparator - the base for most of them.
     */
    private abstract static class SimpleRankingStrategy implements RankingStrategy {
        private final String id;
        private final String displayName;
        private final String description;
        private final boolean ranked;
        private final Comparator<EventTeam> comparator;

        SimpleRankingStrategy(String id, String displayName, String description, boolean ranked,
                              Comparator<EventTeam> comparator) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
            this.ranked = ranked;
            this.comparator = comparator;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public boolean isRanked() {
            return ranked;
        }

        @Override
        public Comparator<EventTeam> comparator() {
            return comparator;
        }
    }
}

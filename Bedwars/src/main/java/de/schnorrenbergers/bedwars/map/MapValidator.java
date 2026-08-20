package de.schnorrenbergers.bedwars.map;

import de.schnorrenbergers.bedwars.game.GameMode;
import de.schnorrenbergers.bedwars.game.TeamColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Says what is still missing before a map can be played.
 * <p>
 * The point is to answer that <em>before</em> a round starts rather than with a null pointer in the middle
 * of one, which is why {@code /bw setup check} exists at all.
 */
public final class MapValidator {

    private MapValidator() {
    }

    /**
     * @param map  the map to look at
     * @param mode the mode it should host
     * @return every problem, in the order they are worth fixing; empty means it is ready
     */
    public static List<String> check(ArenaMap map, GameMode mode) {
        List<String> problems = new ArrayList<>();

        if (map.getLobby() == null) {
            problems.add("The waiting lobby is not set (/bw setup lobby).");
        }

        List<TeamColor> colors = map.getColorsFor(mode);
        if (colors.size() < mode.getTeamCount()) {
            problems.add(mode.getDisplayName() + " needs " + mode.getTeamCount() + " teams, the map has "
                    + colors.size() + " (/bw setup team <colour> spawn).");
        }

        for (TeamColor color : colors) {
            TeamSpot spot = map.getTeam(color);
            if (spot == null) {
                problems.add("Team " + color.getDisplayName() + " has no base at all.");
                continue;
            }
            if (spot.getSpawn() == null) problems.add("Team " + color.getDisplayName() + ": spawn missing.");
            if (spot.getBed() == null) problems.add("Team " + color.getDisplayName() + ": bed missing.");
            if (spot.getGenerator() == null) {
                problems.add("Team " + color.getDisplayName() + ": resource generator missing.");
            }
            if (spot.getShop() == null) problems.add("Team " + color.getDisplayName() + ": shop missing.");
            if (spot.getUpgrade() == null) {
                problems.add("Team " + color.getDisplayName() + ": upgrade shop missing.");
            }
        }

        if (map.getGenerators().isEmpty()) {
            problems.add("No diamond or emerald generator is set (/bw setup gen add DIAMOND).");
        }
        return problems;
    }

    /**
     * @param map  the map
     * @param mode the mode
     * @return whether a round could be started on it
     */
    public static boolean isPlayable(ArenaMap map, GameMode mode) {
        return check(map, mode).isEmpty();
    }
}

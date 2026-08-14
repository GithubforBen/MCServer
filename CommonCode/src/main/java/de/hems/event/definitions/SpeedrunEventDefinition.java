package de.hems.event.definitions;

import de.hems.event.EventDefinition;
import de.hems.event.ranking.RankingStrategies;
import de.hems.event.ranking.RankingStrategy;
import de.hems.types.FileType;

/**
 * A speedrun: the teams play the same route, the fastest time wins. Shows that a different comparison is
 * all it takes to get a completely different leaderboard.
 */
public class SpeedrunEventDefinition extends EventDefinition {

    @Override
    public String getId() {
        return "SPEEDRUN";
    }

    @Override
    public String getDisplayName() {
        return "Speedrun";
    }

    @Override
    public String getDescription() {
        return "Das schnellste Team gewinnt";
    }

    @Override
    public FileType.PLUGIN getPlugin() {
        return null;
    }

    @Override
    public String getIconMaterial() {
        return "CLOCK";
    }

    @Override
    public int getMinTeams() {
        return 1;
    }

    @Override
    public int getDefaultTeamCount() {
        return 2;
    }

    @Override
    public RankingStrategy getDefaultRanking() {
        return RankingStrategies.FASTEST_TIME;
    }
}

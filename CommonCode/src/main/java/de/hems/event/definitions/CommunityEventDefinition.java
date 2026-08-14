package de.hems.event.definitions;

import de.hems.event.EventDefinition;
import de.hems.event.EventTeam;
import de.hems.event.EventTeamColor;
import de.hems.event.ranking.RankingStrategies;
import de.hems.event.ranking.RankingStrategy;
import de.hems.types.FileType;

import java.util.List;

/**
 * An event that is just played together - the teams are groups, nobody wins.
 */
public class CommunityEventDefinition extends EventDefinition {

    @Override
    public String getId() {
        return "COMMUNITY";
    }

    @Override
    public String getDisplayName() {
        return "Community Event";
    }

    @Override
    public String getDescription() {
        return "Zusammen spielen ohne Rangliste";
    }

    @Override
    public FileType.PLUGIN getPlugin() {
        return null;
    }

    @Override
    public String getIconMaterial() {
        return "CAKE";
    }

    @Override
    public int getMinTeams() {
        return 1;
    }

    @Override
    public int getDefaultTeamCount() {
        return 1;
    }

    @Override
    public RankingStrategy getDefaultRanking() {
        return RankingStrategies.NONE;
    }

    @Override
    public List<EventTeam> createTeams(int teamCount) {
        if (teamCount <= 1) return List.of(new EventTeam("Alle", EventTeamColor.AQUA));
        return super.createTeams(teamCount);
    }
}

package de.hems.event.definitions;

import de.hems.event.EventDefinition;
import de.hems.event.EventTeam;
import de.hems.event.EventTeamColor;
import de.hems.event.ranking.RankingStrategies;
import de.hems.event.ranking.RankingStrategy;
import de.hems.types.FileType;
import de.hems.types.ServerTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * A bedwars round: teams defend their bed, the team with the most kept beds and kills wins.
 */
public class BedwarsEventDefinition extends EventDefinition {

    @Override
    public String getId() {
        return "BEDWARS";
    }

    @Override
    public String getDisplayName() {
        return "Bedwars";
    }

    @Override
    public String getDescription() {
        return "Jedes Team verteidigt sein Bett";
    }

    @Override
    public FileType.PLUGIN getPlugin() {
        return FileType.PLUGIN.BEDWARS;
    }

    @Override
    public String getIconMaterial() {
        return "RED_BED";
    }

    @Override
    public ServerTemplate getServerTemplate() {
        return ServerTemplate.BEDWARS;
    }

    @Override
    public int getMinTeams() {
        return 2;
    }

    @Override
    public int getMaxTeams() {
        return 8;
    }

    @Override
    public int getDefaultTeamCount() {
        return 4;
    }

    @Override
    public int getMaxTeamSize() {
        return 4;
    }

    @Override
    public RankingStrategy getDefaultRanking() {
        return RankingStrategies.HIGHEST_SCORE;
    }

    @Override
    public List<EventTeam> createTeams(int teamCount) {
        List<EventTeam> teams = new ArrayList<>();
        for (int i = 0; i < teamCount; i++) {
            EventTeamColor color = EventTeamColor.byIndex(i);
            teams.add(new EventTeam("Bett " + color.getDisplayName(), color));
        }
        return teams;
    }
}

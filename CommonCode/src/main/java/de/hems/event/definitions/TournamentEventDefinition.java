package de.hems.event.definitions;

import de.hems.event.EventDefinition;
import de.hems.event.ranking.RankingStrategies;
import de.hems.event.ranking.RankingStrategy;
import de.hems.types.FileType;

import java.util.List;

/**
 * A tournament on a plain event server: teams collect points in whatever is played, most points win.
 */
public class TournamentEventDefinition extends EventDefinition {

    @Override
    public String getId() {
        return "TURNIER";
    }

    @Override
    public String getDisplayName() {
        return "Turnier";
    }

    @Override
    public String getDescription() {
        return "Teams sammeln Punkte, das beste Team gewinnt";
    }

    @Override
    public FileType.PLUGIN getPlugin() {
        return null; // wird auf einem leeren Event Server gespielt
    }

    @Override
    public List<FileType.PLUGIN> getAdditionalPlugins() {
        return List.of(FileType.PLUGIN.WORLDEDIT);
    }

    @Override
    public String getIconMaterial() {
        return "GOLDEN_SWORD";
    }

    @Override
    public int getMaxTeams() {
        return 9;
    }

    @Override
    public RankingStrategy getDefaultRanking() {
        return RankingStrategies.HIGHEST_SCORE;
    }
}

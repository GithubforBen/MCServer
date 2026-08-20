package de.schnorrenbergers.bedwars.generator;

import de.schnorrenbergers.bedwars.config.GeneratorSettings;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.map.ArenaMap;
import de.schnorrenbergers.bedwars.map.GeneratorSpot;
import de.schnorrenbergers.bedwars.map.TeamSpot;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Every generator of the round.
 * <p>
 * Built once when the round starts, from the map on one side and {@code generators.yml} on the other: the
 * map says where, the config says what and how fast.
 */
public final class GeneratorManager {

    private final List<Generator> generators = new ArrayList<>();
    private final GeneratorSettings settings;

    public GeneratorManager(GeneratorSettings settings) {
        this.settings = settings;
    }

    /**
     * Puts the generators of a map into the world.
     *
     * @param game the round
     */
    public void build(Game game) {
        remove();
        ArenaMap arena = game.getArena();
        if (arena == null || game.getWorld() == null) return;

        for (GameTeam team : game.getTeams()) {
            if (!team.isAlive()) continue;
            TeamSpot spot = arena.getTeam(team.getColor());
            if (spot == null || spot.getGenerator() == null) continue;
            Location at = spot.getGenerator().toLocation(game.getWorld());
            for (GeneratorSettings.Type type : settings.teamTypes()) {
                generators.add(new Generator(type, at, team));
            }
        }

        for (GeneratorSpot spot : arena.getGenerators()) {
            GeneratorSettings.Type type = settings.get(spot.type());
            if (type == null) {
                Bukkit.getLogger().warning("[Bedwars] The map " + arena.getName() + " has a '" + spot.type()
                        + "' generator, which generators.yml does not know.");
                continue;
            }
            generators.add(new Generator(type, spot.point().toLocation(game.getWorld()), null));
        }
    }

    /**
     * @param game  the round
     * @param ticks how long it has been running
     */
    public void tick(Game game, long ticks) {
        for (Generator generator : generators) {
            generator.tick(game);
        }
        if (ticks % 20L == 0L) {
            for (Generator generator : generators) {
                generator.updateHologram();
            }
        }
    }

    /**
     * Raises every generator of a kind, which is what a forge upgrade does.
     *
     * @param typeId the kind, e.g. {@code IRON}
     * @param team   whose generators, or {@code null} for the ones in the middle
     * @param tier   the level they should run at
     */
    public void setTier(String typeId, GameTeam team, int tier) {
        for (Generator generator : generators) {
            if (!generator.getType().id().equalsIgnoreCase(typeId)) continue;
            if (team != null && generator.getOwner() != team) continue;
            if (team == null && generator.getOwner() != null) continue;
            generator.setTier(tier);
        }
    }

    /**
     * Takes the floating text of every generator away.
     */
    public void remove() {
        generators.forEach(Generator::remove);
        generators.clear();
    }

    public List<Generator> all() {
        return List.copyOf(generators);
    }
}

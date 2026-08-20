package de.schnorrenbergers.bedwars.map;

import de.schnorrenbergers.bedwars.game.GameMode;
import de.schnorrenbergers.bedwars.game.TeamColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One map, as it is written in {@code maps/<name>.yml}.
 * <p>
 * A map holds up to eight teams, and which of them a mode uses is part of the map rather than of the mode:
 * the same four bases that make a 4v4v4v4 map are a trio map as well, while eight bases are needed for
 * solo and doubles. Without that per mode list a map would belong to exactly one mode.
 */
public class ArenaMap {

    private final String name;

    private String displayName;
    private MapPoint lobby;
    private MapPoint spectator;
    private int buildMaxY = 256;
    private int voidY = 0;

    private final Map<TeamColor, TeamSpot> teams = new LinkedHashMap<>();
    private final List<GeneratorSpot> generators = new ArrayList<>();
    /** Mode id to the teams that mode plays with. */
    private final Map<String, List<TeamColor>> modeTeams = new LinkedHashMap<>();

    public ArenaMap(String name) {
        this.name = name.toLowerCase(Locale.ROOT);
        this.displayName = name;
    }

    // ---------------------------------------------------------------- yaml

    /**
     * @param name   the map name
     * @param config the file it is written in
     * @return the map
     */
    public static ArenaMap read(String name, YamlConfiguration config) {
        ArenaMap map = new ArenaMap(name);
        map.displayName = config.getString("display-name", name);
        map.lobby = MapPoint.read(config, "lobby");
        map.spectator = MapPoint.read(config, "spectator");
        map.buildMaxY = config.getInt("build.max-y", 256);
        map.voidY = config.getInt("build.void-y", 0);

        ConfigurationSection teams = config.getConfigurationSection("teams");
        if (teams != null) {
            for (String key : teams.getKeys(false)) {
                TeamColor color = TeamColor.byName(key);
                ConfigurationSection section = teams.getConfigurationSection(key);
                if (color == null || section == null) continue;
                map.teams.put(color, TeamSpot.read(color, section));
            }
        }

        ConfigurationSection generators = config.getConfigurationSection("generators");
        if (generators != null) {
            for (String key : generators.getKeys(false)) {
                ConfigurationSection section = generators.getConfigurationSection(key);
                if (section == null) continue;
                MapPoint point = MapPoint.read(section, "at");
                if (point == null) continue;
                map.generators.add(new GeneratorSpot(section.getString("type", ""), point));
            }
        }

        ConfigurationSection modes = config.getConfigurationSection("modes");
        if (modes != null) {
            for (String key : modes.getKeys(false)) {
                List<TeamColor> colors = new ArrayList<>();
                for (String colorName : modes.getStringList(key)) {
                    TeamColor color = TeamColor.byName(colorName);
                    if (color != null) colors.add(color);
                }
                if (!colors.isEmpty()) map.modeTeams.put(key.toLowerCase(Locale.ROOT), colors);
            }
        }
        return map;
    }

    /**
     * @return the map in the shape it is written to disk in
     */
    public YamlConfiguration write() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("display-name", displayName);
        config.setComments("display-name", List.of("The name players are shown. MiniMessage works here."));
        if (lobby != null) lobby.write(config, "lobby");
        if (spectator != null) spectator.write(config, "spectator");

        config.set("build.max-y", buildMaxY);
        config.setComments("build", List.of("Nobody builds above max-y, and anything below void-y is the void."));
        config.set("build.void-y", voidY);

        ConfigurationSection teamSection = config.createSection("teams");
        for (TeamSpot spot : teams.values()) {
            spot.write(teamSection.createSection(spot.getColor().name()));
        }

        ConfigurationSection generatorSection = config.createSection("generators");
        generatorSection.setComments("", List.of("The generators that belong to nobody, out in the middle."));
        for (int i = 0; i < generators.size(); i++) {
            GeneratorSpot generator = generators.get(i);
            ConfigurationSection entry = generatorSection.createSection(String.valueOf(i));
            entry.set("type", generator.type());
            generator.point().write(entry, "at");
        }

        ConfigurationSection modeSection = config.createSection("modes");
        modeSection.setComments("", List.of(
                "Which teams a mode plays with on this map.",
                "A mode that is not listed uses the first teams of the list above."));
        for (Map.Entry<String, List<TeamColor>> entry : modeTeams.entrySet()) {
            modeSection.set(entry.getKey(), entry.getValue().stream().map(Enum::name).toList());
        }
        return config;
    }

    // --------------------------------------------------------------- teams

    /**
     * @param mode the mode to be played
     * @return the teams this map uses for it
     */
    public List<TeamColor> getColorsFor(GameMode mode) {
        List<TeamColor> configured = modeTeams.get(mode.getId());
        if (configured != null && !configured.isEmpty()) {
            return configured.size() <= mode.getTeamCount()
                    ? List.copyOf(configured)
                    : List.copyOf(configured.subList(0, mode.getTeamCount()));
        }
        // no list for this mode: take the teams the map has, in order, as far as the mode needs
        List<TeamColor> available = new ArrayList<>(teams.keySet());
        return available.size() <= mode.getTeamCount()
                ? List.copyOf(available)
                : List.copyOf(available.subList(0, mode.getTeamCount()));
    }

    /**
     * @param mode the mode to be played
     * @return whether this map has a complete base for every team that mode needs
     */
    public boolean supports(GameMode mode) {
        List<TeamColor> colors = getColorsFor(mode);
        if (colors.size() < mode.getTeamCount()) return false;
        for (TeamColor color : colors) {
            TeamSpot spot = teams.get(color);
            if (spot == null || !spot.isComplete()) return false;
        }
        return lobby != null;
    }

    /**
     * @param color the team
     * @return its base, creating an empty one when the map does not have it yet
     */
    public TeamSpot getOrCreateTeam(TeamColor color) {
        return teams.computeIfAbsent(color, TeamSpot::new);
    }

    public @Nullable TeamSpot getTeam(TeamColor color) {
        return teams.get(color);
    }

    public void removeTeam(TeamColor color) {
        teams.remove(color);
    }

    public Map<TeamColor, TeamSpot> getTeams() {
        return Map.copyOf(teams);
    }

    /**
     * @param mode   the mode
     * @param colors the teams it plays with, empty to let the map decide again
     */
    public void setModeTeams(String mode, List<TeamColor> colors) {
        String key = mode.toLowerCase(Locale.ROOT);
        if (colors.isEmpty()) {
            modeTeams.remove(key);
        } else {
            modeTeams.put(key, List.copyOf(colors));
        }
    }

    public Map<String, List<TeamColor>> getModeTeams() {
        return Map.copyOf(modeTeams);
    }

    // ---------------------------------------------------------- generators

    public List<GeneratorSpot> getGenerators() {
        return List.copyOf(generators);
    }

    public void addGenerator(GeneratorSpot generator) {
        generators.add(generator);
    }

    /**
     * Takes the generator closest to a spot away.
     *
     * @param point where to look
     * @param range how far to look, in blocks
     * @return the one that was removed, or {@code null} when there was none in range
     */
    public @Nullable GeneratorSpot removeGeneratorNear(MapPoint point, double range) {
        GeneratorSpot closest = null;
        double closestDistance = range * range;
        for (GeneratorSpot generator : generators) {
            double distance = squaredDistance(generator.point(), point);
            if (distance > closestDistance) continue;
            closest = generator;
            closestDistance = distance;
        }
        if (closest != null) generators.remove(closest);
        return closest;
    }

    private static double squaredDistance(MapPoint one, MapPoint other) {
        double dx = one.x() - other.x();
        double dy = one.y() - other.y();
        double dz = one.z() - other.z();
        return dx * dx + dy * dy + dz * dz;
    }

    // ------------------------------------------------------------- getters

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public @Nullable MapPoint getLobby() {
        return lobby;
    }

    public void setLobby(MapPoint lobby) {
        this.lobby = lobby;
    }

    /**
     * @return where spectators start, falling back to the lobby when the map does not say
     */
    public @Nullable MapPoint getSpectator() {
        return spectator == null ? lobby : spectator;
    }

    public void setSpectator(MapPoint spectator) {
        this.spectator = spectator;
    }

    public int getBuildMaxY() {
        return buildMaxY;
    }

    public void setBuildMaxY(int buildMaxY) {
        this.buildMaxY = buildMaxY;
    }

    public int getVoidY() {
        return voidY;
    }

    public void setVoidY(int voidY) {
        this.voidY = voidY;
    }
}

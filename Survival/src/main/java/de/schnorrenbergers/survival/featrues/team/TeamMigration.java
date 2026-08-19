package de.schnorrenbergers.survival.featrues.team;

import de.hems.paper.team.TeamService;
import de.hems.types.team.TeamData;
import de.hems.types.team.TeamSettings;
import de.schnorrenbergers.survival.Survival;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scoreboard.Team;

import java.util.UUID;

/**
 * Moves teams that were created before they were stored centrally onto the launcher.
 * <p>
 * The old setup kept only the leader in {@code team-config.yml} and the members in minecraft's own
 * scoreboard, with the claims in a third place. Without this, switching to central storage would look
 * exactly like every team on the server having been deleted - so everything that can still be found is
 * collected and pushed up once, and the old file is marked as done rather than removed, in case somebody
 * wants to look at it afterwards.
 */
public final class TeamMigration {

    /** Set in the old config once the teams have been moved. */
    private static final String DONE_KEY = "migrated-to-host";

    private TeamMigration() {
    }

    /**
     * Runs the migration if there is anything to move. Must be called once the teams have been loaded from
     * the launcher, so an already migrated team is not created a second time.
     */
    public static void run() {
        YamlConfiguration old = Survival.getInstance().getTeamConfig().getConfig();
        if (old.getBoolean(DONE_KEY, false)) return;
        ConfigurationSection teams = old.getConfigurationSection("teams");
        if (teams == null || teams.getKeys(false).isEmpty()) {
            markDone(old);
            return;
        }

        int moved = 0;
        for (String name : teams.getKeys(false)) {
            if (TeamService.getTeam(name) != null) continue;
            TeamData team = build(name, teams.getConfigurationSection(name), old);
            if (team == null) continue;
            TeamService.Result result = TeamService.saveBlocking(team, true);
            if (result.successful()) {
                moved++;
            } else {
                Survival.getInstance().getLogger().warning(
                        "Could not move team " + name + " to the host: " + result.message());
            }
        }
        Survival.getInstance().getLogger().info("Moved " + moved + " teams to the host.");
        markDone(old);
    }

    /**
     * Collects everything that is known about an old team.
     *
     * @param name    the team name
     * @param section its entry in the old config
     * @param old     the old config, which also holds the claims
     * @return the team, or {@code null} if not even a leader could be found
     */
    private static TeamData build(String name, ConfigurationSection section, YamlConfiguration old) {
        UUID leader = null;
        if (section != null && section.contains("leaderUUID")) {
            try {
                leader = UUID.fromString(section.getString("leaderUUID"));
            } catch (IllegalArgumentException e) {
                // a team without a readable leader is still worth moving if it has members
            }
        }

        TeamData team = new TeamData();
        team.setName(name);
        team.setLeader(leader);
        team.getSettings().set(TeamSettings.Key.MAX_MEMBERS,
                Survival.getInstance().getTeamRules().getMaxMembersCap());

        // membership and the look of the team only ever existed in minecraft's own scoreboard
        Team scoreboardTeam = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(name);
        if (scoreboardTeam != null) {
            String prefix = scoreboardTeam.getPrefix();
            if (prefix != null && !prefix.isBlank()) {
                team.setTag(ChatColor.stripColor(prefix).replace("[", "").replace("]", "").trim());
            }
            if (scoreboardTeam.getColor() != null) team.setColor(scoreboardTeam.getColor().name());
            for (OfflinePlayer member : scoreboardTeam.getPlayers()) {
                if (member != null) team.getMembers().add(member.getUniqueId());
            }
        }
        if (team.getTag() == null || team.getTag().isBlank()) {
            team.setTag(name.length() > 4 ? name.substring(0, 4).toUpperCase() : name.toUpperCase());
        }
        if (team.getMembers().isEmpty() && leader == null) return null;

        collectClaims(team, old);
        return team;
    }

    /**
     * Picks the chunks that belonged to this team out of the old three level claims section.
     *
     * @param team the team being moved
     * @param old  the old config
     */
    private static void collectClaims(TeamData team, YamlConfiguration old) {
        ConfigurationSection claims = old.getConfigurationSection("claims");
        if (claims == null) return;
        for (String world : claims.getKeys(false)) {
            ConfigurationSection xs = claims.getConfigurationSection(world);
            if (xs == null) continue;
            for (String x : xs.getKeys(false)) {
                ConfigurationSection zs = xs.getConfigurationSection(x);
                if (zs == null) continue;
                for (String z : zs.getKeys(false)) {
                    if (!team.getName().equals(zs.getString(z))) continue;
                    try {
                        team.getClaims().add(TeamData.claimKey(world, Integer.parseInt(x), Integer.parseInt(z)));
                    } catch (NumberFormatException ignored) {
                        // an unreadable coordinate is not worth failing the whole migration over
                    }
                }
            }
        }
    }

    private static void markDone(YamlConfiguration old) {
        old.set(DONE_KEY, true);
        Survival.getInstance().getTeamConfig().save();
    }
}

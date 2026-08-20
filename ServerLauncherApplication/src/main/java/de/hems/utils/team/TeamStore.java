package de.hems.utils.team;

import de.hems.types.team.TeamData;
import de.hems.types.team.TeamSettings;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where the teams of the network live.
 * <p>
 * Teams used to be a config file next to the survival server, which tied them to that one server and to its
 * data directory. They now belong to the launcher, so every server sees the same teams and a wiped survival
 * world no longer takes them with it.
 * <p>
 * Everything is kept in memory and written through to {@code teams.yml} on change, because teams are few
 * and reads are far more common than writes.
 */
public class TeamStore {

    private final File file;
    private final YamlConfiguration config;
    /** Keyed by the lower case name, so team names are unique regardless of how they are typed. */
    private final Map<String, TeamData> teams = new ConcurrentHashMap<>();

    public TeamStore() {
        this(new File("./teams.yml"));
    }

    public TeamStore(File file) {
        this.file = file;
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        load();
    }

    private void load() {
        ConfigurationSection section = config.getConfigurationSection("teams");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) continue;
            TeamData team = read(key, entry);
            if (team != null) teams.put(key.toLowerCase(Locale.ROOT), team);
        }
        System.out.println("Loaded " + teams.size() + " teams from " + file.getName());
    }

    /**
     * @param name  the name the team is stored under
     * @param entry the section holding it
     * @return the team, or {@code null} if the entry is unusable
     */
    private static TeamData read(String name, ConfigurationSection entry) {
        TeamData team = new TeamData();
        team.setName(entry.getString("name", name));
        team.setTag(entry.getString("tag"));
        team.setColor(entry.getString("color", "WHITE"));
        String leader = entry.getString("leader");
        if (leader != null) {
            try {
                team.setLeader(UUID.fromString(leader));
            } catch (IllegalArgumentException e) {
                // a team without a usable leader is still worth keeping, it can be handed over
            }
        }
        for (String member : entry.getStringList("members")) {
            try {
                team.getMembers().add(UUID.fromString(member.trim()));
            } catch (IllegalArgumentException ignored) {
                // skip the broken entry rather than dropping the whole team
            }
        }
        team.setCreatedAt(entry.getLong("created-at", System.currentTimeMillis()));
        team.setHome(entry.getString("home"));
        team.setRevision(entry.getLong("revision", 0L));
        team.getClaims().addAll(entry.getStringList("claims"));
        ConfigurationSection settings = entry.getConfigurationSection("settings");
        team.setSettings(TeamSettings.fromMap(settings == null ? Map.of() : settings.getValues(false)));
        return team.getName() == null ? null : team;
    }

    /**
     * @param team the team to write into the config
     */
    private void write(TeamData team) {
        String path = "teams." + team.getName();
        config.set(path + ".name", team.getName());
        config.set(path + ".tag", team.getTag());
        config.set(path + ".color", team.getColor());
        config.set(path + ".leader", team.getLeader() == null ? null : team.getLeader().toString());
        List<String> members = new ArrayList<>();
        for (UUID member : team.getMembers()) members.add(member.toString());
        config.set(path + ".members", members);
        config.set(path + ".created-at", team.getCreatedAt());
        config.set(path + ".home", team.getHome());
        config.set(path + ".revision", team.getRevision());
        config.set(path + ".claims", new ArrayList<>(team.getClaims()));
        Map<String, Object> settings = new LinkedHashMap<>(team.getSettings().asMap());
        config.set(path + ".settings", settings.isEmpty() ? null : settings);
    }

    public synchronized void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            System.out.println("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }

    /**
     * @return every team, as a fresh list the caller may keep
     */
    public List<TeamData> getTeams() {
        return new ArrayList<>(teams.values());
    }

    /**
     * @param name the team to look up, in any capitalisation
     * @return that team, or {@code null}
     */
    public TeamData getTeam(String name) {
        return name == null ? null : teams.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * @param uuid the player to look up
     * @return the team that player belongs to, or {@code null}
     */
    public TeamData getTeamOf(UUID uuid) {
        if (uuid == null) return null;
        for (TeamData team : teams.values()) {
            if (team.hasMember(uuid)) return team;
        }
        return null;
    }

    /**
     * Stores a team.
     * <p>
     * The write is refused when the caller worked from an older revision than the one that is stored, which
     * is what stops two servers editing the same team from losing one set of changes.
     *
     * @param team            the team to store
     * @param createIfMissing whether it may be created
     * @return what happened, for the answer sent back to the caller
     */
    public synchronized Result put(TeamData team, boolean createIfMissing) {
        return put(team, createIfMissing, null);
    }

    /**
     * Stores a team, optionally under a new name.
     * <p>
     * A rename has to happen in one step. Writing the team under its new name and deleting the old entry
     * afterwards would look like a brand new team whose members all belong to another team already, so the
     * write would be refused and no team could ever be renamed.
     *
     * @param team            the team to store
     * @param createIfMissing whether it may be created
     * @param renameFrom      the name the team had before, or {@code null} for a normal write
     * @return what happened, for the answer sent back to the caller
     */
    public synchronized Result put(TeamData team, boolean createIfMissing, String renameFrom) {
        if (team == null || team.getName() == null || team.getName().isBlank()) {
            return Result.failed("Das Team hat keinen Namen.");
        }
        String key = team.getName().toLowerCase(Locale.ROOT);
        String oldKey = renameFrom == null ? null : renameFrom.toLowerCase(Locale.ROOT);
        boolean renaming = oldKey != null && !oldKey.equals(key);
        // a rename is checked against the entry of the old name, the new one does not exist yet
        TeamData existing = teams.get(renaming ? oldKey : key);
        if (existing == null && !createIfMissing) {
            return Result.failed("Das Team '" + team.getName() + "' gibt es nicht.");
        }
        if (renaming) {
            if (existing == null) {
                return Result.failed("Das Team '" + renameFrom + "' gibt es nicht.");
            }
            if (teams.containsKey(key)) {
                return Result.failed("Diesen Teamnamen gibt es schon.");
            }
        }
        if (existing != null && team.getRevision() != existing.getRevision()) {
            return Result.failed("Das Team wurde inzwischen woanders geändert. Bitte nochmal öffnen.");
        }
        // only a genuinely new team has to prove its members are free - a rename keeps the same people
        if (existing == null && !isMemberFree(team)) {
            return Result.failed("Mindestens ein Mitglied ist bereits in einem anderen Team.");
        }
        if (renaming) {
            teams.remove(oldKey);
            config.set("teams." + existing.getName(), null);
        }
        team.setRevision(team.getRevision() + 1);
        teams.put(key, team);
        write(team);
        save();
        return Result.ok(team);
    }

    /**
     * @param team the team that is about to be created
     * @return whether none of its members belongs to another team already
     */
    private boolean isMemberFree(TeamData team) {
        for (UUID member : team.getMembers()) {
            TeamData other = getTeamOf(member);
            if (other != null && !other.getName().equalsIgnoreCase(team.getName())) return false;
        }
        return true;
    }

    /**
     * @param name the team to remove
     * @return whether it existed
     */
    public synchronized boolean delete(String name) {
        if (name == null) return false;
        TeamData removed = teams.remove(name.toLowerCase(Locale.ROOT));
        if (removed == null) return false;
        config.set("teams." + removed.getName(), null);
        save();
        return true;
    }

    /**
     * How a write ended.
     *
     * @param successful whether it was stored
     * @param message    what to tell the caller when it was not
     * @param team       the team as it is stored now
     */
    public record Result(boolean successful, String message, TeamData team) {

        static Result ok(TeamData team) {
            return new Result(true, "Gespeichert.", team);
        }

        static Result failed(String message) {
            return new Result(false, message, null);
        }
    }
}

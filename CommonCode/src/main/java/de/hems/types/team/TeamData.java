package de.hems.types.team;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A team, as it is stored on the launcher and handed to every server of the network.
 * <p>
 * Teams used to live in a config file next to the survival server, which meant they existed only there and
 * vanished with that server's data directory. They now belong to the launcher - the node that starts
 * everything else - so the lobby, a second survival world or an event server all see the same teams.
 */
public class TeamData implements Serializable {

    private static final long serialVersionUID = 4002L;

    private String name;
    private String tag;
    /** The name of a {@code ChatColor}, kept as text so this class stays free of bukkit. */
    private String color = "WHITE";
    private UUID leader;
    private final Set<UUID> members = new LinkedHashSet<>();
    private long createdAt = System.currentTimeMillis();
    private TeamSettings settings = new TeamSettings();
    /** The chunks the team owns, as {@code world:x:z}. */
    private final Set<String> claims = new LinkedHashSet<>();
    /** Where {@code /cteam home} leads, as {@code world:x:y:z}, or {@code null} if none is set. */
    private String home;
    /** Bumped on every write so two servers cannot silently overwrite each other. */
    private long revision;

    public TeamData() {
    }

    public TeamData(String name, String tag, UUID leader) {
        this.name = name;
        this.tag = tag;
        this.leader = leader;
        if (leader != null) members.add(leader);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public UUID getLeader() {
        return leader;
    }

    public void setLeader(UUID leader) {
        this.leader = leader;
        if (leader != null) members.add(leader);
    }

    /**
     * @param uuid the player to check
     * @return whether that player leads this team
     */
    public boolean isLeader(UUID uuid) {
        return leader != null && leader.equals(uuid);
    }

    public Set<UUID> getMembers() {
        return members;
    }

    /**
     * @param uuid the player to check
     * @return whether that player belongs to this team
     */
    public boolean hasMember(UUID uuid) {
        return uuid != null && members.contains(uuid);
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public TeamSettings getSettings() {
        return settings;
    }

    public void setSettings(TeamSettings settings) {
        this.settings = settings == null ? new TeamSettings() : settings;
    }

    public Set<String> getClaims() {
        return claims;
    }

    public String getHome() {
        return home;
    }

    public void setHome(String home) {
        this.home = home;
    }

    public long getRevision() {
        return revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }

    /**
     * @return how many players may still join before the team is full
     */
    public int getFreeSlots() {
        return Math.max(0, settings.getNumber(TeamSettings.Key.MAX_MEMBERS) - members.size());
    }

    /**
     * @return the members without the leader, in the order they joined
     */
    public List<UUID> getMembersWithoutLeader() {
        List<UUID> without = new ArrayList<>();
        for (UUID member : members) {
            if (!member.equals(leader)) without.add(member);
        }
        return without;
    }

    /**
     * @param world the world the chunk is in
     * @param x     the chunk x
     * @param z     the chunk z
     * @return the key that chunk is stored under
     */
    public static String claimKey(String world, int x, int z) {
        return world + ":" + x + ":" + z;
    }

    @Override
    public String toString() {
        return name + "[" + members.size() + " Mitglieder]";
    }
}

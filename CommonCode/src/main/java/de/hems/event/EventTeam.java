package de.hems.event;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * One team of an event. Every event has teams - how they are compared against each other is decided by the
 * {@link de.hems.event.ranking.RankingStrategy} of the event.
 */
public class EventTeam implements Serializable {
    private static final long serialVersionUID = 300L;

    private UUID id;
    private String name;
    private EventTeamColor color;
    private Set<UUID> members;
    private double score;

    public EventTeam() {
        this.id = UUID.randomUUID();
        this.members = new LinkedHashSet<>();
    }

    public EventTeam(String name, EventTeamColor color) {
        this();
        this.name = name;
        this.color = color;
    }

    public EventTeam(UUID id, String name, EventTeamColor color, Set<UUID> members, double score) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.name = name;
        this.color = color;
        this.members = members == null ? new LinkedHashSet<>() : new LinkedHashSet<>(members);
        this.score = score;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EventTeamColor getColor() {
        return color == null ? EventTeamColor.WHITE : color;
    }

    public void setColor(EventTeamColor color) {
        this.color = color;
    }

    /**
     * @return the coloured name of the team, ready to be shown to players
     */
    public String getDisplayName() {
        return getColor().getColorCode() + name;
    }

    public Set<UUID> getMembers() {
        if (members == null) members = new LinkedHashSet<>();
        return members;
    }

    /**
     * @param player the player to add
     * @return whether the player was not in the team before
     */
    public boolean addMember(UUID player) {
        return getMembers().add(player);
    }

    /**
     * @param player the player to remove
     * @return whether the player was in the team
     */
    public boolean removeMember(UUID player) {
        return getMembers().remove(player);
    }

    public boolean hasMember(UUID player) {
        return getMembers().contains(player);
    }

    public int getSize() {
        return getMembers().size();
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    /**
     * @param points how many points to add, may be negative
     * @return the score afterwards
     */
    public double addScore(double points) {
        score += points;
        return score;
    }

    @Override
    public String toString() {
        return name + "(" + score + ")";
    }
}

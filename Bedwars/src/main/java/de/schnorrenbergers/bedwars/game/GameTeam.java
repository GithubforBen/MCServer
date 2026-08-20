package de.schnorrenbergers.bedwars.game;

import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One team of the round: who is in it, whether its bed still stands and what it has bought.
 * <p>
 * A team is out once its bed is gone <em>and</em> nobody in it is alive any more - the two together, which
 * is the whole point of the game mode and therefore lives here rather than in a listener.
 */
public class GameTeam {

    private final TeamColor color;
    private final List<GamePlayer> members = new ArrayList<>();
    /** Upgrade id to the level this team has reached, filled from phase 4 on. */
    private final Map<String, Integer> upgrades = new LinkedHashMap<>();

    private boolean bedAlive = true;
    private boolean eliminated;

    public GameTeam(TeamColor color) {
        this.color = color;
    }

    public TeamColor getColor() {
        return color;
    }

    /**
     * @return the team name in its colour, for chat, sidebar and titles
     */
    public Component getDisplayName() {
        return Component.text(color.getDisplayName(), color.getTextColor());
    }

    public List<GamePlayer> getMembers() {
        return List.copyOf(members);
    }

    /**
     * @return the members who are standing up right now
     */
    public List<GamePlayer> getAliveMembers() {
        return members.stream().filter(GamePlayer::isAlive).toList();
    }

    /**
     * @return the members who are still part of the round, dead or alive
     */
    public List<GamePlayer> getPlayingMembers() {
        return members.stream().filter(GamePlayer::isPlaying).toList();
    }

    public int size() {
        return members.size();
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    /**
     * @param teamSize how many players the mode allows per team
     * @return whether there is no room left
     */
    public boolean isFull(int teamSize) {
        return members.size() >= teamSize;
    }

    public boolean contains(GamePlayer player) {
        return members.contains(player);
    }

    /**
     * Puts a player into this team, taking them out of the one they were in.
     *
     * @param player who joins
     */
    public void add(GamePlayer player) {
        GameTeam previous = player.getTeam();
        if (previous != null && previous != this) previous.remove(player);
        if (!members.contains(player)) members.add(player);
        player.setTeam(this);
    }

    /**
     * @param player who leaves
     */
    public void remove(GamePlayer player) {
        members.remove(player);
        if (player.getTeam() == this) player.setTeam(null);
    }

    public boolean isBedAlive() {
        return bedAlive;
    }

    public void setBedAlive(boolean bedAlive) {
        this.bedAlive = bedAlive;
    }

    public boolean isEliminated() {
        return eliminated;
    }

    public void setEliminated(boolean eliminated) {
        this.eliminated = eliminated;
    }

    /**
     * @return whether this team can still win: not out, and somebody is still in it
     */
    public boolean isAlive() {
        return !eliminated && !getPlayingMembers().isEmpty();
    }

    /**
     * @return whether the team is out right now: no bed and nobody left standing
     */
    public boolean shouldBeEliminated() {
        return !bedAlive && getPlayingMembers().isEmpty();
    }

    /**
     * @param upgradeId the upgrade to look up
     * @return the level this team has, 0 when it never bought it
     */
    public int getUpgradeLevel(String upgradeId) {
        return upgrades.getOrDefault(upgradeId, 0);
    }

    /**
     * @param upgradeId the upgrade
     * @param level     the level the team has reached
     */
    public void setUpgradeLevel(String upgradeId, int level) {
        upgrades.put(upgradeId, Math.max(0, level));
    }

    public Map<String, Integer> getUpgrades() {
        return Map.copyOf(upgrades);
    }

    @Override
    public String toString() {
        return color.name() + "[" + members.size() + (bedAlive ? ", bed" : ", no bed") + "]";
    }
}

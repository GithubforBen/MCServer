package de.schnorrenbergers.bedwars.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * One participant, for as long as the round lasts.
 * <p>
 * Kept by uuid rather than by {@link Player}, because a player who reconnects is a different object but
 * the same participant - their team, their kills and whether they are out have to survive that.
 */
public class GamePlayer {

    /**
     * Where a participant stands right now.
     */
    public enum State {
        /** Playing. */
        ALIVE,
        /** Dead with a bed still standing, on the way back. */
        RESPAWNING,
        /** Out for good, or never played. */
        SPECTATOR
    }

    private final UUID uuid;
    private final String name;

    private GameTeam team;
    private State state = State.SPECTATOR;

    private int kills;
    private int finalKills;
    private int deaths;
    private int bedsBroken;
    /** Counts down while {@link State#RESPAWNING}. */
    private int respawnTicks;

    public GamePlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    /**
     * @return the player behind this, or {@code null} while they are offline
     */
    public @Nullable Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public boolean isOnline() {
        return getPlayer() != null;
    }

    public @Nullable GameTeam getTeam() {
        return team;
    }

    public void setTeam(@Nullable GameTeam team) {
        this.team = team;
    }

    public boolean hasTeam() {
        return team != null;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public boolean isAlive() {
        return state == State.ALIVE;
    }

    public boolean isSpectator() {
        return state == State.SPECTATOR;
    }

    /**
     * @return whether they are still part of the round, whether or not they are standing up right now
     */
    public boolean isPlaying() {
        return state != State.SPECTATOR && team != null;
    }

    public int getRespawnTicks() {
        return respawnTicks;
    }

    public void setRespawnTicks(int respawnTicks) {
        this.respawnTicks = Math.max(0, respawnTicks);
    }

    public int getKills() {
        return kills;
    }

    public int getFinalKills() {
        return finalKills;
    }

    /**
     * @param finalKill whether the victim is out for good
     */
    public void addKill(boolean finalKill) {
        kills++;
        if (finalKill) finalKills++;
    }

    public int getDeaths() {
        return deaths;
    }

    public void addDeath() {
        deaths++;
    }

    public int getBedsBroken() {
        return bedsBroken;
    }

    public void addBedBroken() {
        bedsBroken++;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof GamePlayer player && uuid.equals(player.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public String toString() {
        return name + (team == null ? "" : " (" + team.getColor() + ")");
    }
}

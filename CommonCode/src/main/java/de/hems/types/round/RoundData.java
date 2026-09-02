package de.hems.types.round;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A round somebody put up themselves: who owns it, what it plays, and which server it ended up on.
 * <p>
 * This is to a self started round what an event is to an event round - the thing that outlives the server
 * and ties it to a person. The bedwars server looks itself up in here by its own name when it starts, which
 * is how it learns which map to load and how big its teams are; the lobby reads the same entry to show the
 * round in the list and to decide who may change it.
 */
public class RoundData implements Serializable {

    private static final long serialVersionUID = 4701L;

    private UUID id = UUID.randomUUID();
    private String serverName;
    private UUID ownerId;
    private String ownerName;
    private String map;
    private int teamSize = 2;
    private ArrayList<String> addons = new ArrayList<>();
    private boolean open = true;
    private ArrayList<String> invited = new ArrayList<>();
    private RoundState state = RoundState.PREPARING;
    private long createdAt = System.currentTimeMillis();
    private long endedAt;
    private int players;

    public RoundData() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * @return the server the round runs on, {@code null} while it is still being named
     */
    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    /**
     * @return who started it, and is therefore its round admin
     */
    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    /**
     * @return the map to play, {@code null} for whatever the server would pick itself
     */
    public String getMap() {
        return map;
    }

    public void setMap(String map) {
        this.map = map;
    }

    /**
     * @return how many players fit into one team
     */
    public int getTeamSize() {
        return teamSize;
    }

    public void setTeamSize(int teamSize) {
        this.teamSize = Math.max(1, Math.min(8, teamSize));
    }

    /**
     * The mode a team size means, in the ids of {@code modes.yml} on the bedwars server.
     *
     * @return the id of the mode to play
     */
    public String getMode() {
        return switch (getTeamSize()) {
            case 1 -> "solo";
            case 2 -> "doubles";
            case 3 -> "trio";
            default -> "quad";
        };
    }

    /**
     * @return the addons that are on, everything else is off
     */
    public Set<String> getAddons() {
        return new LinkedHashSet<>(addons == null ? List.of() : addons);
    }

    public void setAddons(Set<String> addons) {
        this.addons = new ArrayList<>(addons == null ? Set.of() : addons);
    }

    /**
     * @return whether anybody may join, or only the people the owner invites
     */
    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    /**
     * @return who the owner has let in on a closed round
     */
    public Set<UUID> getInvited() {
        Set<UUID> guests = new LinkedHashSet<>();
        for (String raw : invited == null ? List.<String>of() : invited) {
            try {
                guests.add(UUID.fromString(raw));
            } catch (IllegalArgumentException ignored) {
                // one unreadable entry costs one guest, not the guest list
            }
        }
        return guests;
    }

    public void setInvited(Set<UUID> guests) {
        this.invited = new ArrayList<>();
        if (guests == null) return;
        for (UUID guest : guests) {
            if (guest != null) invited.add(guest.toString());
        }
    }

    /**
     * Lets somebody into a closed round.
     *
     * @param player who may come
     * @return whether they were not already invited
     */
    public boolean invite(UUID player) {
        if (player == null) return false;
        if (invited == null) invited = new ArrayList<>();
        String key = player.toString();
        if (invited.contains(key)) return false;
        invited.add(key);
        return true;
    }

    /**
     * Whether somebody may go to this round.
     * <p>
     * An open round lets anybody in and a closed one only its owner and their guests. Without this,
     * "private" would be nothing but not being listed - and a server name is easy to guess and easy to
     * type into /warp.
     *
     * @param player who wants in
     * @return whether they may
     */
    public boolean isAllowed(UUID player) {
        if (open) return true;
        if (player == null) return false;
        return isOwner(player) || getInvited().contains(player);
    }

    public RoundState getState() {
        return state == null ? RoundState.PREPARING : state;
    }

    public void setState(RoundState state) {
        this.state = state;
        if (state == RoundState.ENDED && endedAt == 0L) endedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(long endedAt) {
        this.endedAt = endedAt;
    }

    /**
     * @return how many players were on it when it last reported in
     */
    public int getPlayers() {
        return players;
    }

    public void setPlayers(int players) {
        this.players = players;
    }

    /**
     * @param player somebody
     * @return whether this is their round
     */
    public boolean isOwner(UUID player) {
        return ownerId != null && ownerId.equals(player);
    }

    /**
     * @return a copy that can be changed without touching the one in the local cache
     */
    public RoundData copy() {
        RoundData copy = new RoundData();
        copy.id = id;
        copy.serverName = serverName;
        copy.ownerId = ownerId;
        copy.ownerName = ownerName;
        copy.map = map;
        copy.teamSize = teamSize;
        copy.addons = new ArrayList<>(addons == null ? List.of() : addons);
        copy.open = open;
        copy.invited = new ArrayList<>(invited == null ? List.of() : invited);
        copy.state = state;
        copy.createdAt = createdAt;
        copy.endedAt = endedAt;
        copy.players = players;
        return copy;
    }
}

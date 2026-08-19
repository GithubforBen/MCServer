package de.hems.types.admin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * What the admin website knows about one player.
 * <p>
 * Assembled on the server the player is on, because only there is the live state available.
 */
public class PlayerSnapshot implements Serializable {

    private static final long serialVersionUID = 3003L;

    private UUID uuid;
    private String name;
    /** The server the player is on, or the last one that saw them. */
    private String server;
    private boolean online;
    private double health;
    private double maxHealth;
    private int foodLevel;
    private String gameMode;
    private int level;
    private String world;
    private int x;
    private int y;
    private int z;
    private long firstPlayed;
    private long lastSeen;
    private boolean op;
    private boolean banned;
    private int viewDistance;
    private boolean paying;
    /** The backpacks this player has, empty when no backpack system is installed. */
    private List<BackpackInfo> backpacks = new ArrayList<>();

    public PlayerSnapshot() {
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
    }

    public int getFoodLevel() {
        return foodLevel;
    }

    public void setFoodLevel(int foodLevel) {
        this.foodLevel = foodLevel;
    }

    public String getGameMode() {
        return gameMode;
    }

    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public long getFirstPlayed() {
        return firstPlayed;
    }

    public void setFirstPlayed(long firstPlayed) {
        this.firstPlayed = firstPlayed;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public boolean isOp() {
        return op;
    }

    public void setOp(boolean op) {
        this.op = op;
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    public int getViewDistance() {
        return viewDistance;
    }

    public void setViewDistance(int viewDistance) {
        this.viewDistance = viewDistance;
    }

    public boolean isPaying() {
        return paying;
    }

    public void setPaying(boolean paying) {
        this.paying = paying;
    }

    public List<BackpackInfo> getBackpacks() {
        return backpacks;
    }

    public void setBackpacks(List<BackpackInfo> backpacks) {
        this.backpacks = backpacks == null ? new ArrayList<>() : backpacks;
    }

    /**
     * One backpack a player owns.
     *
     * @param id    how the backpack is addressed when its contents are requested
     * @param title what to call it in the interface
     * @param size  how many slots it has
     */
    public record BackpackInfo(String id, String title, int size) implements Serializable {
        private static final long serialVersionUID = 3004L;
    }
}

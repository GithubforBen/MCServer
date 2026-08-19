package de.hems.types.admin;

import java.io.Serializable;

/**
 * What the website asks CoreProtect for.
 * <p>
 * Mirrors the options of the {@code /co lookup} command, so anything that can be looked up in game can be
 * looked up in the browser.
 */
public class LookupQuery implements Serializable {

    private static final long serialVersionUID = 3006L;

    /** Which of CoreProtect's lookups to run. */
    public enum Kind {
        /** Block placements and breaks around a location. */
        BLOCK,
        /** Items taken out of and put into containers. */
        CONTAINER,
        /** Items picked up and dropped. */
        ITEM,
        /** Player inventory changes. */
        INVENTORY,
        /** Logins and logouts. */
        SESSION,
        /** Chat messages. */
        CHAT,
        /** Commands that were run. */
        COMMAND,
        /** Signs that were written. */
        SIGN,
        /** Name changes. */
        USERNAME
    }

    private Kind kind = Kind.BLOCK;
    private String user;
    /** How far back to look, in seconds. */
    private int timeSeconds = 3600;
    private String world;
    private int x;
    private int y;
    private int z;
    private int radius;
    private boolean hasLocation;
    private int limit = 100;
    private int offset;

    public LookupQuery() {
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind == null ? Kind.BLOCK : kind;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public int getTimeSeconds() {
        return timeSeconds;
    }

    public void setTimeSeconds(int timeSeconds) {
        this.timeSeconds = timeSeconds;
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

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public boolean hasLocation() {
        return hasLocation;
    }

    public void setHasLocation(boolean hasLocation) {
        this.hasLocation = hasLocation;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }
}

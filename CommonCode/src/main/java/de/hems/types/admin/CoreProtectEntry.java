package de.hems.types.admin;

import java.io.Serializable;

/**
 * One row of a CoreProtect lookup, flattened so it survives the trip to the browser.
 */
public class CoreProtectEntry implements Serializable {

    private static final long serialVersionUID = 3005L;

    private long timestamp;
    private String player;
    private String action;
    private String target;
    private String world;
    private int x;
    private int y;
    private int z;
    private boolean rolledBack;
    /** Free form extra, for example the message of a chat lookup. */
    private String detail;

    public CoreProtectEntry() {
    }

    public CoreProtectEntry(long timestamp, String player, String action, String target, String world,
                            int x, int y, int z, boolean rolledBack, String detail) {
        this.timestamp = timestamp;
        this.player = player;
        this.action = action;
        this.target = target;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.rolledBack = rolledBack;
        this.detail = detail;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getPlayer() {
        return player;
    }

    public String getAction() {
        return action;
    }

    public String getTarget() {
        return target;
    }

    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public boolean isRolledBack() {
        return rolledBack;
    }

    public String getDetail() {
        return detail;
    }
}

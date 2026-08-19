package de.hems.types.team;

import java.io.Serializable;

/**
 * The shared backpack of a team.
 * <p>
 * The contents travel as the bytes bukkit serialises items into, so enchantments, custom names and plugin
 * data survive being stored on a node that has no bukkit at all.
 */
public class BackpackData implements Serializable {

    private static final long serialVersionUID = 4003L;

    /** One chest. */
    public static final int SINGLE_CHEST = 27;
    /** A double chest, what a team gets when most of its members pay. */
    public static final int DOUBLE_CHEST = 54;

    private String teamName;
    private int size = SINGLE_CHEST;
    /** What {@code ItemStack.serializeItemsAsBytes()} produced, or {@code null} for an empty backpack. */
    private byte[] contents;
    /** Bumped on every save, so a stale write can be refused instead of silently winning. */
    private long revision;

    public BackpackData() {
    }

    public BackpackData(String teamName, int size, byte[] contents, long revision) {
        this.teamName = teamName;
        this.size = size;
        this.contents = contents;
        this.revision = revision;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public byte[] getContents() {
        return contents;
    }

    public void setContents(byte[] contents) {
        this.contents = contents;
    }

    public long getRevision() {
        return revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }
}

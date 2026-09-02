package de.hems.types.discord;

import java.io.Serializable;
import java.util.UUID;

/**
 * One minecraft account and the discord account behind it.
 * <p>
 * The point of it is small and practical: somebody is causing trouble, or has to be told something, and
 * the only handle anybody has is a minecraft name. This turns that into a person you can actually write
 * to.
 * <p>
 * Both names travel with the link and both can be out of date - people rename. The ids are what the link
 * actually is; the names are there so a lookup can be read out loud without asking mojang first.
 */
public class AccountLink implements Serializable {

    private static final long serialVersionUID = 4901L;

    private String discordId;
    private String discordName;
    private UUID minecraftId;
    private String minecraftName;
    private long linkedAt;

    public AccountLink() {
    }

    public AccountLink(String discordId, String discordName, UUID minecraftId, String minecraftName) {
        this.discordId = discordId;
        this.discordName = discordName;
        this.minecraftId = minecraftId;
        this.minecraftName = minecraftName;
        this.linkedAt = System.currentTimeMillis();
    }

    public String getDiscordId() {
        return discordId;
    }

    public void setDiscordId(String discordId) {
        this.discordId = discordId;
    }

    /** @return how the discord account was called when it was linked */
    public String getDiscordName() {
        return discordName;
    }

    public void setDiscordName(String discordName) {
        this.discordName = discordName;
    }

    public UUID getMinecraftId() {
        return minecraftId;
    }

    public void setMinecraftId(UUID minecraftId) {
        this.minecraftId = minecraftId;
    }

    /** @return how the minecraft account was called when it was linked */
    public String getMinecraftName() {
        return minecraftName;
    }

    public void setMinecraftName(String minecraftName) {
        this.minecraftName = minecraftName;
    }

    public long getLinkedAt() {
        return linkedAt;
    }

    public void setLinkedAt(long linkedAt) {
        this.linkedAt = linkedAt;
    }

    /**
     * @return how to write the discord side of it in a message, {@code @name (id)}
     */
    public String describeDiscord() {
        if (discordName == null || discordName.isBlank()) return discordId;
        return "@" + discordName + " (" + discordId + ")";
    }
}

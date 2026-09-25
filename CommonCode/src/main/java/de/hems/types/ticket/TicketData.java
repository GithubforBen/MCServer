package de.hems.types.ticket;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One ticket, as it travels between the launcher, the game servers, discord and the website.
 * <p>
 * The launcher owns them. A ticket belongs to a player, who is known by their minecraft account, their
 * discord account or both - whichever they wrote from, and the account link fills in the other one where it
 * exists. That is what lets somebody open a ticket in the game and read the answer on discord, or the other
 * way round.
 */
public class TicketData implements Serializable {

    private static final long serialVersionUID = 4500L;

    /** The longest a title may be. */
    public static final int MAX_TITLE = 100;
    /** The longest a message may be - what a discord embed can still show in one piece. */
    public static final int MAX_TEXT = 1500;

    private int id;
    private TicketType type;
    private String title;
    private TicketStatus status;
    private long createdAt;
    private long updatedAt;
    private String authorName;
    private UUID minecraftId;
    private String discordId;
    /** The admin who took it on, or {@code null}. */
    private String assignee;
    /** What the ticket refers to, like the admin action it asks about, or {@code null}. */
    private String context;
    /** The discord thread the admins talk about it in, or {@code null}. */
    private String threadId;
    private ArrayList<TicketMessage> messages = new ArrayList<>();
    /** How many messages the author has seen, which is what "new answer" is measured against. */
    private int seenByAuthor;

    public TicketData() {
    }

    /**
     * @return a ticket of its own with the same content - messages are never changed, so they are shared
     */
    public TicketData copy() {
        TicketData copy = new TicketData();
        copy.id = id;
        copy.type = type;
        copy.title = title;
        copy.status = status;
        copy.createdAt = createdAt;
        copy.updatedAt = updatedAt;
        copy.authorName = authorName;
        copy.minecraftId = minecraftId;
        copy.discordId = discordId;
        copy.assignee = assignee;
        copy.context = context;
        copy.threadId = threadId;
        copy.messages = new ArrayList<>(messages);
        copy.seenByAuthor = seenByAuthor;
        return copy;
    }

    /**
     * @return the first message, which is what the ticket is about
     */
    public String getDescription() {
        return messages.isEmpty() ? "" : messages.getFirst().getText();
    }

    /**
     * @return how many answers from admins the author has not seen yet
     */
    public int getUnseenAnswers() {
        int unseen = 0;
        for (int i = Math.max(0, seenByAuthor); i < messages.size(); i++) {
            if (messages.get(i).isStaff()) unseen++;
        }
        return unseen;
    }

    /**
     * @param player a minecraft account
     * @return whether the ticket is theirs
     */
    public boolean belongsTo(UUID player) {
        return player != null && player.equals(minecraftId);
    }

    /**
     * @return the ticket number the way it is written everywhere, like "#12"
     */
    public String getLabel() {
        return "#" + id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public TicketType getType() {
        return type;
    }

    public void setType(TicketType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public UUID getMinecraftId() {
        return minecraftId;
    }

    public void setMinecraftId(UUID minecraftId) {
        this.minecraftId = minecraftId;
    }

    public String getDiscordId() {
        return discordId;
    }

    public void setDiscordId(String discordId) {
        this.discordId = discordId;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public List<TicketMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<TicketMessage> messages) {
        this.messages = new ArrayList<>(messages);
    }

    public int getSeenByAuthor() {
        return seenByAuthor;
    }

    public void setSeenByAuthor(int seenByAuthor) {
        this.seenByAuthor = seenByAuthor;
    }

    /**
     * @param text what somebody typed
     * @param max  the longest it may be
     * @return it trimmed and cut to the length, never {@code null}
     */
    public static String clip(String text, int max) {
        if (text == null) return "";
        String trimmed = text.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max - 1) + "…";
    }
}

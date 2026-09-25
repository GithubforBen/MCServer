package de.hems.utils.ticket;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.ticket.TicketUpdatedEvent;
import de.hems.types.discord.AccountLink;
import de.hems.types.ticket.TicketData;
import de.hems.types.ticket.TicketMessage;
import de.hems.types.ticket.TicketSource;
import de.hems.types.ticket.TicketStatus;
import de.hems.types.ticket.TicketType;
import de.hems.utils.bot.verification.AccountLinkStore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Everything that happens to a ticket goes through here - from discord, from the game and from the website.
 * <p>
 * That is the whole point of the rework: the three places used to be one (discord), and every change is now
 * made once and then passed on to all of them. Somebody who opens a ticket in the game reads the answer an
 * admin wrote on discord; an admin who answers on the website is seen in the discord thread; and the account
 * link fills in the other half of a player so both places know who they are.
 * <p>
 * Listeners get each change after it is stored. Discord is one of them; the network - which tells the
 * game servers - is another.
 */
public class TicketService {

    /** Something that wants to hear about every change. */
    public interface Listener {
        /**
         * @param ticket the ticket as it is stored now
         * @param change what happened
         * @param source where it happened, so a listener does not echo a change back to where it came from
         * @param byStaff whether an admin made it - the player does not need to be told what they did themselves
         */
        void changed(TicketData ticket, TicketUpdatedEvent.Change change, TicketSource source, boolean byStaff);
    }

    private final TicketStore store;
    private final AccountLinkStore links;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    public TicketService(TicketStore store, AccountLinkStore links) {
        this.store = store;
        this.links = links;
        // the game servers hear about everything through the network
        addListener((ticket, change, source, byStaff) -> {
            try {
                ListenerAdapter.sendListeners(new TicketUpdatedEvent(ticket, change, byStaff));
            } catch (Exception e) {
                System.out.println("Could not announce ticket " + ticket.getLabel() + ": " + e.getMessage());
            }
        });
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public TicketStore getStore() {
        return store;
    }

    /**
     * Opens a ticket.
     *
     * @param type        what it is about
     * @param title       its title
     * @param text        what it says
     * @param authorName  who opened it, as a name
     * @param minecraftId their minecraft account, or {@code null}
     * @param discordId   their discord account, or {@code null}
     * @param context     what it refers to, or {@code null}
     * @param source      where it was opened
     * @return the ticket
     */
    public synchronized TicketData create(TicketType type, String title, String text, String authorName,
                                          UUID minecraftId, String discordId, String context, TicketSource source) {
        // the other half of the player from the account link, so the answer reaches them wherever they are
        AccountLink link = minecraftId != null ? links.get(minecraftId) : links.byDiscord(discordId);
        if (link != null) {
            if (minecraftId == null) minecraftId = link.getMinecraftId();
            if (discordId == null) discordId = link.getDiscordId();
        }
        long now = System.currentTimeMillis();
        TicketData ticket = new TicketData();
        ticket.setId(store.nextId());
        ticket.setType(type == null ? TicketType.OTHER : type);
        ticket.setTitle(TicketData.clip(title, TicketData.MAX_TITLE));
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);
        ticket.setAuthorName(authorName);
        ticket.setMinecraftId(minecraftId);
        ticket.setDiscordId(discordId);
        ticket.setContext(context);
        ticket.getMessages().add(new TicketMessage(authorName, false, source,
                TicketData.clip(text, TicketData.MAX_TEXT), now));
        ticket.setSeenByAuthor(1);
        store.put(ticket);
        System.out.println("Ticket " + ticket.getLabel() + " opened by " + authorName + " (" + source.getTitle() + ")");
        notify(ticket, TicketUpdatedEvent.Change.CREATED, source, false);
        return ticket;
    }

    /**
     * Writes in a ticket. A reply from the player reopens a closed ticket - they have something to add, and
     * a closed ticket nobody reads is the wrong place for it.
     *
     * @param id     the ticket
     * @param author who writes
     * @param staff  whether an admin writes
     * @param text   what
     * @param source where
     * @return the ticket, or {@code null} if there is none by that number
     */
    public synchronized TicketData reply(int id, String author, boolean staff, String text, TicketSource source) {
        TicketData ticket = store.get(id);
        if (ticket == null || text == null || text.isBlank()) return null;
        long now = System.currentTimeMillis();
        ticket.getMessages().add(new TicketMessage(author, staff, source, TicketData.clip(text, TicketData.MAX_TEXT), now));
        ticket.setUpdatedAt(now);
        if (staff) {
            if (ticket.getStatus() == TicketStatus.OPEN) ticket.setStatus(TicketStatus.IN_PROGRESS);
            if (ticket.getAssignee() == null) ticket.setAssignee(author);
        } else {
            ticket.setSeenByAuthor(ticket.getMessages().size());
            if (ticket.getStatus() == TicketStatus.CLOSED) ticket.setStatus(TicketStatus.OPEN);
        }
        store.put(ticket);
        notify(ticket, staff ? TicketUpdatedEvent.Change.STAFF_REPLY : TicketUpdatedEvent.Change.PLAYER_REPLY, source, staff);
        return ticket;
    }

    /**
     * @param id     the ticket
     * @param status what it is now
     * @param by     who changed it, for the log
     * @param staff  whether that is an admin
     * @param source where
     * @return the ticket, or {@code null}
     */
    public synchronized TicketData setStatus(int id, TicketStatus status, String by, boolean staff, TicketSource source) {
        TicketData ticket = store.get(id);
        if (ticket == null || ticket.getStatus() == status) return ticket;
        ticket.setStatus(status);
        ticket.setUpdatedAt(System.currentTimeMillis());
        store.put(ticket);
        System.out.println("Ticket " + ticket.getLabel() + " is " + status.getTitle() + " (" + by + ")");
        notify(ticket, TicketUpdatedEvent.Change.STATUS, source, staff);
        return ticket;
    }

    /**
     * An admin takes a ticket on.
     *
     * @param id     the ticket
     * @param admin  who
     * @param source where
     * @return the ticket, or {@code null}
     */
    public synchronized TicketData claim(int id, String admin, TicketSource source) {
        TicketData ticket = store.get(id);
        if (ticket == null) return null;
        ticket.setAssignee(admin);
        if (ticket.getStatus() == TicketStatus.OPEN) ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.setUpdatedAt(System.currentTimeMillis());
        store.put(ticket);
        notify(ticket, TicketUpdatedEvent.Change.CLAIMED, source, true);
        return ticket;
    }

    /**
     * Notes that the author has read everything so far.
     *
     * @param id the ticket
     */
    public synchronized void markSeen(int id) {
        TicketData ticket = store.get(id);
        if (ticket == null || ticket.getSeenByAuthor() == ticket.getMessages().size()) return;
        ticket.setSeenByAuthor(ticket.getMessages().size());
        store.put(ticket);
    }

    /**
     * Stores where the admins talk about a ticket. Not a change anybody has to hear about.
     *
     * @param id       the ticket
     * @param threadId the discord thread
     */
    public synchronized void setThread(int id, String threadId) {
        TicketData ticket = store.get(id);
        if (ticket == null) return;
        ticket.setThreadId(threadId);
        store.put(ticket);
    }

    /**
     * @param player a minecraft account
     * @return their tickets, newest change first
     */
    public List<TicketData> of(UUID player) {
        List<TicketData> mine = new ArrayList<>();
        for (TicketData ticket : store.all()) {
            if (ticket.belongsTo(player)) mine.add(ticket);
        }
        return mine;
    }

    /**
     * @param discordId a discord account
     * @return whether the ticket is theirs
     */
    public static boolean ownedByDiscord(TicketData ticket, String discordId) {
        return discordId != null && discordId.equals(ticket.getDiscordId());
    }

    /**
     * @return every ticket that is not closed, newest change first
     */
    public List<TicketData> open() {
        List<TicketData> open = new ArrayList<>();
        for (TicketData ticket : store.all()) {
            if (ticket.getStatus() != TicketStatus.CLOSED) open.add(ticket);
        }
        return open;
    }

    private void notify(TicketData ticket, TicketUpdatedEvent.Change change, TicketSource source, boolean byStaff) {
        for (Listener listener : listeners) {
            try {
                listener.changed(ticket, change, source, byStaff);
            } catch (RuntimeException e) {
                System.out.println("A ticket listener failed on " + ticket.getLabel() + ": " + e.getMessage());
            }
        }
    }
}

package de.hems.communication.events.ticket;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.types.ticket.TicketType;

import java.io.Serializable;
import java.util.UUID;

/**
 * Something a player or an admin does with tickets in the game. Answered with {@link RespondTicketEvent}.
 */
public class TicketRequestEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4502L;

    /** What is asked for. */
    public enum Action {
        /** Open a ticket: type, title, text. */
        CREATE,
        /** Write in a ticket: text. */
        REPLY,
        CLOSE,
        REOPEN,
        /** An admin takes a ticket on. */
        CLAIM,
        /** One ticket, with its messages; marks the answers as seen when the author reads it. */
        GET,
        /** The player's own tickets. */
        MINE,
        /** Every ticket that is not closed - for admins. */
        OPEN,
        /** The player's tickets with answers they have not seen. */
        UNSEEN
    }

    private Action action;
    private int ticketId;
    private UUID player;
    private String playerName;
    /** Whether the player is an admin, which the game server knows and the launcher trusts. */
    private boolean staff;
    private TicketType type;
    private String title;
    private String text;

    public TicketRequestEvent(Action action, int ticketId, UUID player, String playerName, boolean staff) {
        super(ListenerAdapter.ServerName.HOST);
        this.action = action;
        this.ticketId = ticketId;
        this.player = player;
        this.playerName = playerName;
        this.staff = staff;
    }

    public TicketRequestEvent() {
    }

    public TicketRequestEvent withContent(TicketType type, String title, String text) {
        this.type = type;
        this.title = title;
        this.text = text;
        return this;
    }

    public Action getAction() {
        return action;
    }

    public int getTicketId() {
        return ticketId;
    }

    public UUID getPlayer() {
        return player;
    }

    public String getPlayerName() {
        return playerName;
    }

    public boolean isStaff() {
        return staff;
    }

    public TicketType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }
}

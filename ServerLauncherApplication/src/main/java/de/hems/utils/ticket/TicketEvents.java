package de.hems.utils.ticket;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.ticket.RespondTicketEvent;
import de.hems.communication.events.ticket.TicketRequestEvent;
import de.hems.types.ticket.TicketData;
import de.hems.types.ticket.TicketSource;
import de.hems.types.ticket.TicketStatus;

import java.util.ArrayList;

/**
 * Answers what players and admins do with tickets in the game.
 * <p>
 * A player may only see and write in their own tickets; an admin in all of them. Whether somebody is an
 * admin is what their game server says - the same trust every other request from a game server gets.
 */
public class TicketEvents {

    private final TicketService tickets;

    public TicketEvents(TicketService tickets) {
        this.tickets = tickets;
        ListenerAdapter.register(TicketRequestEvent.class, event -> onRequest((TicketRequestEvent) event));
    }

    private void onRequest(TicketRequestEvent request) throws Exception {
        Object data = null;
        String error = null;
        switch (request.getAction()) {
            case CREATE -> {
                if (request.getTitle() == null || request.getTitle().isBlank()
                        || request.getText() == null || request.getText().isBlank()) {
                    error = "Titel und Beschreibung dürfen nicht leer sein.";
                } else {
                    data = tickets.create(request.getType(), request.getTitle(), request.getText(),
                            request.getPlayerName(), request.getPlayer(), null, null, TicketSource.GAME);
                }
            }
            case MINE -> data = new ArrayList<>(tickets.of(request.getPlayer()));
            case UNSEEN -> {
                ArrayList<TicketData> unseen = new ArrayList<>();
                for (TicketData ticket : tickets.of(request.getPlayer())) {
                    if (ticket.getUnseenAnswers() > 0) unseen.add(ticket);
                }
                data = unseen;
            }
            case OPEN -> {
                if (request.isStaff()) data = new ArrayList<>(tickets.open());
                else error = "Das dürfen nur Admins.";
            }
            default -> {
                TicketData ticket = tickets.getStore().get(request.getTicketId());
                boolean own = ticket != null && ticket.belongsTo(request.getPlayer());
                if (ticket == null || (!own && !request.isStaff())) {
                    error = "Dieses Ticket gibt es nicht - oder es ist nicht deins.";
                } else {
                    data = act(request, ticket, own);
                    if (data == null) error = "Das geht bei diesem Ticket nicht.";
                }
            }
        }
        ListenerAdapter.sendListeners(new RespondTicketEvent(request.getSender(), data, error, request.getEventId()));
    }

    private TicketData act(TicketRequestEvent request, TicketData ticket, boolean own) {
        int id = ticket.getId();
        String who = request.getPlayerName();
        // an admin writing in their own ticket writes as its author, not as staff
        boolean asStaff = request.isStaff() && !own;
        return switch (request.getAction()) {
            case GET -> {
                if (own) tickets.markSeen(id);
                yield tickets.getStore().get(id);
            }
            case REPLY -> tickets.reply(id, who, asStaff, request.getText(), TicketSource.GAME);
            case CLOSE -> tickets.setStatus(id, TicketStatus.CLOSED, who, asStaff, TicketSource.GAME);
            case REOPEN -> tickets.setStatus(id, TicketStatus.OPEN, who, asStaff, TicketSource.GAME);
            case CLAIM -> request.isStaff() ? tickets.claim(id, who, TicketSource.GAME) : null;
            default -> null;
        };
    }
}

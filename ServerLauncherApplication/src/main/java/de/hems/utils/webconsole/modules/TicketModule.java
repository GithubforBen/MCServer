package de.hems.utils.webconsole.modules;

import de.hems.Main;
import de.hems.types.ticket.TicketData;
import de.hems.types.ticket.TicketMessage;
import de.hems.types.ticket.TicketSource;
import de.hems.types.ticket.TicketStatus;
import de.hems.utils.ticket.TicketService;
import de.hems.utils.webconsole.ApiContext;
import de.hems.utils.webconsole.WebModule;
import de.hems.utils.webconsole.WebServer;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The tickets on the website - the same conversations as on discord and in the game. An answer written here
 * reaches the player in both, and shows up in the admins' discord thread.
 * <p>
 * An admin answers under the name they are logged in with.
 */
public class TicketModule implements WebModule {

    @Override
    public String getId() {
        return "tickets";
    }

    @Override
    public String getTitle() {
        return "Tickets";
    }

    @Override
    public String getDescription() {
        return "Tickets von Discord, aus dem Spiel und von hier: lesen, beantworten, übernehmen, schließen.";
    }

    @Override
    public void register(WebServer server) {
        server.get("/api/tickets", this::list);
        server.get("/api/tickets/{id}", this::get);
        server.post("/api/tickets/{id}/reply", this::reply);
        server.post("/api/tickets/{id}/status", this::status);
        server.post("/api/tickets/{id}/claim", this::claim);
    }

    private static TicketService tickets() {
        return Main.getInstance().getTicketService();
    }

    private void list(ApiContext ctx) {
        JSONArray array = new JSONArray();
        for (TicketData ticket : tickets().getStore().all()) array.put(summary(ticket));
        ctx.ok("tickets", array);
    }

    private void get(ApiContext ctx) {
        TicketData ticket = ticket(ctx);
        if (ticket == null) return;
        JSONArray messages = new JSONArray();
        for (TicketMessage message : ticket.getMessages()) {
            messages.put(new JSONObject()
                    .put("author", message.getAuthor())
                    .put("staff", message.isStaff())
                    .put("source", message.getSource().getTitle())
                    .put("text", message.getText())
                    .put("at", message.getAt()));
        }
        ctx.ok(summary(ticket)
                .put("context", ticket.getContext() == null ? "" : ticket.getContext())
                .put("linkedDiscord", ticket.getDiscordId() != null)
                .put("linkedMinecraft", ticket.getMinecraftId() != null)
                .put("messages", messages));
    }

    private void reply(ApiContext ctx) {
        TicketData ticket = ticket(ctx);
        if (ticket == null) return;
        String text = ctx.string("text", "").trim();
        if (text.isEmpty()) {
            ctx.error(400, "Die Antwort ist leer.");
            return;
        }
        tickets().reply(ticket.getId(), ctx.session().getUsername(), true, text, TicketSource.WEB);
        ctx.ok("Antwort an " + ticket.getAuthorName() + " geschickt.");
    }

    private void status(ApiContext ctx) {
        TicketData ticket = ticket(ctx);
        if (ticket == null) return;
        TicketStatus status;
        try {
            status = TicketStatus.valueOf(ctx.string("status", ""));
        } catch (IllegalArgumentException e) {
            ctx.error(400, "Diesen Status gibt es nicht.");
            return;
        }
        tickets().setStatus(ticket.getId(), status, ctx.session().getUsername(), true, TicketSource.WEB);
        ctx.ok("Ticket " + ticket.getLabel() + " ist jetzt " + status.getTitle() + ".");
    }

    private void claim(ApiContext ctx) {
        TicketData ticket = ticket(ctx);
        if (ticket == null) return;
        tickets().claim(ticket.getId(), ctx.session().getUsername(), TicketSource.WEB);
        ctx.ok("Du bearbeitest jetzt " + ticket.getLabel() + ".");
    }

    /**
     * @return the ticket the path names, or {@code null} after answering with an error
     */
    private static TicketData ticket(ApiContext ctx) {
        TicketData ticket = null;
        try {
            ticket = tickets().getStore().get(Integer.parseInt(ctx.pathParam("id")));
        } catch (NumberFormatException ignored) {
            // answered below
        }
        if (ticket == null) ctx.error(404, "Dieses Ticket gibt es nicht.");
        return ticket;
    }

    private static JSONObject summary(TicketData ticket) {
        return new JSONObject()
                .put("id", ticket.getId())
                .put("title", ticket.getTitle())
                .put("type", ticket.getType().getTitle())
                .put("status", ticket.getStatus().name())
                .put("statusTitle", ticket.getStatus().getTitle())
                .put("author", ticket.getAuthorName())
                .put("assignee", ticket.getAssignee() == null ? "" : ticket.getAssignee())
                .put("createdAt", ticket.getCreatedAt())
                .put("updatedAt", ticket.getUpdatedAt())
                .put("messages", ticket.getMessages().size())
                .put("source", ticket.getMessages().isEmpty() ? "" : ticket.getMessages().getFirst().getSource().getTitle());
    }
}

package de.hems.paper.ticket;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.ticket.RespondTicketEvent;
import de.hems.communication.events.ticket.TicketRequestEvent;
import de.hems.communication.events.ticket.TicketUpdatedEvent;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.paper.PaperContext;
import de.hems.types.ticket.TicketData;
import de.hems.types.ticket.TicketStatus;
import de.hems.types.ticket.TicketType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Tickets on a game server: asking the launcher, and telling the players on this server what happened to
 * theirs - an answer, a closed ticket - and the admins what is new.
 * <p>
 * Every server only tells its own players, so nobody hears the same thing twice.
 */
public final class TicketClient implements Listener {

    /** Who handles tickets in the game. Operators have it. */
    public static final String STAFF_PERMISSION = "network.tickets";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static boolean initialized;

    private TicketClient() {
    }

    /**
     * Starts listening. Called by {@code NetworkPlugin.connect} on every server.
     *
     * @param plugin the plugin of this server
     */
    public static synchronized void init(Plugin plugin) {
        if (initialized) return;
        initialized = true;
        PaperContext.setPlugin(plugin);
        ListenerAdapter.register(TicketUpdatedEvent.class,
                event -> PaperContext.sync(() -> onUpdate((TicketUpdatedEvent) event)));
        Bukkit.getPluginManager().registerEvents(new TicketClient(), plugin);
    }

    /** The answer of the launcher: what came back, or why not. */
    public record Answer(Object data, String error) {
        @SuppressWarnings("unchecked")
        public List<TicketData> list() {
            return data instanceof List<?> list ? (List<TicketData>) list : List.of();
        }

        public TicketData ticket() {
            return data instanceof TicketData ticket ? ticket : null;
        }
    }

    /**
     * Asks the launcher off the main thread and hands the answer back on it.
     *
     * @param request what to ask
     * @param answer  what to do with the answer, on the main thread
     */
    public static void ask(TicketRequestEvent request, Consumer<Answer> answer) {
        PaperContext.async(() -> {
            RespondDataEvent response = ListenerAdapter.ask(request, TIMEOUT);
            Answer result = response instanceof RespondTicketEvent ticket
                    ? new Answer(ticket.getData(), ticket.getError())
                    : new Answer(null, "Das Ticket-System antwortet gerade nicht. Versuch es gleich noch einmal.");
            PaperContext.sync(() -> answer.accept(result));
        });
    }

    /**
     * @return a request on behalf of this player
     */
    public static TicketRequestEvent request(Player player, TicketRequestEvent.Action action, int ticketId) {
        return new TicketRequestEvent(action, ticketId, player.getUniqueId(), player.getName(), isStaff(player));
    }

    public static boolean isStaff(Player player) {
        return player.hasPermission(STAFF_PERMISSION);
    }

    // ---- telling people --------------------------------------------------------------------------------------

    private static void onUpdate(TicketUpdatedEvent update) {
        TicketData ticket = update.getTicket();
        if (ticket == null) return;
        Player author = ticket.getMinecraftId() == null ? null : Bukkit.getPlayer(ticket.getMinecraftId());
        switch (update.getChange()) {
            case CREATED -> toStaff(ticket, author, Component.text("Neues Ticket " + ticket.getLabel() + " von "
                    + ticket.getAuthorName() + ": ", NamedTextColor.GOLD)
                    .append(Component.text(ticket.getTitle(), NamedTextColor.WHITE)));
            case STAFF_REPLY -> {
                if (author == null) return;
                var last = ticket.getMessages().getLast();
                author.sendMessage(Component.text("✉ " + last.getAuthor() + " hat auf dein Ticket "
                                + ticket.getLabel() + " geantwortet: ", NamedTextColor.AQUA)
                        .append(Component.text(TicketData.clip(last.getText(), 200), NamedTextColor.WHITE)));
                author.sendMessage(Component.text("  ").append(button("[Ansehen]", "/ticket " + ticket.getId(), false))
                        .append(Component.text(" "))
                        .append(button("[Antworten]", "/ticket " + ticket.getId() + " antworten", false)));
            }
            case PLAYER_REPLY -> {
                var last = ticket.getMessages().getLast();
                toStaff(ticket, author, Component.text(ticket.getLabel() + " · " + last.getAuthor() + ": ",
                        NamedTextColor.GOLD).append(Component.text(TicketData.clip(last.getText(), 200),
                        NamedTextColor.WHITE)));
            }
            case STATUS -> {
                if (author == null || !update.isByStaff()) return;
                author.sendMessage(Component.text("Dein Ticket " + ticket.getLabel() + " ist jetzt "
                                + ticket.getStatus().getTitle() + ".", NamedTextColor.AQUA)
                        .append(ticket.getStatus() == TicketStatus.CLOSED
                                ? Component.text(" Antworte einfach, falls doch noch etwas ist.", NamedTextColor.GRAY)
                                : Component.empty()));
            }
            case CLAIMED -> {
                if (author == null) return;
                author.sendMessage(Component.text(ticket.getAssignee() + " kümmert sich um dein Ticket "
                        + ticket.getLabel() + ".", NamedTextColor.AQUA));
            }
        }
    }

    /**
     * Tells the admins on this server - the one working on the ticket if they are here, all of them if nobody
     * is on it yet - but not the author about their own ticket.
     */
    private static void toStaff(TicketData ticket, Player author, Component message) {
        Component line = message.append(Component.text(" ")).append(button("[Ansehen]", "/ticket " + ticket.getId(), false));
        Player assignee = ticket.getAssignee() == null ? null : Bukkit.getPlayerExact(ticket.getAssignee());
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isStaff(player) || player.equals(author)) continue;
            if (assignee != null && !player.equals(assignee)) continue;
            player.sendMessage(line);
        }
    }

    /**
     * On joining: the answers the player has not read yet, and for admins how many tickets wait.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        // a little later, so it is not lost among the join messages
        Bukkit.getScheduler().runTaskLater(PaperContext.getPlugin(), () -> {
            if (Bukkit.getPlayer(id) == null) return;
            ask(request(player, TicketRequestEvent.Action.UNSEEN, 0), answer -> {
                for (TicketData ticket : answer.list()) {
                    player.sendMessage(Component.text("✉ Neue Antwort auf dein Ticket " + ticket.getLabel() + " ("
                                    + TicketData.clip(ticket.getTitle(), 40) + ") ", NamedTextColor.AQUA)
                            .append(button("[Ansehen]", "/ticket " + ticket.getId(), false)));
                }
            });
            if (!isStaff(player)) return;
            ask(request(player, TicketRequestEvent.Action.OPEN, 0), answer -> {
                long waiting = answer.list().stream().filter(t -> t.getStatus() == TicketStatus.OPEN).count();
                if (waiting == 0) return;
                player.sendMessage(Component.text(waiting + (waiting == 1 ? " Ticket wartet" : " Tickets warten")
                        + " auf einen Admin. ", NamedTextColor.GOLD).append(button("[Anzeigen]", "/ticket offen", false)));
            });
        }, 60L);
    }

    /**
     * @param label   what it says
     * @param command what clicking it does
     * @param suggest whether it only writes the command into the chat bar, for something to be added to it
     * @return a clickable piece of chat
     */
    static Component button(String label, String command, boolean suggest) {
        return Component.text(label, NamedTextColor.YELLOW)
                .clickEvent(suggest ? ClickEvent.suggestCommand(command) : ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(command.trim(), NamedTextColor.GRAY)));
    }

    /**
     * @param status a status
     * @return its colour in chat
     */
    static NamedTextColor colour(TicketStatus status) {
        return switch (status) {
            case OPEN -> NamedTextColor.GREEN;
            case IN_PROGRESS -> NamedTextColor.GOLD;
            case CLOSED -> NamedTextColor.GRAY;
        };
    }

    static TicketType parseType(String text) {
        for (TicketType type : TicketType.values()) {
            if (type.name().equalsIgnoreCase(text) || type.getTitle().equalsIgnoreCase(text)) return type;
        }
        return null;
    }
}

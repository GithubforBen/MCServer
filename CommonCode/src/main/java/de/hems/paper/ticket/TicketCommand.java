package de.hems.paper.ticket;

import de.hems.communication.events.ticket.TicketRequestEvent;
import de.hems.paper.util.ChatPrompt;
import de.hems.types.ticket.TicketData;
import de.hems.types.ticket.TicketMessage;
import de.hems.types.ticket.TicketType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static de.hems.paper.ticket.TicketClient.button;

/**
 * {@code /ticket}: tickets in the game - the same ones as on discord and on the website.
 * <ul>
 *     <li>{@code /ticket} - your tickets</li>
 *     <li>{@code /ticket neu} - write one</li>
 *     <li>{@code /ticket <nr>} - read one</li>
 *     <li>{@code /ticket <nr> antworten [text]} - write in it</li>
 *     <li>{@code /ticket <nr> schliessen|oeffnen}</li>
 *     <li>for admins: {@code /ticket offen} and {@code /ticket <nr> uebernehmen}</li>
 * </ul>
 */
public final class TicketCommand implements CommandExecutor, TabCompleter {

    /** How many messages of a conversation are shown; the rest is on discord and the website. */
    private static final int SHOWN = 10;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur im Spiel.");
            return true;
        }
        if (args.length == 0) {
            list(player, TicketRequestEvent.Action.MINE);
            return true;
        }
        String first = args[0].toLowerCase();
        if (first.equals("neu")) {
            create(player, args.length > 1 ? TicketClient.parseType(args[1]) : null);
            return true;
        }
        if (first.equals("offen")) {
            if (!TicketClient.isStaff(player)) {
                player.sendMessage(Component.text("Das dürfen nur Admins.", NamedTextColor.RED));
                return true;
            }
            list(player, TicketRequestEvent.Action.OPEN);
            return true;
        }
        int id;
        try {
            id = Integer.parseInt(first.startsWith("#") ? first.substring(1) : first);
        } catch (NumberFormatException e) {
            usage(player);
            return true;
        }
        if (args.length == 1) {
            show(player, id);
            return true;
        }
        switch (args[1].toLowerCase()) {
            case "antworten", "antwort" -> {
                if (args.length > 2) {
                    reply(player, id, String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
                } else {
                    ChatPrompt.ask(player, "Was möchtest du in Ticket #" + id + " schreiben?",
                            text -> reply(player, id, text));
                }
            }
            case "schliessen", "schließen" -> act(player, id, TicketRequestEvent.Action.CLOSE, "Ticket #" + id + " ist geschlossen.");
            case "oeffnen", "öffnen" -> act(player, id, TicketRequestEvent.Action.REOPEN, "Ticket #" + id + " ist wieder offen.");
            case "uebernehmen", "übernehmen" -> act(player, id, TicketRequestEvent.Action.CLAIM, "Du bearbeitest jetzt Ticket #" + id + ".");
            default -> usage(player);
        }
        return true;
    }

    private static void usage(Player player) {
        player.sendMessage(Component.text("/ticket · /ticket neu · /ticket <nr> [antworten <text>|schliessen|oeffnen]"
                + (TicketClient.isStaff(player) ? " · /ticket offen · /ticket <nr> uebernehmen" : ""), NamedTextColor.GRAY));
    }

    // ---- lists -----------------------------------------------------------------------------------------------

    private static void list(Player player, TicketRequestEvent.Action action) {
        TicketClient.ask(TicketClient.request(player, action, 0), answer -> {
            if (answer.error() != null) {
                player.sendMessage(Component.text(answer.error(), NamedTextColor.RED));
                return;
            }
            List<TicketData> tickets = answer.list();
            boolean mine = action == TicketRequestEvent.Action.MINE;
            player.sendMessage(Component.text(mine ? "— Deine Tickets —" : "— Offene Tickets —", NamedTextColor.GOLD));
            if (tickets.isEmpty()) {
                player.sendMessage(Component.text(mine ? "Du hast noch keine Tickets." : "Keine offenen Tickets.",
                        NamedTextColor.GRAY));
            }
            for (TicketData ticket : tickets) {
                Component line = button(ticket.getLabel(), "/ticket " + ticket.getId(), false)
                        .append(Component.text(" " + TicketData.clip(ticket.getTitle(), 40) + " ", NamedTextColor.WHITE))
                        .append(Component.text(ticket.getStatus().getTitle(), TicketClient.colour(ticket.getStatus())));
                if (!mine) line = line.append(Component.text(" · " + ticket.getAuthorName(), NamedTextColor.GRAY));
                if (mine && ticket.getUnseenAnswers() > 0) {
                    line = line.append(Component.text(" ✉ neu", NamedTextColor.AQUA));
                }
                player.sendMessage(line);
            }
            if (mine) player.sendMessage(button("[Neues Ticket]", "/ticket neu", false));
        });
    }

    // ---- one ticket ------------------------------------------------------------------------------------------

    private static void show(Player player, int id) {
        TicketClient.ask(TicketClient.request(player, TicketRequestEvent.Action.GET, id), answer -> {
            TicketData ticket = answer.ticket();
            if (ticket == null) {
                player.sendMessage(Component.text(answer.error() != null ? answer.error() : "Das Ticket gibt es nicht.",
                        NamedTextColor.RED));
                return;
            }
            SimpleDateFormat time = new SimpleDateFormat("dd.MM. HH:mm");
            player.sendMessage(Component.text("— " + ticket.getLabel() + " · " + ticket.getTitle() + " —", NamedTextColor.GOLD));
            player.sendMessage(Component.text(ticket.getType().getTitle() + " · ", NamedTextColor.GRAY)
                    .append(Component.text(ticket.getStatus().getTitle(), TicketClient.colour(ticket.getStatus())))
                    .append(Component.text(" · von " + ticket.getAuthorName()
                            + (ticket.getAssignee() != null ? " · bearbeitet von " + ticket.getAssignee() : ""),
                            NamedTextColor.GRAY)));
            if (ticket.getContext() != null) {
                player.sendMessage(Component.text("Bezieht sich auf: " + ticket.getContext(), NamedTextColor.GRAY));
            }
            List<TicketMessage> messages = ticket.getMessages();
            if (messages.size() > SHOWN) {
                player.sendMessage(Component.text("… " + (messages.size() - SHOWN) + " ältere Nachrichten", NamedTextColor.DARK_GRAY));
            }
            for (TicketMessage message : messages.subList(Math.max(0, messages.size() - SHOWN), messages.size())) {
                player.sendMessage(Component.text(time.format(new Date(message.getAt())) + " ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(message.getAuthor() + (message.isStaff() ? " (Admin)" : "") + ": ",
                                message.isStaff() ? NamedTextColor.AQUA : NamedTextColor.GREEN))
                        .append(Component.text(message.getText(), NamedTextColor.WHITE)));
            }
            Component buttons = button("[Antworten]", "/ticket " + id + " antworten", false).append(Component.text(" "));
            buttons = buttons.append(ticket.getStatus() == de.hems.types.ticket.TicketStatus.CLOSED
                    ? button("[Wieder öffnen]", "/ticket " + id + " oeffnen", false)
                    : button("[Schließen]", "/ticket " + id + " schliessen", false));
            if (TicketClient.isStaff(player) && !ticket.belongsTo(player.getUniqueId())) {
                buttons = buttons.append(Component.text(" ")).append(button("[Übernehmen]", "/ticket " + id + " uebernehmen", false));
            }
            player.sendMessage(buttons);
        });
    }

    private static void reply(Player player, int id, String text) {
        if (text == null || text.isBlank()) return;
        TicketRequestEvent request = TicketClient.request(player, TicketRequestEvent.Action.REPLY, id)
                .withContent(null, null, text);
        TicketClient.ask(request, answer -> {
            if (answer.error() != null) player.sendMessage(Component.text(answer.error(), NamedTextColor.RED));
            else player.sendMessage(Component.text("✓ Gesendet.", NamedTextColor.GREEN));
        });
    }

    private static void act(Player player, int id, TicketRequestEvent.Action action, String done) {
        TicketClient.ask(TicketClient.request(player, action, id), answer -> {
            if (answer.error() != null) player.sendMessage(Component.text(answer.error(), NamedTextColor.RED));
            else player.sendMessage(Component.text("✓ " + done, NamedTextColor.GREEN));
        });
    }

    // ---- writing one -----------------------------------------------------------------------------------------

    private static void create(Player player, TicketType type) {
        if (type == null) {
            Component line = Component.text("Worum geht es? ", NamedTextColor.AQUA);
            for (TicketType value : TicketType.values()) {
                // a question about an admin action is asked on discord, under the log of it
                if (value == TicketType.ADMIN_ACTION) continue;
                line = line.append(button("[" + value.getTitle() + "]", "/ticket neu " + value.name(), false))
                        .append(Component.text(" "));
            }
            player.sendMessage(line);
            return;
        }
        ChatPrompt.ask(player, "Titel deines Tickets (" + type.getTitle() + "), kurz:", title ->
                ChatPrompt.ask(player, "Beschreib jetzt, worum es geht - so genau wie möglich:", text -> {
                    TicketRequestEvent request = TicketClient.request(player, TicketRequestEvent.Action.CREATE, 0)
                            .withContent(type, title, text);
                    TicketClient.ask(request, answer -> {
                        TicketData ticket = answer.ticket();
                        if (ticket == null) {
                            player.sendMessage(Component.text(answer.error() != null ? answer.error()
                                    : "Das hat nicht geklappt.", NamedTextColor.RED));
                            return;
                        }
                        player.sendMessage(Component.text("✓ Ticket " + ticket.getLabel() + " ist angelegt. "
                                + "Du bekommst hier Bescheid, sobald ein Admin antwortet.", NamedTextColor.GREEN));
                        if (ticket.getDiscordId() == null) {
                            player.sendMessage(Component.text("Tipp: Mit /verify verknüpfst du deinen Discord-Account "
                                    + "und bekommst Antworten auch dort.", NamedTextColor.GRAY));
                        }
                    });
                }));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                                      @NotNull String[] args) {
        List<String> options = new ArrayList<>();
        boolean staff = sender instanceof Player player && TicketClient.isStaff(player);
        if (args.length == 1) {
            options.add("neu");
            if (staff) options.add("offen");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("neu")) {
            for (TicketType type : TicketType.values()) {
                if (type != TicketType.ADMIN_ACTION) options.add(type.name().toLowerCase());
            }
        } else if (args.length == 2) {
            options.addAll(List.of("antworten", "schliessen", "oeffnen"));
            if (staff) options.add("uebernehmen");
        }
        String typed = args[args.length - 1].toLowerCase();
        options.removeIf(option -> !option.startsWith(typed));
        return options;
    }
}

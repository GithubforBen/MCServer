package de.hems.utils.bot.tickets;

import de.hems.Main;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;
import java.util.UUID;

public class Tickets {
    public static void createTicket(Ticket ticket) {
        saveTicket(ticket);
        String string = Main.getInstance().getConfiguration().getConfig().getString("ticket-channel");
        if (string == null) return;
        TextChannel textChannelById = Main.getInstance().getJda().getTextChannelById(string);
        Guild x = textChannelById.getGuild();
        new Thread(() -> {
            x.loadMembers().get().forEach((y) -> {
                if (y.hasPermission(Permission.MESSAGE_MANAGE)) {
                    if (!y.getUser().isBot()) {
                        y.getUser().openPrivateChannel().queue((channel) -> channel.sendMessageEmbeds(ticket.getEmbed()).addComponents(
                                ActionRow.of(
                                        Button.primary("mark-" + ticket.getTicketId(), "mark ticket as in progress ticket"),
                                        Button.primary("close-" + ticket.getTicketId(), "close ticket"),
                                        Button.primary("respond-" + ticket.getTicketId(), "respond ticket")
                                )
                        ).queue());
                    }
                }
            });
        }).start();
    }

    public static void saveTicket(Ticket ticket) {
        YamlConfiguration config = Main.getInstance().getConfiguration().getConfig();
        int tickets = ticket.getTicketId();
        if (!config.contains("tickets")) config.set("tickets", List.of(tickets));
        else {
            List<Integer> tickets1 = config.getIntegerList("tickets");
            if (! tickets1.contains(tickets)) {
                tickets1.add(tickets);
                config.set("tickets", tickets1);
            }
        }
        config.set("ticket-" + tickets + ".title", ticket.getTitle());
        config.set("ticket-" + tickets + ".uuid", ticket.getTicketId());
        config.set("ticket-" + tickets + ".type", ticket.getType().toString());
        config.set("ticket-" + tickets + ".description", ticket.getContent());
        config.set("ticket-" + tickets + ".status", ticket.getStatus().toString());
        config.set("ticket-" + tickets + ".author", ticket.getAuthor());
        config.set("ticket-" + tickets + ".response", ticket.getResponse());
        Main.getInstance().getConfiguration().save();
    }

    public static Ticket getTicket(int uuid) {
        YamlConfiguration config = Main.getInstance().getConfiguration().getConfig();
        if (!config.contains("tickets")) return null;
        for (int tickets : config.getIntegerList("tickets")) {
            if (config.getInt("ticket-" + tickets + ".uuid") != uuid) continue;
            return new Ticket(
                    config.getString("ticket-" + tickets + ".title"),
                    Ticket.TicketType.valueOf(
                            config.getString("ticket-" + tickets + ".type")
                    ),
                    config.getString("ticket-" + tickets + ".author"),
                    config.getString("ticket-" + tickets + ".description"),
                    Ticket.Status.valueOf(config.getString("ticket-" + tickets + ".status")),
                    config.getInt("ticket-" + tickets + ".uuid"),
                    config.getString("ticket-" + tickets + ".response")
            );
        }
        return null;
    }

    public static void updateTicketChannel() {
        String string = Main.getInstance().getConfiguration().getConfig().getString("ticket-channel");
        if (string == null) return;
        TextChannel textChannelById = Main.getInstance().getJda().getTextChannelById(string);
        if (textChannelById == null) return;
        List<Message> messages = textChannelById.getHistory().retrievePast(50).complete();
        while (messages.size() > 2) {
            textChannelById.deleteMessages(messages).complete();
            messages = textChannelById.getHistory().retrievePast(50).complete();
        }
        if (!textChannelById.getHistory().retrievePast(10).complete().isEmpty()) return;
        EmbedBuilder embedBuilder = Main.getEmbedBuilder();
        embedBuilder.setTitle("Tickets");
        embedBuilder.addField("Click on the Button below to create a ticket", "<<write>>", true);

        textChannelById.sendMessageEmbeds(embedBuilder.build()).addComponents(
                        ActionRow.of(
                                Button.primary("createTicket", "Click here to create a ticket")))
                .queue();
    }
}

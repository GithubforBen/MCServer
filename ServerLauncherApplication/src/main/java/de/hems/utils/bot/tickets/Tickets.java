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
        YamlConfiguration config = Main.getInstance().getConfiguration().getConfig();
        if (!config.contains("tickets")) config.set("tickets", 1);
        else config.set("tickets", config.getInt("tickets") + 1);
        config.set("ticket-" + config.getInt("tickets") + ".uuid", ticket.getTicketId().toString());
        config.set("ticket-" + config.getInt("tickets") + ".type", ticket.getType().toString());
        config.set("ticket-" + config.getInt("tickets") + ".description", ticket.getContent());
        config.set("ticket-" + config.getInt("tickets") + ".status", ticket.getStatus().toString());
        config.set("ticket-" + config.getInt("tickets") + ".author", ticket.getAuthor());//TODO: add a way for admins to see this
        Main.getInstance().getConfiguration().save();
        String string = Main.getInstance().getConfiguration().getConfig().getString("ticket-channel");
        if (string == null) return;
        TextChannel textChannelById = Main.getInstance().getJda().getTextChannelById(string);
        Guild x = textChannelById.getGuild();
        x.getMembers().forEach((y) -> {
            if (y.hasPermission(Permission.ADMINISTRATOR)) {
                y.getUser().openPrivateChannel().queue((channel) -> channel.sendMessageEmbeds(ticket.getEmbed()).queue());
            }
        });
    }

    private static Ticket getTicket(UUID uuid) {
        YamlConfiguration config = Main.getInstance().getConfiguration().getConfig();
        if (!config.contains("tickets")) return null;
        for (int i = 0; i < config.getInt("tickets"); i++) {
            if (!config.getString("ticket-" + (i + 1) + ".uuid").equals(uuid.toString())) continue;
            return new Ticket(
                    Ticket.TicketType.valueOf(
                            config.getString("ticket-" + config.getInt("tickets") + ".type")
                    ),
                    config.getString("ticket-" + config.getInt("tickets") + ".author"),
                    config.getString("ticket-" + config.getInt("tickets") + ".description"),
                    Ticket.Status.valueOf(config.getString("ticket-" + config.getInt("tickets") + ".status")),
                    UUID.fromString(config.getString("ticket-" + config.getInt("tickets") + ".uuid"))
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
        while (!messages.isEmpty()) {
            textChannelById.deleteMessages(messages).complete();
            messages = textChannelById.getHistory().retrievePast(50).complete();
        }
        EmbedBuilder embedBuilder = Main.getEmbedBuilder();
        embedBuilder.setTitle("Tickets");
        embedBuilder.addField("Click on the Button below to create a ticket", "<<write>>", true);

        textChannelById.sendMessageEmbeds(embedBuilder.build()).addComponents(
                        ActionRow.of(
                                Button.primary("createTicket", "Click here to create a ticket")))
                .queue();
    }
}

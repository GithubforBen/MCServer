package de.hems.utils.tickets;

import de.hems.Main;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class Tickets {
    public static void updateTicketChannel() {
        String string = Main.getInstance().getConfiguration().getConfig().getString("ticket-channel");
        if (string == null) return;
        TextChannel textChannelById = Main.getInstance().getJda().getTextChannelById(string);
        if (textChannelById == null) return;
        while (!textChannelById.getLatestMessageId().equals("0")) {
            textChannelById.deleteMessageById(textChannelById.getLatestMessageId()).queue();
        }
        EmbedBuilder embedBuilder = Main.getEmbedBuilder();
        embedBuilder.setTitle("Tickets");
        embedBuilder.addField("Click on the Button below to create a ticket", "<<write>>", true);

        textChannelById.sendMessageEmbeds(embedBuilder.build()).addComponents(
                Container.of(
                        ActionRow.of(
                Button.primary("createTicket", "Click here to create a ticket")))
                ).queue();
    }
}

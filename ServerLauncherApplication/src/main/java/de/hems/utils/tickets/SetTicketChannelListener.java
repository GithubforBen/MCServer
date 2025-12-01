package de.hems.utils.tickets;

import de.hems.Main;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Properties;

public class SetTicketChannelListener extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("setticketchannel")) {
            return;
        }
        YamlConfiguration config = Main.getInstance().getConfiguration().getConfig();
        MessageChannelUnion channel = event.getChannel();
        config.set("ticket-channel", channel.getId());
        Main.getInstance().getConfiguration().save();
        event.reply("Ticket channel set to " + channel.getAsMention()).queue();
        Tickets.updateTicketChannel();
    }
}

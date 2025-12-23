package de.hems.utils.bot.adminabuse;

import de.hems.Main;
import de.hems.utils.bot.tickets.Tickets;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class SetLoggingChannel extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("setloggingchannel")) {
            return;
        }
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("You do not have permission to use this command!").queue();
            return;
        }
        YamlConfiguration config = Main.getInstance().getConfiguration().getConfig();
        MessageChannelUnion channel = event.getChannel();
        config.set("logging-channel", channel.getId());
        Main.getInstance().getConfiguration().save();
        event.reply("Logging channel set to " + channel.getAsMention()).complete();
    }
}

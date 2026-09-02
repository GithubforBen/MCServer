package de.hems.utils.bot.payingplayer;

import de.hems.Main;
import de.hems.api.UUIDFetcher;
import de.hems.utils.bot.tickets.Tickets;
import de.hems.utils.bot.verification.DiscordOwner;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class PayingPlayerCommand extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("payingplayer")) {
            return;
        }
        if (!DiscordOwner.is(event.getUser())) {
            event.reply("Diesen Befehl darf nur der Besitzer des Netzwerks benutzen.").setEphemeral(true).queue();
            return;
        }
        String minecraftname = event.getOption("minecraftname").getAsString();
        UUID uuidByName = UUIDFetcher.findUUIDByName(minecraftname, true);
        if (uuidByName == null) {
            event.reply("Player not found!").queue();
            return;
        }
        YamlConfiguration config = Main.getInstance().getConfiguration().getConfig();
        if (!config.contains("paying-players")) {
            config.set("paying-players", List.of(uuidByName.toString()));
        } else {
            List<String> uuids = config.getStringList("paying-players");
            if (!uuids.contains(uuidByName.toString())) {
                uuids.add(uuidByName.toString());
                config.set("paying-players", uuids);
            }
        }
        Main.getInstance().getConfiguration().save();
        event.reply("Player added!").queue();
    }
}

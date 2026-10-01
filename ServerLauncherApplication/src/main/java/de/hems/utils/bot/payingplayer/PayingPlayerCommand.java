package de.hems.utils.bot.payingplayer;

import de.hems.Main;
import de.hems.api.UUIDFetcher;
import de.hems.communication.events.configs.PayingPlayersChangedEvent;
import de.hems.utils.bot.verification.DiscordOwner;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * {@code /payingplayer <minecraftname>} on discord: puts a player on the list of those who pay for the
 * server, and tells every server at once.
 */
public class PayingPlayerCommand extends ListenerAdapter {

    private static final String CONFIG_KEY = "paying-players";

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
        // mojang can take longer than the three seconds discord waits for an answer, and the player was
        // then added while discord said the command had failed
        event.deferReply().queue();
        Main.getInstance().async(() -> {
            UUID uuid = UUIDFetcher.findUUIDByName(minecraftname, true);
            if (uuid == null) {
                event.getHook().sendMessage("Den Spieler **" + minecraftname + "** gibt es nicht.").queue();
                return;
            }
            boolean added;
            synchronized (PayingPlayerCommand.class) {
                YamlConfiguration config = Main.getInstance().getConfiguration().getConfig();
                List<String> uuids = config.getStringList(CONFIG_KEY);
                added = !uuids.contains(uuid.toString());
                if (added) {
                    uuids.add(uuid.toString());
                    config.set(CONFIG_KEY, uuids);
                    Main.getInstance().getConfiguration().save();
                }
            }
            announce();
            event.getHook().sendMessage(added
                    ? "**" + minecraftname + "** zahlt jetzt - alle Server wissen es sofort."
                    : "**" + minecraftname + "** steht schon auf der Liste.").queue();
        });
    }

    /**
     * Sends the current list to every server, so a change is there at once and not with the next refresh.
     */
    public static void announce() {
        List<String> uuids = Main.getInstance().getConfiguration().getConfig().getStringList(CONFIG_KEY);
        try {
            de.hems.communication.ListenerAdapter.sendListeners(new PayingPlayersChangedEvent(uuids));
        } catch (Exception e) {
            System.out.println("Could not announce the paying players: " + e.getMessage());
        }
    }
}

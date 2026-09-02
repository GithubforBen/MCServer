package de.hems.utils.bot.verification;

import de.hems.Main;
import de.hems.api.UUIDFetcher;
import de.hems.communication.events.admin.OpChangedEvent;
import de.hems.types.discord.AccountLink;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * {@code /op} and {@code /deop} on discord: handing out operator rights without editing a file.
 * <p>
 * Only the owner. Operator is every right there is, so it is not something a moderator role should be able
 * to hand around quietly - and the owner is the one account that has all rights anyway.
 * <p>
 * The name goes into the launcher's {@code ops} list, which is what every server is built with, and the
 * change is announced so the servers that are already running apply it without a restart.
 */
public class OpCommand extends ListenerAdapter {

    /** The key in the launcher config, the same one the servers are configured from. */
    private static final String CONFIG_KEY = "ops";

    private final AccountLinkStore links;

    public OpCommand(AccountLinkStore links) {
        this.links = links;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        boolean granting = event.getName().equals("op");
        if (!granting && !event.getName().equals("deop")) return;
        if (!DiscordOwner.is(event.getUser())) {
            event.reply("Operator-Rechte vergibt nur der Besitzer des Netzwerks.").setEphemeral(true).queue();
            return;
        }
        OptionMapping option = event.getOption("minecraftname");
        if (option == null) {
            event.reply("Sag dazu, wen du meinst: /" + event.getName() + " <minecraftname>")
                    .setEphemeral(true).queue();
            return;
        }
        String minecraftName = option.getAsString().trim();
        event.deferReply(true).queue();
        Main.getInstance().async(() -> {
            UUID uuid = UUIDFetcher.findUUIDByName(minecraftName, true);
            if (uuid == null) {
                event.getHook().sendMessage("Den Spieler **" + minecraftName + "** gibt es nicht.").queue();
                return;
            }
            boolean changed = granting ? add(minecraftName) : remove(minecraftName);
            if (!changed) {
                event.getHook().sendMessage("**" + minecraftName + "** war schon "
                        + (granting ? "Operator" : "keiner") + ".").queue();
                return;
            }
            announce(uuid, minecraftName, granting);
            AccountLink link = links.get(uuid);
            event.getHook().sendMessage("**" + minecraftName + "**"
                    + (link == null ? "" : " (" + link.describeDiscord() + ")")
                    + (granting ? " ist jetzt Operator." : " ist kein Operator mehr.")
                    + "\nAuf laufenden Servern gilt das sofort.").queue();
        });
    }

    /**
     * @param minecraftName the player to add
     * @return whether they were not already on the list
     */
    private synchronized boolean add(String minecraftName) {
        YamlConfiguration config = Main.getInstance().getConfiguration().getConfig();
        List<String> ops = new ArrayList<>(config.getStringList(CONFIG_KEY));
        for (String entry : ops) {
            if (entry.equalsIgnoreCase(minecraftName)) return false;
        }
        ops.add(minecraftName);
        config.set(CONFIG_KEY, ops);
        Main.getInstance().getConfiguration().save();
        return true;
    }

    /**
     * @param minecraftName the player to remove
     * @return whether they were on the list
     */
    private synchronized boolean remove(String minecraftName) {
        YamlConfiguration config = Main.getInstance().getConfiguration().getConfig();
        List<String> ops = new ArrayList<>(config.getStringList(CONFIG_KEY));
        String wanted = minecraftName.toLowerCase(Locale.ROOT);
        boolean removed = ops.removeIf(entry -> entry.toLowerCase(Locale.ROOT).equals(wanted));
        if (!removed) return false;
        config.set(CONFIG_KEY, ops);
        Main.getInstance().getConfiguration().save();
        return true;
    }

    /**
     * Tells the running servers, so nobody has to be restarted to become an operator.
     */
    private void announce(UUID player, String name, boolean operator) {
        try {
            de.hems.communication.ListenerAdapter.sendListeners(new OpChangedEvent(player, name, operator));
        } catch (Exception e) {
            System.out.println("Could not announce the operator change for " + name + ": " + e.getMessage()
                    + " - it will apply when the servers are next started.");
        }
    }
}

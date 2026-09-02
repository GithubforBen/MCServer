package de.hems.utils.bot.verification;

import de.hems.Main;
import de.hems.api.UUIDFetcher;
import de.hems.types.discord.AccountLink;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * {@code /verify <minecraftname>} on discord: the first half of linking an account.
 * <p>
 * It only hands out a code. The link is made when that code is typed in on the server, because that is the
 * only moment anybody has proved they are the account they claim to be. The reply is ephemeral - the code
 * is a key, and a key in a public channel is a key somebody else can use.
 */
public class OnAccountVerifyCommand extends ListenerAdapter {

    private final AccountLinkStore links;

    public OnAccountVerifyCommand(AccountLinkStore links) {
        this.links = links;
    }

    /**
     * Undoes a link.
     * <p>
     * Only the owner, and deliberately not the player themselves: the reason to look a link up is that
     * somebody has to be spoken to, and a link anybody could drop the moment it becomes inconvenient would
     * be worth nothing at exactly the moment it matters.
     */
    private void unlink(SlashCommandInteractionEvent event) {
        if (!DiscordOwner.is(event.getUser())) {
            event.reply("Eine Verknüpfung löst nur der Besitzer des Netzwerks.").setEphemeral(true).queue();
            return;
        }
        OptionMapping option = event.getOption("minecraftname");
        if (option == null) {
            event.reply("Sag dazu, wen du meinst: /unlink <minecraftname>").setEphemeral(true).queue();
            return;
        }
        String minecraftName = option.getAsString().trim();
        AccountLink link = links.byName(minecraftName);
        if (link == null) {
            event.reply("**" + minecraftName + "** ist mit keinem Discord-Account verknüpft.")
                    .setEphemeral(true).queue();
            return;
        }
        links.unlink(link.getMinecraftId());
        announceRemoval(link);
        event.reply("**" + link.getMinecraftName() + "** und " + link.describeDiscord()
                + " sind nicht mehr verknüpft.").setEphemeral(true).queue();
    }

    /**
     * Tells the servers, so a lookup a second later does not still show the link that was just dropped.
     */
    private void announceRemoval(AccountLink link) {
        try {
            de.hems.communication.ListenerAdapter.sendListeners(
                    new de.hems.communication.events.discord.AccountLinkUpdatedEvent(
                            link.getMinecraftId(), null));
        } catch (Exception e) {
            System.out.println("Could not announce the removed link of " + link.getMinecraftName()
                    + ": " + e.getMessage());
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("unlink")) {
            unlink(event);
            return;
        }
        if (!event.getName().equals("verify")) return;
        OptionMapping option = event.getOption("minecraftname");
        if (option == null) {
            event.reply("Sag dazu, wie du im Spiel heißt: /verify <minecraftname>").setEphemeral(true).queue();
            return;
        }
        String minecraftName = option.getAsString().trim();

        AccountLink existing = links.byDiscord(event.getUser().getId());
        if (existing != null) {
            event.reply("Dein Discord-Account ist schon mit **" + existing.getMinecraftName()
                            + "** verknüpft. Der Besitzer kann das mit /unlink " + existing.getMinecraftName()
                            + " lösen.")
                    .setEphemeral(true).queue();
            return;
        }
        // mojang is slow enough to run into discord's three second reply window
        event.deferReply(true).queue();
        Main.getInstance().async(() -> {
            UUID uuid = UUIDFetcher.findUUIDByName(minecraftName, true);
            if (uuid == null) {
                event.getHook().sendMessage("Den Spieler **" + minecraftName
                        + "** gibt es nicht - hast du dich vertippt?").queue();
                return;
            }
            AccountLink taken = links.get(uuid);
            if (taken != null) {
                event.getHook().sendMessage("**" + minecraftName + "** gehört schon zu "
                        + taken.describeDiscord() + ".").queue();
                return;
            }
            String code = links.startLink(event.getUser().getId(), event.getUser().getName(),
                    uuid, minecraftName);
            event.getHook().sendMessage("Dein Code ist **" + code + "**.\n"
                    + "Geh auf den Server und tippe dort `/verify " + code + "` ein.\n"
                    + "Der Code gilt " + (AccountLinkStore.CODE_VALID_MS / 60000L) + " Minuten.").queue();
        });
    }
}

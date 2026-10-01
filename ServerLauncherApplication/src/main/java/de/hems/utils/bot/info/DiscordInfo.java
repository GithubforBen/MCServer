package de.hems.utils.bot.info;

import de.hems.Main;
import de.hems.utils.Configuration;
import de.hems.utils.whitelist.WhitelistSync;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The explanation of the network in a discord channel - one for the players, one for the admins.
 * <p>
 * Every section of the text is a message of its own, so a link can point at "Lotto" instead of at a wall.
 * The bot remembers its messages and brings them up to date at every start: a changed section is edited in
 * place, and only when the number of sections changed is everything posted anew. It only ever deletes
 * messages it posted itself for this page.
 */
public class DiscordInfo extends ListenerAdapter {

    /** The two texts, with the command that sets their channel and where that is kept. */
    public enum Page {
        PLAYERS("setinfochannel", "info-channel", "spieler.md"),
        ADMINS("setadmininfochannel", "admin-info-channel", "admins.md");

        private final String command;
        private final String key;
        private final String file;

        Page(String command, String key, String file) {
            this.command = command;
            this.key = key;
            this.file = file;
        }

        public String getCommand() {
            return command;
        }

        private String messagesKey() {
            return key + "-messages";
        }
    }

    private final Configuration configuration;

    public DiscordInfo(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Brings both pages up to date, in the background. Called once the bot is ready.
     */
    public void refreshAll() {
        Main.getInstance().async(() -> {
            for (Page page : Page.values()) publish(page);
        });
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        Page page = null;
        for (Page candidate : Page.values()) {
            if (candidate.command.equals(event.getName())) page = candidate;
        }
        if (page == null) return;
        if (event.getMember() == null || !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("Dafür brauchst du Administrator-Rechte.").setEphemeral(true).queue();
            return;
        }
        if (!(event.getChannel() instanceof TextChannel channel)) {
            event.reply("Das geht nur in einem normalen Textkanal.").setEphemeral(true).queue();
            return;
        }
        if (!channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_SEND,
                Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_HISTORY)) {
            // otherwise the answer says it is coming, and then nothing comes
            event.reply("Ich darf hier nicht schreiben. Ich brauche „Nachrichten senden“, „Links einbetten“ "
                    + "und „Nachrichtenverlauf lesen“.").setEphemeral(true).queue();
            return;
        }
        Page chosen = page;
        String previous = configuration.getConfig().getString(page.key);
        String now = event.getChannel().getId();
        event.reply("Die Erklärung für " + (page == Page.PLAYERS ? "Spieler" : "Admins") + " kommt jetzt nach "
                + event.getChannel().getAsMention() + ". Sie wird bei jedem Start des Launchers aktualisiert.")
                .setEphemeral(true).queue();
        Main.getInstance().async(() -> {
            synchronized (this) {
                if (previous != null && !previous.equals(now)) {
                    // the old copy would go stale the moment the text changes, so it goes with the move
                    delete(text(previous), configuration.getConfig().getStringList(chosen.messagesKey()));
                    configuration.getConfig().set(chosen.messagesKey(), List.of());
                }
                configuration.getConfig().set(chosen.key, now);
                configuration.save();
            }
            publish(chosen);
        });
    }

    /**
     * Posts a page into its channel or brings the posted one up to date. Runs off the JDA threads, it waits
     * for every message so they stay in order.
     */
    private synchronized void publish(Page page) {
        TextChannel channel = text(configuration.getConfig().getString(page.key));
        if (channel == null) return;
        List<MessageEmbed> embeds;
        try {
            embeds = embeds(InfoText.parse(InfoText.load(page.file), values()));
        } catch (Exception e) {
            System.out.println("Could not read the info text " + page.file + ": " + e.getMessage());
            return;
        }
        List<String> ids = configuration.getConfig().getStringList(page.messagesKey());
        try {
            if (ids.size() == embeds.size() && update(channel, ids, embeds)) return;
            delete(channel, ids);
            List<String> posted = new ArrayList<>();
            for (MessageEmbed embed : embeds) {
                posted.add(channel.sendMessageEmbeds(embed).complete().getId());
            }
            configuration.getConfig().set(page.messagesKey(), posted);
            configuration.save();
        } catch (RuntimeException e) {
            System.out.println("Could not post the info text into #" + channel.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Edits the posted messages where the text changed.
     *
     * @return false when one of them is gone, so the page has to be posted again
     */
    private boolean update(TextChannel channel, List<String> ids, List<MessageEmbed> embeds) {
        List<Message> messages = new ArrayList<>();
        for (String id : ids) {
            try {
                messages.add(channel.retrieveMessageById(id).complete());
            } catch (RuntimeException e) {
                return false;
            }
        }
        for (int i = 0; i < messages.size(); i++) {
            MessageEmbed want = embeds.get(i);
            List<MessageEmbed> have = messages.get(i).getEmbeds();
            boolean same = have.size() == 1
                    && Objects.equals(have.get(0).getTitle(), want.getTitle())
                    && Objects.equals(have.get(0).getDescription(), want.getDescription());
            if (!same) messages.get(i).editMessageEmbeds(want).complete();
        }
        return true;
    }

    private static void delete(TextChannel channel, List<String> ids) {
        if (channel == null) return;
        for (String id : ids) {
            try {
                channel.deleteMessageById(id).complete();
            } catch (RuntimeException ignored) {
                // already gone - which is what deleting wanted
            }
        }
    }

    private static List<MessageEmbed> embeds(List<InfoText.Section> sections) {
        List<MessageEmbed> embeds = new ArrayList<>();
        for (InfoText.Section section : sections) {
            embeds.add(Main.getEmbedBuilder()
                    .setTimestamp(null)
                    .setTitle(section.title().isBlank() ? null : section.title())
                    .setDescription(section.body())
                    .build());
        }
        return embeds;
    }

    /** What the placeholders in the texts stand for. A local address is nothing a player can join. */
    private static Map<String, String> values() {
        Map<String, String> values = new HashMap<>();
        try {
            String rules = WhitelistSync.rulesUrl();
            if (!local(rules)) values.put("regeln", rules);
            String ip = Main.getInstance().getPublicAddress();
            if (ip != null && !local(ip)) values.put("adresse", ip);
        } catch (Exception ignored) {
            // without an address the line is left out
        }
        return values;
    }

    private static boolean local(String address) {
        return address.contains("localhost") || address.contains("127.0.0.1");
    }

    private static TextChannel text(String id) {
        JDA jda = Main.getInstance() == null ? null : Main.getInstance().getJda();
        return jda == null || id == null ? null : jda.getTextChannelById(id);
    }
}

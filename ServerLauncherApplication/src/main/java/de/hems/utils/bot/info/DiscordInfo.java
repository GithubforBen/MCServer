package de.hems.utils.bot.info;

import de.hems.Main;
import de.hems.utils.Configuration;
import de.hems.utils.whitelist.WhitelistStore;
import de.hems.utils.whitelist.WhitelistSync;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.order.ChannelOrderAction;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The explanations of the network on discord: what there is for the players, the handbook for the admins,
 * and the rules.
 * <p>
 * Each is one message with buttons rather than a wall of them. The message shows the first part; a click on
 * "Weiter" or a part from the menu opens the text for whoever clicked, only for them, and they page through
 * it there - a shared message that everybody turns the pages of would turn under somebody else's feet.
 * <p>
 * The bot remembers the message and brings it up to date at every start, editing it in place.
 * <p>
 * Setting the channel of a page in a channel that already has messages in it does not post into the middle
 * of them: that channel is renamed to "…-archiv", hidden and moved to the bottom, and a fresh copy of it -
 * same name, category, place and permissions - takes its place and gets the page.
 */
public class DiscordInfo extends ListenerAdapter {

    /** Prefix of every button and menu of the pages. */
    private static final String COMPONENT = "info:";
    /** What discord allows in one select menu. */
    private static final int MAX_OPTIONS = 25;
    /** What discord allows as the label of one option. */
    private static final int MAX_LABEL = 100;

    /** The texts, with the command that sets their channel and where that is kept. */
    public enum Page {
        PLAYERS("setinfochannel", "info-channel", "Infos"),
        ADMINS("setadmininfochannel", "admin-info-channel", "Admin-Handbuch"),
        RULES("setruleschannel", "rules-channel", "Regeln");

        private final String command;
        private final String key;
        private final String title;

        Page(String command, String key, String title) {
            this.command = command;
            this.key = key;
            this.title = title;
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
     * Brings every page up to date, in the background. Called once the bot is ready.
     */
    public void refreshAll() {
        Main.getInstance().async(() -> {
            for (Page page : Page.values()) publish(page);
        });
    }

    // ------------------------------------------------------------------ commands

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("regeln")) {
            View view = view(Page.RULES, 0);
            if (view == null) {
                event.reply("Es sind noch keine Regeln eingetragen.").setEphemeral(true).queue();
                return;
            }
            event.replyEmbeds(view.embed()).setComponents(view.rows()).setEphemeral(true).queue();
            return;
        }
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
        event.deferReply(true).queue();
        Main.getInstance().async(() -> {
            String answer;
            try {
                answer = place(chosen, channel);
            } catch (RuntimeException e) {
                answer = "Das hat nicht geklappt: " + e.getMessage();
            }
            event.getHook().sendMessage(answer).queue();
        });
    }

    /**
     * Makes a channel the home of a page: the channel itself when nothing else is in it, a fresh copy of it
     * when there is, with the old one archived.
     *
     * @return what to tell the admin
     */
    private synchronized String place(Page page, TextChannel channel) {
        List<String> ours = configuration.getConfig().getStringList(page.messagesKey());
        TextChannel target = channel;
        String archived = null;
        if (hasOtherMessages(channel, ours)) {
            Guild guild = channel.getGuild();
            if (!guild.getSelfMember().hasPermission(channel, Permission.MANAGE_CHANNEL, Permission.MANAGE_PERMISSIONS)) {
                return "In " + channel.getAsMention() + " stehen schon Nachrichten. Damit ich ihn archivieren und "
                        + "einen frischen Kanal anlegen kann, brauche ich „Kanäle verwalten“ und „Berechtigungen "
                        + "verwalten“ - oder nimm einen leeren Kanal.";
            }
            target = replace(channel);
            archived = channel.getName();
        }
        String previous = configuration.getConfig().getString(page.key);
        if (previous != null && !previous.equals(target.getId())) {
            // the old copy would go stale the moment the text changes, so it goes with the move
            delete(text(previous), ours);
            configuration.getConfig().set(page.messagesKey(), List.of());
        }
        configuration.getConfig().set(page.key, target.getId());
        configuration.save();
        publish(page);
        return "„" + page.title + "“ steht jetzt in " + target.getAsMention()
                + (archived == null ? "" : " - der alte Kanal mit den Nachrichten darin heißt jetzt „"
                + archived + "-archiv“ und ist versteckt")
                + ". Bei jedem Start des Launchers wird die Nachricht aktualisiert.";
    }

    /**
     * @return whether somebody other than this page has written in the channel
     */
    private static boolean hasOtherMessages(TextChannel channel, List<String> ours) {
        for (Message message : channel.getHistory().retrievePast(20).complete()) {
            if (!ours.contains(message.getId())) return true;
        }
        return false;
    }

    /**
     * Archives a channel and puts a fresh copy of it in its place.
     *
     * @return the copy
     */
    private static TextChannel replace(TextChannel old) {
        Guild guild = old.getGuild();
        String name = old.getName();
        // the copy is made first, so it takes over the permissions the channel had before it was hidden
        TextChannel copy = old.createCopy().complete();
        try {
            Category parent = old.getParentCategory();
            ChannelOrderAction order = parent != null
                    ? parent.modifyTextChannelPositions() : guild.modifyTextChannelPositions();
            int at = order.getCurrentOrder().indexOf(old);
            if (at >= 0) {
                order.selectPosition(copy).moveTo(at);
                order.selectPosition(old).moveTo(order.getCurrentOrder().size() - 1);
                order.complete();
            }
        } catch (RuntimeException e) {
            // the place is cosmetic; the copy works wherever it ended up
            System.out.println("Could not move the info channel into place: " + e.getMessage());
        }
        old.getManager().setName(clip(name + "-archiv", 100)).complete();
        old.upsertPermissionOverride(guild.getPublicRole()).deny(Permission.VIEW_CHANNEL).complete();
        return copy;
    }

    // ------------------------------------------------------------------ the posted message

    /**
     * Posts a page into its channel or brings the posted one up to date. Runs off the JDA threads.
     */
    private synchronized void publish(Page page) {
        TextChannel channel = text(configuration.getConfig().getString(page.key));
        if (channel == null) return;
        View view = view(page, 0);
        List<String> ids = configuration.getConfig().getStringList(page.messagesKey());
        try {
            if (view == null) {
                delete(channel, ids);
                configuration.getConfig().set(page.messagesKey(), List.of());
                configuration.save();
                return;
            }
            if (ids.size() == 1) {
                Message message = retrieve(channel, ids.getFirst());
                if (message != null) {
                    message.editMessageEmbeds(view.embed()).setComponents(view.rows()).complete();
                    return;
                }
            }
            // none yet, or the one message per section of before: posted anew as one
            delete(channel, ids);
            String id = channel.sendMessageEmbeds(view.embed()).setComponents(view.rows()).complete().getId();
            configuration.getConfig().set(page.messagesKey(), List.of(id));
            configuration.save();
        } catch (RuntimeException e) {
            System.out.println("Could not post " + page.title + " into #" + channel.getName() + ": " + e.getMessage());
        }
    }

    private static Message retrieve(TextChannel channel, String id) {
        try {
            return channel.retrieveMessageById(id).complete();
        } catch (RuntimeException e) {
            return null;
        }
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

    // ------------------------------------------------------------------ paging

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (!id.startsWith(COMPONENT)) return;
        // info:<page>:<part>
        String[] parts = id.split(":");
        if (parts.length != 3) return;
        show(event.getMessage().isEphemeral(), parts[1], parts[2], event);
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        String id = event.getComponentId();
        if (!id.startsWith(COMPONENT) || event.getValues().isEmpty()) return;
        // info:<page>
        show(event.getMessage().isEphemeral(), id.substring(COMPONENT.length()), event.getValues().getFirst(), event);
    }

    /**
     * Shows a part of a page: in place when the click came from somebody's own copy, as their own copy when
     * it came from the message in the channel.
     */
    private void show(boolean own, String pageName, String partText,
                      net.dv8tion.jda.api.interactions.components.ComponentInteraction event) {
        Page page;
        int part;
        try {
            page = Page.valueOf(pageName);
            part = Integer.parseInt(partText);
        } catch (IllegalArgumentException e) {
            return;
        }
        View view = view(page, part);
        if (view == null) {
            event.reply("Dieser Text ist gerade leer.").setEphemeral(true).queue();
            return;
        }
        if (own) {
            event.editMessageEmbeds(view.embed()).setComponents(view.rows()).queue();
        } else {
            event.replyEmbeds(view.embed()).setComponents(view.rows()).setEphemeral(true).queue();
        }
    }

    /**
     * One part of a page as it is shown: the text, which part of how many, and the way to the others.
     *
     * @return {@code null} when the page has no text
     */
    static View view(Page page, int part) {
        List<InfoText.Section> sections = sections(page);
        if (sections.isEmpty()) return null;
        int at = Math.max(0, Math.min(part, sections.size() - 1));
        InfoText.Section section = sections.get(at);
        String heading = section.title().isBlank() ? page.title : section.title();
        MessageEmbed embed = Main.getEmbedBuilder()
                .setTimestamp(null)
                .setTitle(heading)
                .setDescription(section.body())
                .setFooter(page.title + " · Teil " + (at + 1) + " von " + sections.size()
                        + (sections.size() > 1 ? " · mit den Knöpfen weiterblättern" : ""))
                .build();

        List<ActionRow> rows = new ArrayList<>();
        List<Button> buttons = new ArrayList<>();
        if (sections.size() > 1) {
            buttons.add(Button.secondary(COMPONENT + page.name() + ":" + (at - 1), "◀ Zurück").withDisabled(at == 0));
            buttons.add(Button.primary(COMPONENT + page.name() + ":" + (at + 1), "Weiter ▶")
                    .withDisabled(at == sections.size() - 1));
        }
        if (page == Page.RULES) {
            String url = WhitelistSync.rulesUrl();
            if (!local(url)) buttons.add(Button.link(url, "Regeln akzeptieren und auf die Whitelist"));
        }
        if (!buttons.isEmpty()) rows.add(ActionRow.of(buttons));
        if (sections.size() > 1) {
            List<SelectOption> options = new ArrayList<>();
            for (int i = 0; i < sections.size() && i < MAX_OPTIONS; i++) {
                String title = sections.get(i).title().isBlank() ? page.title : sections.get(i).title();
                options.add(SelectOption.of(clip((i + 1) + ". " + title, MAX_LABEL), String.valueOf(i))
                        .withDefault(i == at));
            }
            rows.add(ActionRow.of(StringSelectMenu.create(COMPONENT + page.name())
                    .setPlaceholder("Springe zu …")
                    .addOptions(options)
                    .build()));
        }
        return new View(embed, rows);
    }

    /** What a message of a page consists of. */
    record View(MessageEmbed embed, List<ActionRow> rows) {
    }

    /**
     * @return the parts of a page, read fresh, so a changed text or changed rules show at the next click
     */
    private static List<InfoText.Section> sections(Page page) {
        try {
            return switch (page) {
                case PLAYERS -> InfoText.parse(InfoText.load("spieler.md"), values());
                case ADMINS -> InfoText.parse(InfoText.load("admins.md"), values());
                case RULES -> InfoText.parse(rulesMarkdown(), Map.of());
            };
        } catch (Exception e) {
            System.out.println("Could not read " + page.title + ": " + e.getMessage());
            return List.of();
        }
    }

    /**
     * The rules from the whitelist, which are plain text written for the website, made into parts: every
     * top-level paragraph ({@code §1 - Griefing}) is a part of its own, the ones under it
     * ({@code §2.1 - Hacken}) are bold lines in it. Markdown characters are escaped - the asterisks behind
     * "Spawncamping*" would otherwise turn half a paragraph italic.
     */
    private static String rulesMarkdown() {
        Main main = Main.getInstance();
        WhitelistStore store = main == null ? null : main.getWhitelistStore();
        String rules = store == null ? null : store.getRules();
        return rules == null || rules.isBlank() ? "" : rulesMarkdown(rules);
    }

    static String rulesMarkdown(String rules) {
        StringBuilder out = new StringBuilder();
        StringBuilder intro = new StringBuilder();
        boolean started = false;
        for (String line : rules.replace("\r", "").split("\n", -1)) {
            String trimmed = line.strip();
            if (trimmed.matches("§\\d+(\\s*[-–:].*)?")) {
                // a single line before the first paragraph is the title of the whole thing ("MC-Server
                // Regelwerk"), which the page already has - as a part of its own it would be nothing but that
                if (!started && intro.toString().strip().contains("\n")) out.append(intro);
                started = true;
                out.append("## ").append(trimmed).append('\n');
                continue;
            }
            String converted = trimmed.startsWith("§")
                    ? "**" + MarkdownSanitizer.escape(trimmed) + "**\n"
                    : MarkdownSanitizer.escape(line) + "\n";
            (started ? out : intro).append(converted);
        }
        if (!started) out.append(intro);
        return out.toString();
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

    private static String clip(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    private static TextChannel text(String id) {
        JDA jda = Main.getInstance() == null ? null : Main.getInstance().getJda();
        return jda == null || id == null ? null : jda.getTextChannelById(id);
    }
}

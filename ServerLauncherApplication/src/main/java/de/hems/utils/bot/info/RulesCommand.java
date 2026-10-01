package de.hems.utils.bot.info;

import de.hems.Main;
import de.hems.utils.whitelist.WhitelistStore;
import de.hems.utils.whitelist.WhitelistSync;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /regeln} on discord: the rules of the server, the same text the website shows, with a button to the
 * page where they are accepted - which is what puts a player on the whitelist.
 * <p>
 * Only the one who asked sees the answer. The rules are long, and a channel that fills up with them every
 * time somebody asks is a channel nobody reads any more.
 */
public class RulesCommand extends ListenerAdapter {

    /** What fits into the embeds of one message, all of them together. */
    private static final int MAX_MESSAGE = 6000;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("regeln")) return;
        WhitelistStore store = Main.getInstance().getWhitelistStore();
        String rules = store == null ? "" : store.getRules();
        if (rules == null || rules.isBlank()) {
            event.reply("Es sind noch keine Regeln eingetragen.").setEphemeral(true).queue();
            return;
        }
        List<List<MessageEmbed>> messages = messages(InfoText.parse(format(rules), java.util.Map.of()));

        String url = WhitelistSync.rulesUrl();
        boolean reachable = !url.contains("localhost") && !url.contains("127.0.0.1");
        var reply = event.replyEmbeds(messages.get(0)).setEphemeral(true);
        if (reachable && messages.size() == 1) reply = reply.setComponents(ActionRow.of(button(url)));
        reply.queue(hook -> {
            for (int i = 1; i < messages.size(); i++) {
                var next = hook.sendMessageEmbeds(messages.get(i)).setEphemeral(true);
                // the button goes under the last part, where somebody who has read them all ends up
                if (reachable && i == messages.size() - 1) next = next.setComponents(ActionRow.of(button(url)));
                next.queue();
            }
        });
    }

    private static Button button(String url) {
        return Button.link(url, "Regeln akzeptieren und auf die Whitelist");
    }

    /**
     * The rules are plain text written for the website. Markdown characters in them are escaped - the
     * asterisks behind "Spawncamping*" would otherwise turn half a paragraph italic - and the lines that start
     * a paragraph ({@code §1 - Griefing}) are made bold.
     */
    static String format(String rules) {
        StringBuilder out = new StringBuilder();
        for (String line : rules.replace("\r", "").split("\n", -1)) {
            String escaped = MarkdownSanitizer.escape(line);
            out.append(line.strip().startsWith("§") ? "**" + escaped.strip() + "**" : escaped).append('\n');
        }
        return out.toString();
    }

    /** The sections as embeds, put together into messages that each stay under what discord takes. */
    private static List<List<MessageEmbed>> messages(List<InfoText.Section> sections) {
        List<List<MessageEmbed>> messages = new ArrayList<>();
        List<MessageEmbed> current = new ArrayList<>();
        int size = 0;
        boolean first = true;
        for (InfoText.Section section : sections) {
            MessageEmbed embed = Main.getEmbedBuilder()
                    .setTimestamp(null)
                    .setTitle(first ? "Regeln" : null)
                    .setDescription(section.body())
                    .build();
            first = false;
            int length = embed.getLength();
            if (!current.isEmpty() && (size + length > MAX_MESSAGE || current.size() == 10)) {
                messages.add(current);
                current = new ArrayList<>();
                size = 0;
            }
            current.add(embed);
            size += length;
        }
        if (!current.isEmpty()) messages.add(current);
        return messages;
    }
}

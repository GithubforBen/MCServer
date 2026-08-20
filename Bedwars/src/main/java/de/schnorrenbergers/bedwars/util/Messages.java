package de.schnorrenbergers.bedwars.util;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every line the plugin says, out of {@code messages.yml}.
 * <p>
 * Nothing in the plugin writes a sentence itself. That keeps the wording in one file that can be
 * translated or reworded without a build, and it is the reason the texts are english while this plan and
 * the comments are not - the file decides, not the code.
 * <p>
 * The texts are <a href="https://docs.advntr.dev/minimessage/format.html">MiniMessage</a>, so colours and
 * hovers live in the file as well. Placeholders are passed as name/value pairs and are inserted as plain
 * text, never parsed - a player called {@code <red>} cannot recolour a message.
 */
public final class Messages {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final Map<String, String> DEFAULTS = defaults();

    private static ConfigFile file;
    private static String prefix = "<gray>[<red>Bed<white>wars<gray>] <reset>";

    private Messages() {
    }

    /**
     * Reads the file and fills in every text that is missing.
     */
    public static void load() {
        file = new ConfigFile("messages.yml");
        for (Map.Entry<String, String> entry : DEFAULTS.entrySet()) {
            file.get(entry.getKey(), entry.getValue());
        }
        file.save();
        prefix = file.get("prefix", DEFAULTS.get("prefix"));
    }

    public static void reload() {
        load();
    }

    /**
     * @param key          which text
     * @param placeholders name/value pairs, e.g. {@code "team", "RED"}
     * @return the text, ready to send
     */
    public static Component get(String key, String... placeholders) {
        return MINI.deserialize(raw(key), resolver(placeholders));
    }

    /**
     * @param to           who to tell
     * @param key          which text
     * @param placeholders name/value pairs
     */
    public static void send(Audience to, String key, String... placeholders) {
        to.sendMessage(get(key, placeholders));
    }

    /**
     * Says something to everybody on this server.
     *
     * @param key          which text
     * @param placeholders name/value pairs
     */
    public static void broadcast(String key, String... placeholders) {
        Bukkit.getServer().sendMessage(get(key, placeholders));
    }

    /**
     * @param key which text
     * @return the raw MiniMessage line, or the key itself when nothing is configured under it
     */
    public static String raw(String key) {
        if (file == null) return DEFAULTS.getOrDefault(key, key);
        return file.get(key, DEFAULTS.getOrDefault(key, key));
    }

    /**
     * @param placeholders name/value pairs, an odd trailing name is ignored
     * @return the resolver that inserts them, plus the shared prefix
     */
    private static TagResolver resolver(String... placeholders) {
        TagResolver.Builder builder = TagResolver.builder();
        builder.resolver(Placeholder.parsed("prefix", prefix));
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            builder.resolver(Placeholder.unparsed(placeholders[i], placeholders[i + 1]));
        }
        return builder.build();
    }

    /**
     * @return the texts as they are shipped, in the order they are written to the file
     */
    private static Map<String, String> defaults() {
        Map<String, String> texts = new LinkedHashMap<>();
        texts.put("prefix", "<gray>[<red>Bed<white>wars<gray>] <reset>");

        texts.put("command.no-permission", "<prefix><red>You are not allowed to do that.");
        texts.put("command.unknown", "<prefix><red>Unknown subcommand <white><input></white>.");
        texts.put("command.usage", "<prefix><gray>Usage: <white>/bw <usage>");
        texts.put("command.players-only", "<prefix><red>Only a player can do that.");

        texts.put("reload.done", "<prefix><green>Reloaded every config in <white><millis>ms</white>.");
        texts.put("reload.failed", "<prefix><red>Reload failed: <white><error>");

        texts.put("status.header", "<prefix><gray>This server hosts:");
        texts.put("status.mode", "<gray>- Mode: <white><mode></white> <gray>(<teams> teams of <size>)");
        texts.put("status.phase", "<gray>- Phase: <white><phase>");
        texts.put("status.players", "<gray>- Players: <white><online></white><gray>/<white><maximum>");
        texts.put("status.map", "<gray>- Map: <white><map>");
        texts.put("status.map-missing", "<gray>- Map: <red>none set up yet");

        texts.put("addon.header", "<prefix><gray>Addons on this server:");
        texts.put("addon.entry.on", "<green>✔ <white><addon></white> <dark_gray>- <gray><description>");
        texts.put("addon.entry.off", "<red>✖ <gray><addon> <dark_gray>- <gray><description>");
        texts.put("addon.entry.source", "<dark_gray>   set by <source>");
        texts.put("addon.unknown", "<prefix><red>There is no addon called <white><addon></white>.");
        texts.put("addon.switched", "<prefix><gray>Addon <white><addon></white> is now <state><gray>.");
        texts.put("addon.locked", "<prefix><red>The round has already started, <white><addon></white> stays as it is.");
        texts.put("addon.state.on", "<green>on");
        texts.put("addon.state.off", "<red>off");

        texts.put("lobby.counting", "<prefix><gray>Enough players - starting in <white><seconds></white> seconds.");
        texts.put("lobby.countdown", "<prefix><yellow><seconds></yellow> <gray>...");
        texts.put("lobby.full", "<prefix><gray>The lobby is full - starting in <white><seconds></white> seconds.");
        texts.put("lobby.cancelled", "<prefix><red>Not enough players any more. <gray>Waiting for <white><needed></white>.");

        texts.put("game.started", "<prefix><green>The round begins! <gray>Mode: <white><mode>");
        texts.put("game.ended.winner", "<prefix><gold><team></gold> <yellow>wins the round!");
        texts.put("game.ended.nobody", "<prefix><gray>The round is over. Nobody won.");
        texts.put("team.eliminated", "<prefix><red><team></red> <gray>has been eliminated.");

        texts.put("phase.lobby", "Waiting");
        texts.put("phase.running", "Running");
        texts.put("phase.ending", "Over");
        return texts;
    }
}

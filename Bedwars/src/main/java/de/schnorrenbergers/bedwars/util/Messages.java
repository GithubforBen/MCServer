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
        repair();
        for (Map.Entry<String, String> entry : DEFAULTS.entrySet()) {
            file.get(entry.getKey(), entry.getValue());
        }
        file.save();
        prefix = file.get("prefix", DEFAULTS.get("prefix"));
    }

    /**
     * Throws away keys that are a block rather than a sentence.
     * <p>
     * An older version of this file wrote {@code death.killed} and {@code death.killed.final}, and yaml
     * cannot hold both: the second turns the first into a block, and the line players were shown was
     * {@code MemorySection[path='death.killed']}. The keys were renamed, but a file written by that
     * version is still lying on every server that ever ran one - so the block is dropped here and the
     * sentence is written back underneath it.
     */
    private static void repair() {
        for (String key : DEFAULTS.keySet()) {
            if (file.contains(key) && !file.raw().isString(key)) file.set(key, null);
        }
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

        texts.put("command.start.done", "<prefix><green>The round starts now.");
        texts.put("command.start.running", "<prefix><red>The round is already under way.");
        texts.put("command.start.impossible", "<prefix><red>No map is ready, or one is being set up.");
        texts.put("command.stop.done", "<prefix><gray>The round was ended.");
        texts.put("command.stop.not-running", "<prefix><red>No round is running.");

        texts.put("reload.done", "<prefix><green>Reloaded every config in <white><millis>ms</white>.");
        texts.put("reload.failed", "<prefix><red>Reload failed: <white><error>");

        texts.put("status.header", "<prefix><gray>This server hosts:");
        texts.put("status.mode", "<gray>- Mode: <white><mode></white> <gray>(<teams> teams of <size>)");
        texts.put("status.phase", "<gray>- Phase: <white><phase>");
        texts.put("status.players", "<gray>- Players: <white><online></white><gray>/<white><maximum>");
        texts.put("status.map", "<gray>- Map: <white><map>");
        texts.put("status.map-missing", "<gray>- Map: <red>none set up yet");

        texts.put("status.setup", "<gray>- <yellow>A map is being set up, the round is on hold.");

        texts.put("setup.usage", "<prefix><gray>Usage: <white>/bw <usage>");
        texts.put("setup.status", "<prefix><gray>Setting up <white><map></white> <gray>(<state><gray>)");
        texts.put("setup.state.saved", "<green>saved");
        texts.put("setup.state.unsaved", "<yellow>unsaved changes");
        texts.put("setup.no-maps", "<prefix><red>No maps yet. <gray>Put a world folder into <white><folder></white>.");
        texts.put("setup.list.header", "<prefix><gray><count> map(s) on this server:");
        texts.put("setup.list.entry", "<gray>- <white><map>");
        texts.put("setup.unknown-map", "<prefix><red>There is no map called <white><map></white> in <white><folder></white>.");
        texts.put("setup.already", "<prefix><red>You are already setting up <white><map></white>. <gray>Leave it with /bw setup exit.");
        texts.put("setup.not-active", "<prefix><red>No map is open. <gray>Open one with /bw setup <map>.");
        texts.put("setup.started", "<prefix><green>Setting up <white><map></white>. <gray>The round is on hold.");
        texts.put("setup.world-failed", "<prefix><red>The world of <white><map></white> could not be loaded.");
        texts.put("setup.set", "<prefix><green><what></green> <gray>set to <white><where></white>.");
        texts.put("setup.removed", "<prefix><yellow><what></yellow> <gray>removed.");
        texts.put("setup.look-at-bed", "<prefix><red>Look at the bed you mean.");
        texts.put("setup.not-a-number", "<prefix><red><input></red> <gray>is not a number.");
        texts.put("setup.unknown-team", "<prefix><red>There is no team colour <white><input></white>.");
        texts.put("setup.unknown-mode", "<prefix><red>There is no mode <white><input></white> in modes.yml.");
        texts.put("setup.gen-none", "<prefix><red>No generator within <white><range></white> blocks.");
        texts.put("setup.build-set", "<prefix><green>Build limit</green> <gray>set: up to <white><max></white>, void below <white><void></white>.");
        texts.put("setup.mode-set", "<prefix><green><mode></green> <gray>plays with <white><teams></white>.");
        texts.put("setup.mode-auto", "<prefix><gray><mode> uses the map's own teams again.");
        texts.put("setup.check.ok", "<prefix><green>The map is ready for <white><mode></white>.");
        texts.put("setup.check.header", "<prefix><yellow><count></yellow> <gray>thing(s) missing for <white><mode></white>:");
        texts.put("setup.check.entry", "<gray>- <white><problem>");
        texts.put("setup.saved", "<prefix><green>Saved <white><map></white>, world and all.");
        texts.put("setup.save-failed", "<prefix><red>Could not save <white><map></white>. <gray>See the console.");
        texts.put("setup.unsaved", "<prefix><yellow>You have unsaved changes. <gray>Type it again to leave anyway.");
        texts.put("setup.exited", "<prefix><gray>Setup closed.");

        texts.put("addon.header", "<prefix><gray>Addons on this server:");
        texts.put("addon.entry.on", "<green>✔ <white><addon></white> <dark_gray>- <gray><description>");
        texts.put("addon.entry.off", "<red>✖ <gray><addon> <dark_gray>- <gray><description>");
        texts.put("addon.entry.source", "<dark_gray>   set by <source>");
        texts.put("addon.unknown", "<prefix><red>There is no addon called <white><addon></white>.");
        texts.put("addon.switched", "<prefix><gray>Addon <white><addon></white> is now <state><gray>.");
        texts.put("addon.locked", "<prefix><red>The round has already started, <white><addon></white> stays as it is.");
        texts.put("addon.state.on", "<green>on");
        texts.put("addon.state.off", "<red>off");

        texts.put("lobby.settings-item", "<yellow>Settings <gray>(right click)");
        texts.put("lobby.settings-lore", "<gray>How this round is played: 1.8 combat, the|<gray>locator bar, the shop, whether the round|<gray>starts by itself.|<yellow>Only you can see this item.");
        texts.put("lobby.select.lore", "<gray>Pick the team you want to play in.|<gray>Whoever has not picked when the round|<gray>starts is put into the emptiest one.");
        texts.put("lobby.auto-start-off", "<prefix><yellow>The automatic start is off. <gray>An admin starts the round.");
        texts.put("lobby.waiting-for-event", "<prefix><gray>Waiting for <white><event></white><gray>, which begins in <white><time></white>.");
        texts.put("lobby.event-countdown", "<prefix><yellow><seconds></yellow> <gray>until the event begins.");

        texts.put("admin.title", "<dark_gray>Server Settings");
        texts.put("admin.on", "<gray>Now: <green>On");
        texts.put("admin.off", "<gray>Now: <red>Off");
        texts.put("admin.click-on", "<yellow>Click to switch it on");
        texts.put("admin.click-off", "<yellow>Click to switch it off");
        texts.put("admin.switched", "<prefix><gray><feature> is now <state><gray>.");
        texts.put("admin.state.on", "<green>on");
        texts.put("admin.state.off", "<red>off");
        texts.put("admin.addons", "<white>Addons");
        texts.put("admin.addons-lore", "<gray>The addons of this round");

        texts.put("addon.title", "<dark_gray>Addons");
        texts.put("addon.none", "<prefix><gray>This server has no addons.");
        texts.put("addon.entry.state.on", "<green>On");
        texts.put("addon.entry.state.off", "<red>Off");
        texts.put("addon.entry.click", "<gray>Click to switch it for this round");

        texts.put("bed-token.name", "<gold>Bed Token");
        texts.put("bed-token.lore", "<gray>Bring it home and use it where your bed stood.");
        texts.put("bed-token.bought", "<prefix><gold><team></gold> <yellow>bought a Bed Token! <gray>(<player>)");
        texts.put("bed-token.carrying", "<prefix><gray>Take it to your own bed spot and right click.");
        texts.put("bed-token.picked-up", "<prefix><gray><player> <gray>picked the Bed Token up.");
        texts.put("bed-token.dropped", "<prefix><yellow><player></yellow> <gray>dropped the Bed Token!");
        texts.put("bed-token.restored", "<prefix><gold><team></gold> <yellow>have their bed back! <gray>(<player>)");
        texts.put("bed-token.no-need", "<prefix><gray>Your bed is still standing.");
        texts.put("bed-token.too-late", "<prefix><red>Your team is already out.");
        texts.put("bed-token.used-up", "<prefix><red>Your team has already used its <maximum> token(s).");
        texts.put("bed-token.already-out", "<prefix><red><player></red> <gray>is already carrying your team's token.");
        texts.put("bed-token.wrong-place", "<prefix><red>Only where your own bed stood.");
        texts.put("bed-token.no-room", "<prefix><red>There is something in the way of the bed.");

        texts.put("kit.item", "<yellow>Pick a kit");
        texts.put("kit.title", "<dark_gray>Kits");
        texts.put("kit.perk", "<gray>Perk: <white><perk> <level>");
        texts.put("kit.click", "<green>Click to pick this");
        texts.put("kit.picked", "<green>You are playing this");
        texts.put("kit.chosen", "<prefix><gray>You will play <white><kit></white>.");

        texts.put("custom-item.category", "<light_purple>Specials");
        texts.put("custom-item.grappling-hook.name", "<light_purple>Grappling Hook");
        texts.put("custom-item.grappling-hook.lore", "<gray>Pulls you to wherever the hook lands. One throw.");
        texts.put("custom-item.rescue-platform.name", "<light_purple>Rescue Platform");
        texts.put("custom-item.rescue-platform.lore", "<gray>Builds a platform under you. It does not last.");
        texts.put("custom-item.bridge-egg.name", "<light_purple>Bridge Egg");
        texts.put("custom-item.bridge-egg.lore", "<gray>Lays a bridge in your colour wherever it flies.");
        texts.put("custom-item.jump-pad.name", "<light_purple>Jump Pad");
        texts.put("custom-item.jump-pad.lore", "<gray>Place it and step on it.");

        texts.put("killstreak.reached", "<prefix><yellow><player></yellow> <gray>is on <white><streak></white> kills in a row.");
        texts.put("killstreak.title", "<gold><streak> in a row");
        texts.put("killstreak.subtitle", "<gray><effect> <level>");
        texts.put("killstreak.bounty.set", "<prefix><red>There is a bounty of <white><amount> <currency></white> <red>on <white><player></white>.");
        texts.put("killstreak.bounty.title", "<gold>+<amount> <currency>");
        texts.put("killstreak.bounty.subtitle", "<gray>Bounty on <white><victim>");
        texts.put("killstreak.bounty.claimed", "<prefix><white><player></white> <gray>collected <white><amount> <currency></white> <gray>for <white><victim></white>.");

        texts.put("random-event.title", "<gold>SOMETHING IS COMING");
        texts.put("random-event.subtitle", "<gray><event>");
        texts.put("random-event.warning", "<prefix><gold><event></gold> <gray>in <white><seconds></white>s - get to the middle.");
        texts.put("random-event.now", "<prefix><gold><event></gold> <gray>is happening now!");
        texts.put("random-event.resource-rain.name", "Resource Rain");
        texts.put("random-event.double-generators.name", "Faster Generators");
        texts.put("random-event.double-generators.over", "<prefix><gray>The middle generators are back to normal.");
        texts.put("random-event.loot-chest.name", "Loot Chest");

        texts.put("lobby.counting", "<prefix><gray>Enough players - starting in <white><seconds></white> seconds.");
        texts.put("lobby.countdown", "<prefix><yellow><seconds></yellow> <gray>...");
        texts.put("lobby.full", "<prefix><gray>The lobby is full - starting in <white><seconds></white> seconds.");
        texts.put("lobby.cancelled", "<prefix><red>Not enough players any more. <gray>Waiting for <white><needed></white>.");

        texts.put("lobby.joined", "<prefix><green><player></green> <gray>joined <dark_gray>(<white><online></white>/<white><maximum></white>)");
        texts.put("lobby.left", "<prefix><red><player></red> <gray>left <dark_gray>(<white><online></white>/<white><maximum></white>)");
        texts.put("lobby.no-map", "<prefix><red>This server has no map set up yet. <gray>An operator can fix that with /bw setup.");
        texts.put("lobby.select.item", "<green>Choose your team <gray>(right click)");
        texts.put("lobby.select.title", "<dark_gray>Choose your team");
        texts.put("lobby.team.count", "<gray><size>/<maximum> players");
        texts.put("lobby.team.member", "<dark_gray>- <white><player>");
        texts.put("lobby.team.empty", "<dark_gray>nobody yet");
        texts.put("lobby.team.chosen", "<prefix><gray>You are on <white><team></white> now.");
        texts.put("lobby.team.already", "<prefix><gray>You are already on <white><team></white>.");
        texts.put("lobby.team.full", "<prefix><red><team></red> <gray>is full.");

        texts.put("game.your-team", "<prefix><gray>You play for <white><team></white>.");
        texts.put("game.started", "<prefix><green>The round begins! <gray>Mode: <white><mode>");
        texts.put("game.ended.winner", "<prefix><gold><team></gold> <yellow>wins the round!");
        texts.put("game.ended.nobody", "<prefix><gray>The round is over. Nobody won.");
        texts.put("team.eliminated", "<prefix><red><team></red> <gray>has been eliminated.");

        texts.put("timeline.event", "<prefix><yellow><event></yellow><gray>.");
        texts.put("timeline.generator", "<prefix><yellow><event></yellow> <gray>- the middle generators got faster.");
        texts.put("timeline.generator-title", "<yellow><event>");
        texts.put("timeline.generator-subtitle", "<gray>The middle generators got faster");
        texts.put("timeline.bed-destruction", "<prefix><red><event></red> <gray>- every bed has fallen.");
        texts.put("timeline.bed-destruction-title", "<red>BED DESTRUCTION");
        texts.put("timeline.bed-destruction-subtitle", "<gray>Every bed is gone - one life left");
        texts.put("timeline.bed-destruction-yours", "<prefix><red>Your bed is gone. <gray>The next death is your last.");
        texts.put("timeline.sudden-death", "<prefix><dark_red><event></dark_red> <gray>- the dragons are out.");
        texts.put("timeline.sudden-death-title", "<dark_red>SUDDEN DEATH");
        texts.put("timeline.sudden-death-subtitle", "<gray>A dragon for every team - and the map starts falling");
        texts.put("timeline.header", "<prefix><gray>The round runs for <white><total></white> <gray>(<white><elapsed></white> so far):");
        texts.put("timeline.entry.done", "<dark_gray>  <at> <strikethrough><event>");
        texts.put("timeline.entry.next", "<yellow>  <at> <event> <gray>- in <white><time>");
        texts.put("timeline.entry.waiting", "<gray>  <at> <white><event>");
        texts.put("timeline.hover", "<white><event></white> <gray>at <white><at></white><newline><newline><gray><description>");
        texts.put("timeline.explained", "<dark_gray>  <description>");
        texts.put("timeline.hover-hint", "<dark_gray>  Hover over a line to read what it does.");
        texts.put("timeline.none", "<prefix><gray>This round has no timeline. <gray>timeline.yml has no events.");
        texts.put("timeline.not-running", "<prefix><red>The round is not running.");
        texts.put("timeline.skipped", "<prefix><gray>Set off <white><event></white> ahead of time.");
        texts.put("timeline.skip.done", "<prefix><gray>Nothing is left on the timeline.");

        texts.put("dragon.name", "<team> <dark_purple>Dragon");
        texts.put("dragon.bar", "<dark_purple>Dragon <gray>- <white><team>");
        texts.put("dragon.killed", "<prefix><gray>The dragon of <team> <gray>was brought down.");

        texts.put("wither.name", "<team> <dark_gray>Wither");
        texts.put("wither.wave", "<prefix><dark_gray>The withers are here <gray>- <white><amount></white> <gray>of them.");
        texts.put("wither.killed", "<prefix><gray>A wither of <team> <gray>was brought down.");

        texts.put("end.time-limit", "<prefix><gold>Time is up! <gray>The score decides this round.");
        texts.put("end.score.header", "<prefix><gray>Final score:");
        texts.put("end.score.entry", "<white>  <initial> <team> <dark_gray>- <white><points></white> <gray>(<white><beds></white> beds, <white><finals></white> finals, <white><kills></white> kills)");
        texts.put("end.score.entry-out", "<dark_gray>  <initial> <team> - <points> (<beds> beds, <finals> finals, <kills> kills, out)");
        texts.put("end.title.won", "<gold>VICTORY");
        texts.put("end.title.lost", "<red>GAME OVER");
        texts.put("end.title.nobody", "<gray>GAME OVER");
        texts.put("end.subtitle", "<gray><team> <gray>won the round");
        texts.put("end.subtitle-nobody", "<gray>Nobody won");
        texts.put("end.top.header", "<prefix><gray>The round belonged to:");
        texts.put("end.top.entry", "<gray>  <place>. <white><player></white> <dark_gray>(<team><dark_gray>) <gray>- <white><kills></white> kills, <white><finals></white> finals, <white><beds></white> beds");

        texts.put("generator.hologram", "<aqua><type></aqua> <dark_gray>| <gray>Tier <white><tier></white> <dark_gray>| <yellow><seconds>s");

        texts.put("generator.none", "<prefix><gray>No generator is running.");
        texts.put("generator.header", "<prefix><gray><count> generator(s):");
        texts.put("generator.entry", "<gray>- <white><owner></white> <dark_gray>at <gray><where>");
        texts.put("generator.middle", "middle");

        texts.put("build.too-high", "<prefix><red>You cannot build above <white><limit></white>.");
        texts.put("build.protected", "<prefix><red>You cannot build here.");
        texts.put("build.not-yours", "<prefix><red>You can only break what somebody built.");
        texts.put("item.platform-solid", "<prefix><red>A rescue platform cannot be broken. It goes by itself.");
        texts.put("item.tower-no-room", "<prefix><red>There is no room here for a tower.");
        texts.put("custom-item.pop-up-tower.name", "<white>Pop-Up Tower");
        texts.put("custom-item.pop-up-tower.lore", "<gray>Four walls and a ladder, around you, at once.");
        texts.put("setup.time-set", "<prefix><gray>Time: <white><cycle></white> <dark_gray>(<ticks>)");
        texts.put("setup.time.moving", "the sun moves");
        texts.put("setup.time.fixed", "held still");
        texts.put("chest.not-yours", "<prefix><red>That chest belongs to <white><team></white><red>.");
        texts.put("chest.deposited", "<prefix><gray>Stored <white><what></white> <gray>in your team chest.");
        texts.put("chest.nothing-to-deposit", "<prefix><gray>You are not carrying anything to store.");

        texts.put("bed.own", "<prefix><red>That is your own bed.");
        texts.put("bed.nobody", "the game");
        texts.put("bed.destroyed", "<prefix><gray>The bed of <white><team></white> was destroyed by <white><player></white>!");
        texts.put("bed.title", "<red>BED DESTROYED");
        texts.put("bed.subtitle", "<gray>You will no longer respawn!");

        texts.put("death.killed", "<white><player></white> <gray>was killed by <white><killer></white>.");
        texts.put("death.killed-final", "<white><player></white> <gray>was killed by <white><killer></white>. <red>FINAL KILL!");
        texts.put("death.alone", "<white><player></white> <gray>died.");
        texts.put("death.alone-final", "<white><player></white> <gray>died. <red>FINAL KILL!");
        texts.put("death.collected", "<prefix><gray>You took <white><what></white> off <white><player></white>.");
        texts.put("death.left", "<prefix><white><player></white> <gray>left the round.");
        texts.put("death.left-final", "<prefix><white><player></white> <gray>left the round for good.");
        texts.put("item.pearl-cooldown", "<prefix><gray>Another pearl in <white><seconds></white>s.");

        texts.put("watch.title", "<dark_gray>Watch");
        texts.put("watch.nobody", "<prefix><gray>There is nobody left to watch.");
        texts.put("watch.now", "<prefix><gray>Watching <white><player></white>.");
        texts.put("watch.gone", "<prefix><gray><player> is not standing any more.");
        texts.put("watch.team", "<gray>Team: <white><team>");
        texts.put("watch.kills", "<gray><kills> kills, <beds> beds");
        texts.put("watch.only-spectators", "<prefix><gray>Only somebody who is out can watch.");

        texts.put("stats.header", "<prefix><gray>This round so far:");
        texts.put("stats.entry", "<gray>  <place>. <white><player></white> <dark_gray>(<team><dark_gray>) <gray>- <white><kills></white> kills, <white><finals></white> finals, <white><beds></white> beds, <white><deaths></white> deaths");
        texts.put("stats.empty", "<prefix><gray>Nothing has happened yet.");
        texts.put("stats.footer", "<gray>  Running for <white><time></white>.");

        texts.put("respawn.title", "<red>You died");
        texts.put("respawn.subtitle", "<gray>Back in <white><seconds></white>s");
        texts.put("respawn.back", "<green>Go!");

        texts.put("sidebar.title", "<bold><red>BED<white>WARS");
        texts.put("sidebar.map", "<gray>Map: <white><map>");
        texts.put("sidebar.event", "<white><event> <gray>in <white><time>");
        texts.put("sidebar.sudden-death", "<dark_red>Sudden Death");
        texts.put("sidebar.team.bed", "<white><initial> <gray><team> <green>\u2714");
        texts.put("sidebar.team.alive", "<white><initial> <gray><team> <yellow><players>");
        texts.put("sidebar.team.out", "<dark_gray><initial> <team> <red>\u2716");
        texts.put("sidebar.you", " <gray>(you)");
        texts.put("sidebar.kills", "<gray>Kills: <white><kills>");
        texts.put("sidebar.beds", "<gray>Beds: <white><beds>");
        texts.put("sidebar.name-prefix", "<gray>[<initial><gray>] ");

        texts.put("shop.title", "<dark_gray>Shop <dark_gray>\u00bb <gray><category>");
        texts.put("shop.category.open", "<green>You are looking at this page");
        texts.put("shop.price", "<gray>Cost: <white><amount></white> <gray><currency>");
        texts.put("shop.click-to-buy", "<green>Click to buy");
        texts.put("shop.too-expensive", "<red>You cannot afford this");
        texts.put("shop.owned", "<green>You already own this");
        texts.put("shop.maxed", "<green>Fully upgraded");
        texts.put("shop.step", "<gray>Step <white><level></white><gray>/<white><maximum>");
        texts.put("shop.closed", "<prefix><red>The shop is switched off for this round.");
        texts.put("shop.bought", "<prefix><green>Bought <white><amount>x <item></white>.");
        texts.put("shop.already-owned", "<prefix><gray>You already own <white><item></white>.");
        texts.put("shop.cannot-afford", "<prefix><red>You need <white><amount></white> more <white><currency></white>.");
        texts.put("shop.empty", "<prefix><red>This server sells nothing. <gray>shop.yml has no entries.");
        texts.put("shop.not-playing", "<prefix><gray>Only players in the round can shop.");
        texts.put("shop.enemy-only", "<prefix><red>That is only sold at another team's shop keeper.");
        texts.put("shop.keeper.items", "<white><team> <yellow>Item Shop");
        texts.put("shop.keeper.upgrades", "<white><team> <aqua>Team Upgrades");

        texts.put("upgrade.title", "<dark_gray>Team Upgrades");
        texts.put("upgrade.level", "<gray>Level: <white><level></white><gray>/<white><maximum>");
        texts.put("upgrade.maxed-lore", "<green>Fully upgraded");
        texts.put("upgrade.maxed", "<prefix><gray><upgrade> is already at its highest level.");
        texts.put("upgrade.bought", "<prefix><white><player></white> <gray>bought <white><upgrade> <level></white> <gray>for the team.");
        texts.put("upgrade.no-team", "<prefix><red>Only a player with a team can buy upgrades.");

        texts.put("trap.queue.empty", "<dark_gray>Trap slot <position> <gray>- empty");
        texts.put("trap.queue.filled", "<white>Trap slot <position> <gray>- <yellow><trap>");
        texts.put("trap.queue-full", "<prefix><red>Your trap queue is full <gray>(<maximum>).");
        texts.put("trap.queue-full-lore", "<red>The trap queue is full (<maximum>)");
        texts.put("trap.bought", "<prefix><white><player></white> <gray>queued <white><trap></white> <dark_gray>(#<position>)");
        texts.put("trap.title", "<red>TRAP SET OFF");
        texts.put("trap.subtitle", "<gray><trap>");
        texts.put("trap.set-off", "<prefix><red><trap></red> <gray>went off at your base!");
        texts.put("trap.set-off-alarm", "<prefix><red><trap></red> <gray>caught <white><player></white> <gray>of <white><team></white> <gray>at your base!");

        texts.put("item.magic-milk", "<prefix><gray>Traps ignore you for <white><seconds></white>s.");
        texts.put("item.minion-name", "<gray><team> <dark_gray>| <white><player>");
        texts.put("item.no-room", "<prefix><red>There is no room for that here.");
        texts.put("item.platform-no-room", "<prefix><gray>There is solid ground under you already.");
        texts.put("compass.now-tracking", "<prefix><gray>The compass now points at <white><team></white>.");
        texts.put("compass.tracking", "<gray>Tracking <white><team>");
        texts.put("compass.idle", "<gray>Right click to pick a team");
        texts.put("compass.nobody-left", "<prefix><gray>There is no other team left to track.");
        texts.put("compass.cannot-afford", "<prefix><red>Tracking <white><team></white> costs <white><amount> <currency></white>.");

        texts.put("chat.global", "<gray>[<white>ALL<gray>] <white><player><gray>: <white><message>");
        texts.put("chat.team", "<gray>[<aqua>TEAM<gray>] <white><player><gray>: <white><message>");
        texts.put("chat.no-team", "no team");

        texts.put("phase.lobby", "Waiting");
        texts.put("phase.running", "Running");
        texts.put("phase.ending", "Over");
        return texts;
    }
}

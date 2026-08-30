package de.schnorrenbergers.bedwars.commands;

import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.addon.Addon;
import de.schnorrenbergers.bedwars.addon.AddonRegistry;
import de.schnorrenbergers.bedwars.admin.AdminMenu;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.api.BedwarsGameEndEvent;
import de.schnorrenbergers.bedwars.game.TeamColor;
import de.schnorrenbergers.bedwars.game.phase.IngamePhase;
import de.schnorrenbergers.bedwars.game.timeline.Timeline;
import de.schnorrenbergers.bedwars.game.timeline.TimelineEvent;
import de.schnorrenbergers.bedwars.generator.Generator;
import de.schnorrenbergers.bedwars.generator.GeneratorManager;
import de.schnorrenbergers.bedwars.map.ArenaMap;
import de.schnorrenbergers.bedwars.lobby.AddonMenu;
import de.schnorrenbergers.bedwars.map.setup.SetupCommand;
import de.schnorrenbergers.bedwars.spectator.WatchMenu;
import de.schnorrenbergers.bedwars.stats.RoundStats;
import de.schnorrenbergers.bedwars.stats.StatsTracker;
import de.schnorrenbergers.bedwars.shop.ShopMenu;
import de.schnorrenbergers.bedwars.shop.upgrade.UpgradeMenu;
import de.schnorrenbergers.bedwars.util.Messages;
import de.schnorrenbergers.bedwars.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@code /bw}, the one command of the plugin.
 * <p>
 * Everything the plugin can be told lives under here as a subcommand, so the map setup, the addons and the
 * round itself do not each need their own name. The later phases add {@code setup}, {@code map} and
 * {@code start} next to what is already here.
 */
public class BedwarsCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "bedwars.admin";

    private final SetupCommand setup = new SetupCommand();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            usage(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "status" -> status(sender);
            case "reload" -> reload(sender);
            case "admin", "settings" -> admin(sender);
            case "addons" -> addons(sender);
            case "addon" -> addon(sender, args);
            case "start" -> start(sender);
            case "generators" -> generators(sender);
            case "timeline" -> timeline(sender, args);
            case "stats" -> stats(sender);
            case "watch" -> watch(sender);
            case "shop" -> shop(sender);
            case "upgrades" -> upgrades(sender);
            case "stop" -> stop(sender);
            case "setup" -> {
                if (!denied(sender)) setup.handle(sender, tail(args));
            }
            default -> Messages.send(sender, "command.unknown", "input", args[0]);
        }
        return true;
    }

    private void usage(CommandSender sender) {
        Messages.send(sender, "command.usage", "usage",
                "status | setup | start | stop | admin | generators | timeline [skip] | shop | upgrades"
                        + " | stats | watch | reload | addons | addon <id> on|off|default");
    }

    /**
     * Says what this server is hosting and where the round stands.
     */
    private void status(CommandSender sender) {
        Game game = Bedwars.getInstance().getGame();
        Messages.send(sender, "status.header");
        Messages.send(sender, "status.mode",
                "mode", game.getMode().getDisplayName(),
                "teams", String.valueOf(game.getMode().getTeamCount()),
                "size", String.valueOf(game.getMode().getTeamSize()));
        Messages.send(sender, "status.phase", "phase", Messages.raw(game.getPhaseType().getMessageKey()));
        Messages.send(sender, "status.players",
                "online", String.valueOf(game.getOnlineCount()),
                "maximum", String.valueOf(game.getMaximumPlayers()));
        ArenaMap arena = game.getArena();
        if (arena == null) {
            Messages.send(sender, "status.map-missing");
        } else {
            Messages.send(sender, "status.map", "map", arena.getName());
        }
        if (game.isSetupMode()) Messages.send(sender, "status.setup");
    }

    private void reload(CommandSender sender) {
        if (denied(sender)) return;
        long started = System.currentTimeMillis();
        try {
            Bedwars.getInstance().reload();
            Messages.send(sender, "reload.done",
                    "millis", String.valueOf(System.currentTimeMillis() - started));
        } catch (Exception e) {
            Messages.send(sender, "reload.failed", "error", String.valueOf(e.getMessage()));
        }
    }

    /**
     * Starts the round now, without waiting for the lobby to fill.
     * <p>
     * Exists for the two moments where waiting is wrong: testing a map you have just set up, and an event
     * that decides the round begins.
     */
    private void start(CommandSender sender) {
        if (denied(sender)) return;
        Game game = Bedwars.getInstance().getGame();
        if (!game.isWaiting()) {
            Messages.send(sender, "command.start.running");
            return;
        }
        if (!game.canStart()) {
            Messages.send(sender, "command.start.impossible");
            return;
        }
        game.setPhase(new IngamePhase(game));
        Messages.send(sender, "command.start.done");
    }

    /**
     * Lists every generator with its level and how long until it drops again.
     * <p>
     * The floating text says the same thing, but only to somebody standing in front of it - this answers
     * "is that generator running at all" from the console, which is where you ask it while building a map.
     */
    private void generators(CommandSender sender) {
        GeneratorManager manager = Bedwars.getInstance().getGame().getGenerators();
        List<Generator> all = manager == null ? List.of() : manager.all();
        if (all.isEmpty()) {
            Messages.send(sender, "generator.none");
            return;
        }
        Messages.send(sender, "generator.header", "count", String.valueOf(all.size()));
        for (Generator generator : all) {
            Messages.send(sender, "generator.entry",
                    "owner", generator.getOwner() == null
                            ? Messages.raw("generator.middle")
                            : generator.getOwner().getColor().getDisplayName(),
                    "where", (int) generator.getLocation().getX() + " "
                            + (int) generator.getLocation().getY() + " "
                            + (int) generator.getLocation().getZ());
            sender.sendMessage(generator.describe());
        }
    }

    /**
     * Lists what the round still has coming, and lets an operator pull the next one forward.
     * <p>
     * The second half is the only way to test an endgame at all: bed destruction is half an hour into a
     * default round, and nobody finds out that way whether it works.
     */
    private void timeline(CommandSender sender, String[] args) {
        Game game = Bedwars.getInstance().getGame();
        Timeline timeline = game.getTimeline();
        if (timeline == null || timeline.getEvents().isEmpty()) {
            Messages.send(sender, "timeline.none");
            return;
        }
        if (args.length > 1 && args[1].equalsIgnoreCase("skip")) {
            skip(sender, game, timeline);
            return;
        }
        List<TimelineEvent> events = timeline.getEvents();
        Messages.send(sender, "timeline.header",
                "total", Text.clock(events.getLast().seconds()),
                "elapsed", Text.clock(timeline.getElapsedSeconds()));
        TimelineEvent next = timeline.getNext();
        for (TimelineEvent event : events) {
            String key = timeline.hasHappened(event) ? "timeline.entry.done"
                    : event.equals(next) ? "timeline.entry.next" : "timeline.entry.waiting";
            sender.sendMessage(explain(event, Messages.get(key,
                    "at", Text.clock(event.seconds()),
                    "event", Text.plain(event.displayName()),
                    "time", Text.clock(timeline.getSecondsUntilNext()))));
        }
        Messages.send(sender, "timeline.hover-hint");
    }

    /**
     * Hangs the description of an event onto its line.
     * <p>
     * The sidebar has room for a name and a countdown and nothing else, so a player reads "Diamond II in
     * 3:20" and learns exactly when something will happen to them without ever learning what. The line
     * here is the same name, and holding the mouse over it says what it means.
     *
     * @param event the event
     * @param line  the line it is written on
     * @return that line, with the description as its hover text
     */
    private static Component explain(TimelineEvent event, Component line) {
        if (!event.hasDescription()) return line;
        return line.hoverEvent(HoverEvent.showText(Messages.get("timeline.hover",
                "event", Text.plain(event.displayName()),
                "at", Text.clock(event.seconds()),
                "description", event.description())));
    }

    /**
     * Sets the next timeline event off now.
     */
    private void skip(CommandSender sender, Game game, Timeline timeline) {
        if (denied(sender)) return;
        if (!game.isRunning()) {
            Messages.send(sender, "timeline.not-running");
            return;
        }
        TimelineEvent fired = timeline.skip(game);
        if (fired == null) {
            Messages.send(sender, "timeline.skip.done");
            return;
        }
        Messages.send(sender, "timeline.skipped", "event", Text.plain(fired.displayName()));
    }

    /**
     * Shows where the round stands, by the same scoring that decides it.
     */
    private void stats(CommandSender sender) {
        Game game = Bedwars.getInstance().getGame();
        StatsTracker tracker = Bedwars.getInstance().getStats();
        RoundStats round = tracker == null
                ? RoundStats.of(game, elapsed(game), Bedwars.getInstance().getTimelineSettings().getWeights())
                : tracker.snapshot(game);
        if (round.rows().isEmpty()) {
            Messages.send(sender, "stats.empty");
            return;
        }
        Messages.send(sender, "stats.header");
        int place = 1;
        for (RoundStats.Row row : round.rows()) {
            Messages.send(sender, "stats.entry",
                    "place", String.valueOf(place++),
                    "player", row.name(),
                    "team", row.team() == null ? Messages.raw("chat.no-team") : row.team().getDisplayName(),
                    "kills", String.valueOf(row.kills()),
                    "finals", String.valueOf(row.finals()),
                    "beds", String.valueOf(row.beds()),
                    "deaths", String.valueOf(row.deaths()));
        }
        Messages.send(sender, "stats.footer", "time", Text.clock(round.seconds()));
    }

    /**
     * @param game the round
     * @return how long it has been running
     */
    private static int elapsed(Game game) {
        return game.getTimeline() == null ? 0 : game.getTimeline().getElapsedSeconds();
    }

    /**
     * Opens the list of everybody still standing, for somebody who is out.
     */
    private void watch(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "command.players-only");
            return;
        }
        Game game = Bedwars.getInstance().getGame();
        GamePlayer participant = game.get(player);
        // whoever is still in the round has somewhere else to be
        if (participant != null && participant.isAlive() && !player.hasPermission(PERMISSION)) {
            Messages.send(sender, "watch.only-spectators");
            return;
        }
        WatchMenu.open(player);
    }

    /**
     * Opens the item shop without a villager.
     * <p>
     * For the two cases a villager cannot cover: a map whose shop spots are not set yet, and checking a
     * price change in {@code shop.yml} without walking back to a base.
     */
    private void shop(CommandSender sender) {
        if (denied(sender)) return;
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "command.players-only");
            return;
        }
        ShopMenu.open(player, Bedwars.getInstance().getGame().get(player) == null
                ? null : Bedwars.getInstance().getGame().get(player).getTeam());
    }

    /**
     * Opens the team upgrades without a villager, for the same reason as {@link #shop(CommandSender)}.
     */
    private void upgrades(CommandSender sender) {
        if (denied(sender)) return;
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "command.players-only");
            return;
        }
        UpgradeMenu.open(player);
    }

    /**
     * Ends the round with nobody winning.
     */
    private void stop(CommandSender sender) {
        if (denied(sender)) return;
        Game game = Bedwars.getInstance().getGame();
        if (!game.isRunning()) {
            Messages.send(sender, "command.stop.not-running");
            return;
        }
        game.end(null, BedwarsGameEndEvent.Reason.STOPPED);
        Messages.send(sender, "command.stop.done");
    }

    /**
     * Lists every addon with its state and who decided it - as a menu for somebody who is standing in the
     * lobby, and as text for the console, which is the other place this question is asked from.
     */
    /**
     * Opens the switches of the server: 1.8 combat, the locator bar, the sun, and the rest of them.
     */
    private void admin(CommandSender sender) {
        if (denied(sender)) return;
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "command.players-only");
            return;
        }
        AdminMenu.open(player);
    }

    private void addons(CommandSender sender) {
        AddonRegistry registry = Bedwars.getInstance().getAddons();
        if (sender instanceof Player player && (player.hasPermission(PERMISSION) || player.isOp())) {
            AddonMenu.open(player);
            return;
        }
        Messages.send(sender, "addon.header");
        for (Addon addon : registry.all()) {
            boolean on = registry.isEnabled(addon.getId());
            Messages.send(sender, on ? "addon.entry.on" : "addon.entry.off",
                    "addon", addon.getId(),
                    "description", addon.getDescription());
            Messages.send(sender, "addon.entry.source",
                    "source", registry.getSource(addon.getId()).getLabel());
        }
    }

    /**
     * Switches one addon for this round.
     */
    private void addon(CommandSender sender, String[] args) {
        if (denied(sender)) return;
        if (args.length < 3) {
            Messages.send(sender, "command.usage", "usage", "addon <id> on|off|default");
            return;
        }
        AddonRegistry registry = Bedwars.getInstance().getAddons();
        String id = args[1];
        if (!registry.has(id)) {
            Messages.send(sender, "addon.unknown", "addon", id);
            return;
        }
        Game game = Bedwars.getInstance().getGame();
        if (!game.isWaiting()) {
            Messages.send(sender, "addon.locked", "addon", id);
            return;
        }
        switch (args[2].toLowerCase(Locale.ROOT)) {
            case "on" -> registry.setSessionOverride(id, true);
            case "off" -> registry.setSessionOverride(id, false);
            case "default", "reset" -> registry.clearSessionOverride(id);
            default -> {
                Messages.send(sender, "command.usage", "usage", "addon <id> on|off|default");
                return;
            }
        }
        registry.apply(game);
        Messages.send(sender, "addon.switched",
                "addon", id,
                "state", Messages.raw(registry.isEnabled(id) ? "addon.state.on" : "addon.state.off"));
    }

    /**
     * @return whether the sender may not do this, telling them so
     */
    private boolean denied(CommandSender sender) {
        if (sender.hasPermission(PERMISSION) || sender.isOp()) return false;
        Messages.send(sender, "command.no-permission");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length <= 1) {
            return filter(List.of("status", "setup", "start", "stop", "admin", "generators", "timeline",
                            "shop", "upgrades", "stats", "watch", "reload", "addons", "addon"),
                    args.length == 0 ? "" : args[0]);
        }
        if (args[0].equalsIgnoreCase("timeline") && args.length == 2) {
            return filter(List.of("skip"), args[1]);
        }
        if (args[0].equalsIgnoreCase("setup")) return completeSetup(args);
        if (args[0].equalsIgnoreCase("addon")) {
            if (args.length == 2) {
                List<String> ids = new ArrayList<>();
                for (Addon addon : Bedwars.getInstance().getAddons().all()) ids.add(addon.getId());
                return filter(ids, args[1]);
            }
            if (args.length == 3) return filter(List.of("on", "off", "default"), args[2]);
        }
        return List.of();
    }

    /**
     * Completes {@code /bw setup ...}, which is where most of the typing happens.
     */
    private List<String> completeSetup(String[] args) {
        List<String> subcommands = new ArrayList<>(List.of(
                "list", "lobby", "spectator", "team", "gen", "build", "mode", "name", "check", "save", "exit"));
        if (args.length == 2) {
            subcommands.addAll(Bedwars.getInstance().getMaps().list());
            return filter(subcommands, args[1]);
        }
        if (args[1].equalsIgnoreCase("team")) {
            if (args.length == 3) return filter(colors(), args[2]);
            if (args.length == 4) {
                return filter(List.of("spawn", "bed", "shop", "upgrade", "generator", "protection", "remove"),
                        args[3]);
            }
        }
        if (args[1].equalsIgnoreCase("gen")) {
            if (args.length == 3) return filter(List.of("add", "remove"), args[2]);
            if (args.length == 4 && args[2].equalsIgnoreCase("add")) {
                return filter(List.of("DIAMOND", "EMERALD"), args[3]);
            }
        }
        if (args[1].equalsIgnoreCase("mode")) {
            if (args.length == 3) {
                List<String> modes = new ArrayList<>();
                Bedwars.getInstance().getModeSettings().all().forEach(mode -> modes.add(mode.getId()));
                return filter(modes, args[2]);
            }
            List<String> options = new ArrayList<>(colors());
            options.add("auto");
            return filter(options, args[args.length - 1]);
        }
        return List.of();
    }

    /**
     * @return every team colour, as it is typed
     */
    private static List<String> colors() {
        List<String> names = new ArrayList<>();
        for (TeamColor color : TeamColor.values()) names.add(color.name());
        return names;
    }

    /**
     * @param args the whole command
     * @return everything after the subcommand
     */
    private static String[] tail(String[] args) {
        String[] rest = new String[Math.max(0, args.length - 1)];
        System.arraycopy(args, 1, rest, 0, rest.length);
        return rest;
    }

    /**
     * @param options what could be meant
     * @param typed   what has been typed so far
     * @return the options that still fit
     */
    private static List<String> filter(List<String> options, String typed) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}

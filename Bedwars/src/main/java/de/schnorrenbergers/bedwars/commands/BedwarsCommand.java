package de.schnorrenbergers.bedwars.commands;

import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.addon.Addon;
import de.schnorrenbergers.bedwars.addon.AddonRegistry;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
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
            case "addons" -> addons(sender);
            case "addon" -> addon(sender, args);
            default -> Messages.send(sender, "command.unknown", "input", args[0]);
        }
        return true;
    }

    private void usage(CommandSender sender) {
        Messages.send(sender, "command.usage", "usage", "status | reload | addons | addon <id> on|off|default");
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
        String map = game.getSettings().getMap();
        if (map == null || map.isBlank()) {
            Messages.send(sender, "status.map-missing");
        } else {
            Messages.send(sender, "status.map", "map", map);
        }
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
     * Lists every addon with its state and who decided it.
     */
    private void addons(CommandSender sender) {
        AddonRegistry registry = Bedwars.getInstance().getAddons();
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
            return filter(List.of("status", "reload", "addons", "addon"), args.length == 0 ? "" : args[0]);
        }
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
     * @param options what could be meant
     * @param typed   what has been typed so far
     * @return the options that still fit
     */
    private static List<String> filter(List<String> options, String typed) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}

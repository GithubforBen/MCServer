package de.schnorrenbergers.lobby.npc;

import de.hems.paper.commands.WarpCommand;
import de.schnorrenbergers.lobby.LobbyWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * {@code /npc} - putting up the npcs of the lobby.
 * <p>
 * Like the parkour and the lotto stand, an npc goes where the admin is standing and faces where they are
 * looking: walking there is easier than typing coordinates.
 */
public class NpcCommand implements CommandExecutor, TabCompleter {

    public static final String PERMISSION = "network.npc.admin";

    /** What minecraft accepts as a player name, which is what a skin is looked up by. */
    private static final Pattern PLAYER_NAME = Pattern.compile("[A-Za-z0-9_]{3,16}");

    private static final List<String> SUBCOMMANDS =
            List.of("list", "warp", "events", "name", "skin", "ziel", "hier", "tp", "weg");

    private final LobbyNpcs npcs;

    public NpcCommand(LobbyNpcs npcs) {
        this.npcs = npcs;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("NPCs stellt man im Spiel auf.");
            return true;
        }
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(Component.text("Dafür fehlt dir die Berechtigung.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            list(player);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list", "liste" -> list(player);
            case "warp", "server" -> createWarp(player, args);
            case "events", "event", "kalender" -> createEvents(player, args);
            case "name" -> rename(player, args);
            case "skin" -> skin(player, args);
            case "ziel", "target" -> target(player, args);
            case "hier", "move" -> move(player, args);
            case "tp" -> teleport(player, args);
            case "weg", "remove", "delete" -> remove(player, args);
            default -> usage(player);
        }
        return true;
    }

    private void usage(Player player) {
        player.sendMessage(Component.text("/npc warp <server> [name]  ·  /npc events [name]", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/npc name <id> <name>  ·  /npc skin <id> <spieler|aus>", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/npc ziel <id> <server>  ·  /npc hier <id>  ·  /npc tp <id>  ·  /npc weg <id>",
                NamedTextColor.GRAY));
        player.sendMessage(Component.text("Namen dürfen &-Farbcodes enthalten, z.B. &bSurvival", NamedTextColor.DARK_GRAY));
    }

    private void list(Player player) {
        if (npcs.all().isEmpty()) {
            player.sendMessage(Component.text("In der Lobby steht noch kein NPC.", NamedTextColor.GRAY));
            usage(player);
            return;
        }
        player.sendMessage(Component.text("NPCs der Lobby:", NamedTextColor.GOLD));
        for (LobbyNpc npc : npcs.all()) {
            Location at = npc.getLocation();
            String what = npc.getType() == NpcType.WARP ? "→ " + npc.getTarget() : "Eventkalender";
            player.sendMessage(Component.text(" " + npc.getId(), NamedTextColor.WHITE)
                    .append(Component.text(" · ", NamedTextColor.DARK_GRAY))
                    .append(npc.displayName())
                    .append(Component.text(" · " + what, NamedTextColor.GRAY))
                    .append(Component.text(" · " + at.getBlockX() + " " + at.getBlockY() + " " + at.getBlockZ(),
                            NamedTextColor.DARK_GRAY))
                    .hoverEvent(HoverEvent.showText(Component.text("Hinspringen")))
                    .clickEvent(ClickEvent.runCommand("/npc tp " + npc.getId())));
        }
    }

    private void createWarp(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("/npc warp <server> [name]", NamedTextColor.RED));
            return;
        }
        Location at = spot(player);
        if (at == null) return;
        String server = args[1].toUpperCase(Locale.ROOT);
        String name = args.length > 2 ? join(args, 2) : "&b" + pretty(server);
        LobbyNpc npc = npcs.create(server, NpcType.WARP, server, name, at);
        created(player, npc);
    }

    private void createEvents(Player player, String[] args) {
        Location at = spot(player);
        if (at == null) return;
        String name = args.length > 1 ? join(args, 1) : "&6Events";
        LobbyNpc npc = npcs.create("events", NpcType.EVENTS, null, name, at);
        created(player, npc);
    }

    private void created(Player player, LobbyNpc npc) {
        player.sendMessage(Component.text("✓ NPC '" + npc.getId() + "' steht hier.", NamedTextColor.GREEN));
        player.sendMessage(Component.text("Skin: /npc skin " + npc.getId() + " <spieler>", NamedTextColor.GRAY)
                .clickEvent(ClickEvent.suggestCommand("/npc skin " + npc.getId() + " ")));
    }

    private void rename(Player player, String[] args) {
        LobbyNpc npc = npc(player, args, 3, "/npc name <id> <name>");
        if (npc == null) return;
        npc.setName(join(args, 2));
        npcs.changed(npc);
        player.sendMessage(Component.text("✓ Heißt jetzt ", NamedTextColor.GREEN).append(npc.displayName()));
    }

    private void skin(Player player, String[] args) {
        LobbyNpc npc = npc(player, args, 3, "/npc skin <id> <spieler|aus>");
        if (npc == null) return;
        String skin = args[2];
        if (skin.equalsIgnoreCase("aus") || skin.equalsIgnoreCase("none")) {
            npc.setSkin(null);
        } else if (!PLAYER_NAME.matcher(skin).matches()) {
            player.sendMessage(Component.text("'" + skin + "' ist kein Minecraft-Name.", NamedTextColor.RED));
            return;
        } else {
            npc.setSkin(skin);
        }
        npcs.changed(npc);
        player.sendMessage(Component.text(npc.getSkin() == null ? "✓ Trägt wieder den Standard-Skin."
                : "✓ Trägt jetzt den Skin von " + npc.getSkin() + ".", NamedTextColor.GREEN));
    }

    private void target(Player player, String[] args) {
        LobbyNpc npc = npc(player, args, 3, "/npc ziel <id> <server>");
        if (npc == null) return;
        if (npc.getType() != NpcType.WARP) {
            player.sendMessage(Component.text("Nur ein Warp-NPC hat ein Ziel.", NamedTextColor.RED));
            return;
        }
        npc.setTarget(args[2].toUpperCase(Locale.ROOT));
        npcs.changed(npc);
        player.sendMessage(Component.text("✓ Schickt jetzt nach " + npc.getTarget() + ".", NamedTextColor.GREEN));
    }

    private void move(Player player, String[] args) {
        LobbyNpc npc = npc(player, args, 2, "/npc hier <id>");
        if (npc == null) return;
        Location at = spot(player);
        if (at == null) return;
        npc.setLocation(at);
        npcs.changed(npc);
        player.sendMessage(Component.text("✓ Steht jetzt hier.", NamedTextColor.GREEN));
    }

    private void teleport(Player player, String[] args) {
        LobbyNpc npc = npc(player, args, 2, "/npc tp <id>");
        if (npc == null) return;
        player.teleport(npc.getLocation());
    }

    private void remove(Player player, String[] args) {
        LobbyNpc npc = npc(player, args, 2, "/npc weg <id>");
        if (npc == null) return;
        npcs.remove(npc);
        player.sendMessage(Component.text("✓ NPC '" + npc.getId() + "' ist weg.", NamedTextColor.GREEN));
    }

    // ------------------------------------------------------------------ helpers

    private @Nullable LobbyNpc npc(Player player, String[] args, int needed, String usage) {
        if (args.length < needed) {
            player.sendMessage(Component.text(usage, NamedTextColor.RED));
            return null;
        }
        LobbyNpc npc = npcs.get(args[1]);
        if (npc == null) {
            player.sendMessage(Component.text("Es gibt keinen NPC '" + args[1] + "'. /npc list zeigt alle.",
                    NamedTextColor.RED));
        }
        return npc;
    }

    /**
     * @return where an npc put up by this player stands: the middle of their block, facing where they look
     */
    private @Nullable Location spot(Player player) {
        if (LobbyWorld.get() == null || !player.getWorld().equals(LobbyWorld.get())) {
            player.sendMessage(Component.text("NPCs gehören in die Lobby-Welt.", NamedTextColor.RED));
            return null;
        }
        Location at = player.getLocation().getBlock().getLocation().add(0.5, 0, 0.5);
        at.setY(player.getLocation().getY());
        at.setYaw(player.getLocation().getYaw());
        at.setPitch(0f);
        return at;
    }

    private static String join(String[] args, int from) {
        return String.join(" ", Arrays.copyOfRange(args, from, args.length));
    }

    private static String pretty(String server) {
        String lower = server.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission(PERMISSION)) return List.of();
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(SUBCOMMANDS);
        } else if (args.length == 2 && List.of("warp", "server").contains(args[0].toLowerCase(Locale.ROOT))) {
            options.addAll(WarpCommand.knownServers());
        } else if (args.length == 3 && List.of("ziel", "target").contains(args[0].toLowerCase(Locale.ROOT))) {
            options.addAll(WarpCommand.knownServers());
        } else if (args.length == 2 && !List.of("warp", "server", "events", "event", "kalender", "list")
                .contains(args[0].toLowerCase(Locale.ROOT))) {
            for (LobbyNpc npc : npcs.all()) options.add(npc.getId());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("skin")) {
            options.add("aus");
            for (Player online : sender.getServer().getOnlinePlayers()) options.add(online.getName());
        }
        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        options.removeIf(option -> !option.toLowerCase(Locale.ROOT).startsWith(typed));
        return options;
    }
}

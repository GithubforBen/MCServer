package de.schnorrenbergers.bedwars.map.setup;

import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.game.GameMode;
import de.schnorrenbergers.bedwars.game.TeamColor;
import de.schnorrenbergers.bedwars.map.ArenaMap;
import de.schnorrenbergers.bedwars.map.GeneratorSpot;
import de.schnorrenbergers.bedwars.map.MapPoint;
import de.schnorrenbergers.bedwars.map.MapValidator;
import de.schnorrenbergers.bedwars.map.TeamSpot;
import de.schnorrenbergers.bedwars.util.Messages;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Everything under {@code /bw setup}.
 * <p>
 * There is no wand. Every point is taken from where you stand or from the block you look at, because that
 * is what you are doing anyway while walking a map - and a wand would be one more thing to lose, hand over
 * and clean up afterwards.
 */
public class SetupCommand {

    /** How far {@code gen remove} looks for the generator that was meant. */
    private static final double REMOVE_RANGE = 6.0d;
    /** How far the setup commands look for the block you mean. */
    private static final int REACH = 6;

    /**
     * @param sender who typed it
     * @param args   everything after {@code /bw setup}
     */
    public void handle(CommandSender sender, String[] args) {
        if (args.length == 0) {
            status(sender);
            return;
        }
        String subcommand = args[0].toLowerCase(Locale.ROOT);
        // asking what is there and what is missing needs no position, so the console can do both
        if (subcommand.equals("list")) {
            list(sender);
            return;
        }
        if (subcommand.equals("check")) {
            check(sender);
            return;
        }
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "command.players-only");
            return;
        }
        switch (subcommand) {
            case "lobby" -> setLobby(player);
            case "spectator" -> setSpectator(player);
            case "team" -> team(player, args);
            case "gen" -> generator(player, args);
            case "build" -> build(player, args);
            case "mode" -> mode(player, args);
            case "name" -> displayName(player, args);
            case "save" -> save(player);
            case "exit" -> exit(player);
            default -> start(player, args[0]);
        }
    }

    // ------------------------------------------------------------- session

    /**
     * Says which map is open and what is still missing.
     */
    private void status(CommandSender sender) {
        SetupSession session = session();
        if (session == null) {
            Messages.send(sender, "setup.usage", "usage",
                    "setup <map> | list | lobby | spectator | team | gen | build | mode | name | check | save | exit");
            return;
        }
        Messages.send(sender, "setup.status",
                "map", session.getMap().getName(),
                "state", Messages.raw(session.isDirty() ? "setup.state.unsaved" : "setup.state.saved"));
        check(sender);
    }

    private void list(CommandSender sender) {
        List<String> maps = Bedwars.getInstance().getMaps().list();
        if (maps.isEmpty()) {
            Messages.send(sender, "setup.no-maps", "folder", Bedwars.getInstance().getMaps().getDirectory().getPath());
            return;
        }
        Messages.send(sender, "setup.list.header", "count", String.valueOf(maps.size()));
        for (String map : maps) {
            Messages.send(sender, "setup.list.entry", "map", map);
        }
    }

    /**
     * Opens a map: copies it in, loads it and puts you on it.
     */
    private void start(Player player, String name) {
        SetupSession running = session();
        if (running != null) {
            Messages.send(player, "setup.already", "map", running.getMap().getName());
            return;
        }
        if (!Bedwars.getInstance().getMaps().hasWorld(name)) {
            Messages.send(player, "setup.unknown-map", "map", name,
                    "folder", Bedwars.getInstance().getMaps().getDirectory().getPath());
            return;
        }
        World world = Bedwars.getInstance().getMapLoader().load(name);
        if (world == null) {
            Messages.send(player, "setup.world-failed", "map", name);
            return;
        }
        ArenaMap map = Bedwars.getInstance().getMaps().load(name);
        Bedwars.getInstance().startSetup(new SetupSession(map, world, player.getUniqueId()));

        MapPoint lobby = map.getLobby();
        player.teleport(lobby == null ? world.getSpawnLocation() : lobby.toLocation(world));
        Messages.send(player, "setup.started", "map", name);
        check(player);
    }

    private void save(Player player) {
        SetupSession session = requireSession(player);
        if (session == null) return;
        boolean written = Bedwars.getInstance().getMaps().save(session.getMap())
                && Bedwars.getInstance().getMapLoader().saveBack(session.getMap().getName(), session.getWorld());
        if (!written) {
            Messages.send(player, "setup.save-failed", "map", session.getMap().getName());
            return;
        }
        session.clean();
        Messages.send(player, "setup.saved", "map", session.getMap().getName());
    }

    private void exit(Player player) {
        SetupSession session = requireSession(player);
        if (session == null) return;
        if (session.isDirty()) {
            // said once, and the next exit goes through: refusing outright would trap somebody who really
            // does want to throw their changes away
            session.clean();
            Messages.send(player, "setup.unsaved");
            return;
        }
        Bedwars.getInstance().stopSetup();
        Messages.send(player, "setup.exited");
    }

    // -------------------------------------------------------------- points

    private void setLobby(Player player) {
        SetupSession session = requireSession(player);
        if (session == null) return;
        session.getMap().setLobby(MapPoint.of(player.getLocation()));
        session.touch();
        told(player, "waiting lobby", player.getLocation().getBlockX(), player.getLocation().getBlockY(),
                player.getLocation().getBlockZ());
    }

    private void setSpectator(Player player) {
        SetupSession session = requireSession(player);
        if (session == null) return;
        session.getMap().setSpectator(MapPoint.of(player.getLocation()));
        session.touch();
        told(player, "spectator spawn", player.getLocation().getBlockX(), player.getLocation().getBlockY(),
                player.getLocation().getBlockZ());
    }

    /**
     * {@code /bw setup team <colour> spawn|bed|shop|upgrade|generator|protection <n>|remove}
     */
    private void team(Player player, String[] args) {
        SetupSession session = requireSession(player);
        if (session == null) return;
        if (args.length < 3) {
            Messages.send(player, "setup.usage", "usage",
                    "setup team <colour> spawn|bed|shop|upgrade|generator|protection <blocks>|remove");
            return;
        }
        TeamColor color = TeamColor.byName(args[1]);
        if (color == null) {
            Messages.send(player, "setup.unknown-team", "input", args[1]);
            return;
        }
        ArenaMap map = session.getMap();
        if (args[2].equalsIgnoreCase("remove")) {
            map.removeTeam(color);
            session.touch();
            Messages.send(player, "setup.removed", "what", "team " + color.getDisplayName());
            return;
        }
        TeamSpot spot = map.getOrCreateTeam(color);
        switch (args[2].toLowerCase(Locale.ROOT)) {
            case "spawn" -> {
                spot.setSpawn(MapPoint.of(player.getLocation()));
                session.touch();
                told(player, color.getDisplayName() + " spawn", player.getLocation());
            }
            case "shop" -> {
                spot.setShop(MapPoint.of(player.getLocation()));
                session.touch();
                told(player, color.getDisplayName() + " shop", player.getLocation());
            }
            case "upgrade" -> {
                spot.setUpgrade(MapPoint.of(player.getLocation()));
                session.touch();
                told(player, color.getDisplayName() + " upgrade shop", player.getLocation());
            }
            case "generator" -> {
                Block block = targetBlock(player);
                MapPoint point = block == null
                        ? MapPoint.ofBlock(player.getLocation())
                        : MapPoint.ofBlock(block.getLocation().add(0, 1, 0));
                spot.setGenerator(point);
                session.touch();
                told(player, color.getDisplayName() + " resource generator", point);
            }
            case "bed" -> {
                Block block = targetBlock(player);
                if (block == null || !block.getType().name().endsWith("_BED")) {
                    Messages.send(player, "setup.look-at-bed");
                    return;
                }
                spot.setBed(MapPoint.ofBlock(block.getLocation()));
                session.touch();
                told(player, color.getDisplayName() + " bed", block.getX(), block.getY(), block.getZ());
            }
            case "protection" -> {
                if (args.length < 4) {
                    Messages.send(player, "setup.usage", "usage", "setup team <colour> protection <blocks>");
                    return;
                }
                Integer blocks = number(args[3]);
                if (blocks == null) {
                    Messages.send(player, "setup.not-a-number", "input", args[3]);
                    return;
                }
                spot.setProtection(blocks);
                session.touch();
                Messages.send(player, "setup.set",
                        "what", color.getDisplayName() + " protection",
                        "where", blocks + " blocks");
            }
            default -> Messages.send(player, "setup.usage", "usage",
                    "setup team <colour> spawn|bed|shop|upgrade|generator|protection <blocks>|remove");
        }
    }

    /**
     * {@code /bw setup gen add <type>} and {@code /bw setup gen remove}
     */
    private void generator(Player player, String[] args) {
        SetupSession session = requireSession(player);
        if (session == null) return;
        if (args.length < 2) {
            Messages.send(player, "setup.usage", "usage", "setup gen add <type> | setup gen remove");
            return;
        }
        ArenaMap map = session.getMap();
        if (args[1].equalsIgnoreCase("remove")) {
            GeneratorSpot removed = map.removeGeneratorNear(MapPoint.of(player.getLocation()), REMOVE_RANGE);
            if (removed == null) {
                Messages.send(player, "setup.gen-none", "range", String.valueOf((int) REMOVE_RANGE));
                return;
            }
            session.touch();
            Messages.send(player, "setup.removed", "what", removed.type() + " generator");
            return;
        }
        if (!args[1].equalsIgnoreCase("add") || args.length < 3) {
            Messages.send(player, "setup.usage", "usage", "setup gen add <type> | setup gen remove");
            return;
        }
        Block block = targetBlock(player);
        MapPoint point = block == null
                ? MapPoint.ofBlock(player.getLocation())
                : MapPoint.ofBlock(block.getLocation().add(0, 1, 0));
        map.addGenerator(new GeneratorSpot(args[2], point));
        session.touch();
        told(player, args[2].toUpperCase(Locale.ROOT) + " generator", point);
    }

    /**
     * {@code /bw setup build <maxY> [voidY]}
     */
    private void build(Player player, String[] args) {
        SetupSession session = requireSession(player);
        if (session == null) return;
        if (args.length < 2) {
            Messages.send(player, "setup.usage", "usage", "setup build <max-y> [void-y]");
            return;
        }
        Integer maxY = number(args[1]);
        if (maxY == null) {
            Messages.send(player, "setup.not-a-number", "input", args[1]);
            return;
        }
        session.getMap().setBuildMaxY(maxY);
        if (args.length >= 3) {
            Integer voidY = number(args[2]);
            if (voidY == null) {
                Messages.send(player, "setup.not-a-number", "input", args[2]);
                return;
            }
            session.getMap().setVoidY(voidY);
        }
        session.touch();
        Messages.send(player, "setup.build-set",
                "max", String.valueOf(session.getMap().getBuildMaxY()),
                "void", String.valueOf(session.getMap().getVoidY()));
    }

    /**
     * {@code /bw setup mode <mode> <colour...>} - which teams a mode plays with on this map.
     */
    private void mode(Player player, String[] args) {
        SetupSession session = requireSession(player);
        if (session == null) return;
        if (args.length < 3) {
            Messages.send(player, "setup.usage", "usage", "setup mode <mode> <colour...> | setup mode <mode> auto");
            return;
        }
        if (!Bedwars.getInstance().getModeSettings().has(args[1])) {
            Messages.send(player, "setup.unknown-mode", "input", args[1]);
            return;
        }
        if (args[2].equalsIgnoreCase("auto")) {
            session.getMap().setModeTeams(args[1], List.of());
            session.touch();
            Messages.send(player, "setup.mode-auto", "mode", args[1]);
            return;
        }
        List<TeamColor> colors = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            TeamColor color = TeamColor.byName(args[i]);
            if (color == null) {
                Messages.send(player, "setup.unknown-team", "input", args[i]);
                return;
            }
            if (!colors.contains(color)) colors.add(color);
        }
        session.getMap().setModeTeams(args[1], colors);
        session.touch();
        Messages.send(player, "setup.mode-set",
                "mode", args[1],
                "teams", String.join(", ", colors.stream().map(TeamColor::getDisplayName).toList()));
    }

    private void displayName(Player player, String[] args) {
        SetupSession session = requireSession(player);
        if (session == null) return;
        if (args.length < 2) {
            Messages.send(player, "setup.usage", "usage", "setup name <what players see>");
            return;
        }
        String name = String.join(" ", List.of(args).subList(1, args.length));
        session.getMap().setDisplayName(name);
        session.touch();
        Messages.send(player, "setup.set", "what", "display name", "where", name);
    }

    /**
     * Lists what is still missing for the mode this server would play.
     */
    private void check(CommandSender sender) {
        ArenaMap map = checkedMap();
        if (map == null) {
            Messages.send(sender, "setup.not-active");
            return;
        }
        GameMode mode = Bedwars.getInstance().getGame().getMode();
        List<String> problems = MapValidator.check(map, mode);
        if (problems.isEmpty()) {
            Messages.send(sender, "setup.check.ok", "mode", mode.getDisplayName());
            return;
        }
        Messages.send(sender, "setup.check.header",
                "count", String.valueOf(problems.size()),
                "mode", mode.getDisplayName());
        for (String problem : problems) {
            Messages.send(sender, "setup.check.entry", "problem", problem);
        }
    }

    /**
     * @return the map to check: the one being set up, or the one this server would play
     */
    private @Nullable ArenaMap checkedMap() {
        SetupSession session = session();
        return session != null ? session.getMap() : Bedwars.getInstance().getGame().getArena();
    }

    // -------------------------------------------------------------- helpers

    private @Nullable SetupSession session() {
        return Bedwars.getInstance().getSetup();
    }

    /**
     * @return the running session, telling the player when there is none
     */
    private @Nullable SetupSession requireSession(Player player) {
        SetupSession session = session();
        if (session == null) Messages.send(player, "setup.not-active");
        return session;
    }

    /**
     * @return the block the player is looking at, or {@code null} when that is thin air
     */
    private static @Nullable Block targetBlock(Player player) {
        return player.getTargetBlockExact(REACH);
    }

    private static @Nullable Integer number(String input) {
        try {
            return Integer.valueOf(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void told(Player player, String what, org.bukkit.Location where) {
        told(player, what, where.getBlockX(), where.getBlockY(), where.getBlockZ());
    }

    private static void told(Player player, String what, MapPoint where) {
        told(player, what, (int) where.x(), (int) where.y(), (int) where.z());
    }

    private static void told(Player player, String what, int x, int y, int z) {
        Messages.send(player, "setup.set", "what", what, "where", x + " " + y + " " + z);
    }
}

package de.schnorrenbergers.lobby.parkour;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
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
 * {@code /parkour} - running a course, and building one.
 * <p>
 * The building half is a command rather than a config file for the same reason the bedwars map setup is:
 * every point of a course is "where I am standing right now", and there is no way to type that into a file
 * that is not worse than walking there.
 */
public class ParkourCommand implements CommandExecutor, TabCompleter {

    private final ParkourService parkour;

    public ParkourCommand(ParkourService parkour) {
        this.parkour = parkour;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            list(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> list(sender);
            case "start", "join" -> start(sender, args);
            case "leave", "quit", "stop" -> leave(sender);
            case "top", "times" -> top(sender, args);
            case "setup" -> setup(sender, args);
            default -> usage(sender);
        }
        return true;
    }

    private void usage(CommandSender sender) {
        sender.sendMessage(Component.text("/parkour [list | start <strecke> | leave | top <strecke>]",
                NamedTextColor.GRAY));
        if (sender.isOp()) {
            sender.sendMessage(Component.text("/parkour setup <strecke> "
                    + "[start | checkpoint | undo | finish | board | name <text> | delete]", NamedTextColor.DARK_GRAY));
        }
    }

    /**
     * Lists every course with the player's own best time, each one clickable to start it.
     */
    private void list(CommandSender sender) {
        if (parkour.getStore().all().isEmpty()) {
            sender.sendMessage(Component.text("Es gibt noch keine Strecke.", NamedTextColor.GRAY));
            if (sender.isOp()) {
                sender.sendMessage(Component.text("Bau eine mit /parkour setup <name> start",
                        NamedTextColor.DARK_GRAY));
            }
            return;
        }
        sender.sendMessage(Component.text("Strecken:", NamedTextColor.AQUA));
        for (ParkourCourse course : parkour.getStore().all()) {
            Component line = Component.text("  " + course.getDisplayName(), NamedTextColor.WHITE)
                    .append(Component.text(course.isComplete()
                                    ? "  (" + course.getCheckpoints().size() + " Checkpoints)"
                                    : "  (unfertig)",
                            course.isComplete() ? NamedTextColor.GRAY : NamedTextColor.RED));
            if (sender instanceof Player player) {
                ParkourStore.Record best = parkour.getStore().best(course.getName(), player.getUniqueId());
                if (best != null) {
                    line = line.append(Component.text("  Bestzeit " + ParkourService.format(best.millis()),
                            NamedTextColor.DARK_AQUA));
                }
                line = line.clickEvent(ClickEvent.runCommand("/parkour start " + course.getName()))
                        .hoverEvent(HoverEvent.showText(Component.text("Starten")));
            }
            sender.sendMessage(line);
        }
    }

    private void start(CommandSender sender, String[] args) {
        Player player = playerOrNull(sender);
        if (player == null) return;
        ParkourCourse course = courseOrNull(sender, args, 1);
        if (course == null) return;
        parkour.start(player, course);
    }

    private void leave(CommandSender sender) {
        Player player = playerOrNull(sender);
        if (player == null) return;
        if (parkour.runOf(player) == null) {
            sender.sendMessage(Component.text("Du läufst gerade keine Strecke.", NamedTextColor.GRAY));
            return;
        }
        // to the start of the course, the same as the barrier in the hotbar does - one way of giving up,
        // one place it puts you
        parkour.quit(player);
    }

    private void top(CommandSender sender, String[] args) {
        ParkourCourse course = courseOrNull(sender, args, 1);
        if (course == null) return;
        List<ParkourStore.Record> board = parkour.leaderboard(course);
        if (board.isEmpty()) {
            sender.sendMessage(Component.text("Diese Strecke hat noch niemand geschafft.",
                    NamedTextColor.GRAY));
            return;
        }
        sender.sendMessage(Component.text("Bestzeiten - " + course.getDisplayName(), NamedTextColor.AQUA));
        for (int i = 0; i < board.size(); i++) {
            ParkourStore.Record record = board.get(i);
            sender.sendMessage(Component.text("  " + (i + 1) + ". ", NamedTextColor.DARK_AQUA)
                    .append(Component.text(record.name(), NamedTextColor.WHITE))
                    .append(Component.text("  " + ParkourService.format(record.millis()),
                            NamedTextColor.GRAY)));
        }
    }

    // -------------------------------------------------------------------- building

    /**
     * {@code /parkour setup <course> <what>} - every point is taken from where the sender is standing.
     */
    private void setup(CommandSender sender, String[] args) {
        Player player = playerOrNull(sender);
        if (player == null) return;
        if (!player.isOp()) {
            player.sendMessage(Component.text("Dafür bist du nicht berechtigt.", NamedTextColor.RED));
            return;
        }
        if (args.length < 3) {
            usage(sender);
            return;
        }
        String name = args[1];
        ParkourCourse course = parkour.getStore().getOrCreate(name);
        switch (args[2].toLowerCase(Locale.ROOT)) {
            case "start" -> {
                course.setStart(ParkourPoint.of(player.getLocation(), ParkourPoint.DEFAULT_RADIUS));
                say(player, "Start von " + course.getDisplayName() + " gesetzt: " + course.getStart());
            }
            case "checkpoint", "cp" -> {
                course.addCheckpoint(ParkourPoint.of(player.getLocation(), ParkourPoint.DEFAULT_RADIUS));
                say(player, "Checkpoint " + course.getCheckpoints().size() + " gesetzt.");
            }
            case "undo" -> {
                ParkourPoint removed = course.removeLastCheckpoint();
                say(player, removed == null
                        ? "Es gibt keinen Checkpoint mehr zum Entfernen."
                        : "Checkpoint bei " + removed + " entfernt.");
            }
            case "board", "bestenliste" -> {
                course.setBoard(ParkourPoint.of(player.getLocation(), ParkourPoint.DEFAULT_RADIUS));
                say(player, "Die Bestenliste hängt jetzt hier.");
            }
            case "finish", "ziel" -> {
                course.setFinish(ParkourPoint.of(player.getLocation(), ParkourPoint.DEFAULT_RADIUS));
                say(player, "Ziel gesetzt: " + course.getFinish());
            }
            case "name" -> {
                if (args.length < 4) {
                    say(player, "/parkour setup " + name + " name <text>");
                    return;
                }
                course.setDisplayName(String.join(" ", List.of(args).subList(3, args.length)));
                say(player, "Heißt jetzt " + course.getDisplayName() + ".");
            }
            case "delete", "remove" -> {
                parkour.getStore().remove(name);
                parkour.getStore().save();
                parkour.getHolograms().refresh();
                say(player, "Strecke " + name + " gelöscht.");
                return;
            }
            default -> {
                usage(sender);
                return;
            }
        }
        parkour.getStore().save();
        parkour.getHolograms().refresh();
        if (!course.isComplete()) {
            player.sendMessage(Component.text("Noch unfertig: es fehlt "
                    + (course.getStart() == null ? "der Start" : "das Ziel") + ".", NamedTextColor.YELLOW));
        }
    }

    // --------------------------------------------------------------------- helpers

    private static @Nullable Player playerOrNull(CommandSender sender) {
        if (sender instanceof Player player) return player;
        sender.sendMessage(Component.text("Das kann nur ein Spieler.", NamedTextColor.RED));
        return null;
    }

    private @Nullable ParkourCourse courseOrNull(CommandSender sender, String[] args, int index) {
        if (args.length <= index) {
            List<ParkourCourse> all = new ArrayList<>(parkour.getStore().all());
            // one course is the normal case, and having to name it would be pure ceremony
            if (all.size() == 1) return all.getFirst();
            sender.sendMessage(Component.text("Welche Strecke? /parkour list", NamedTextColor.GRAY));
            return null;
        }
        ParkourCourse course = parkour.getStore().get(args[index]);
        if (course == null) {
            sender.sendMessage(Component.text("Die Strecke '" + args[index] + "' gibt es nicht.",
                    NamedTextColor.RED));
        }
        return course;
    }

    private static void say(Player player, String message) {
        player.sendMessage(Component.text(message, NamedTextColor.GREEN));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length <= 1) {
            List<String> options = new ArrayList<>(List.of("list", "start", "leave", "top"));
            if (sender.isOp()) options.add("setup");
            return filter(options, args.length == 0 ? "" : args[0]);
        }
        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            for (ParkourCourse course : parkour.getStore().all()) names.add(course.getName());
            return filter(names, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setup")) {
            return filter(List.of("start", "checkpoint", "undo", "finish", "board", "name", "delete"),
                    args[2]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String typed) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}

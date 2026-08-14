package de.hems.paper.commands;

import de.hems.event.EventApi;
import de.hems.event.EventCalendar;
import de.hems.event.EventRegistry;
import de.hems.event.ScheduledEvent;
import de.hems.paper.PaperContext;
import de.hems.paper.eventcalendar.EventCalendarUi;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@code /kalender} opens the event calendar. There is a command form as well, so events can also be
 * planned from a script or a command block:
 * <pre>
 * /kalender create &lt;art&gt; &lt;name&gt; &lt;tag&gt;[,tag...]
 * /kalender heute
 * /kalender liste
 * </pre>
 */
public class EventCalendarCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Nur Spieler können den Kalender öffnen - nutze /kalender liste.");
                return true;
            }
            EventCalendarUi.open(player);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "heute" -> print(sender, EventApi.getEventsToday(), "Heute");
            case "liste" -> {
                if (sender instanceof Player player) {
                    PaperContext.async(() -> {
                        try {
                            EventCalendar.refresh();
                        } catch (Exception ignored) {
                        }
                        PaperContext.sync(() -> EventCalendarUi.printUpcoming(player));
                    });
                } else {
                    print(sender, EventApi.getEvents(), "Kalender");
                }
            }
            case "create" -> create(sender, args);
            default -> sender.sendMessage(ChatColor.RED + "/kalender [heute|liste|create]");
        }
        return true;
    }

    private void create(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Nur Admins können Events planen.");
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "/kalender create <art> <name> <2026-09-01>[,2026-09-02...]");
            return;
        }
        List<LocalDate> days = new ArrayList<>();
        for (String raw : args[3].split(",")) {
            try {
                days.add(LocalDate.parse(raw.trim()));
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "'" + raw + "' ist kein Datum (Format: 2026-09-01).");
                return;
            }
        }
        if (!EventRegistry.isKnown(args[1])) {
            sender.sendMessage(ChatColor.RED + "Unbekannte Event Art '" + args[1] + "'.");
            return;
        }
        PaperContext.async(() -> {
            try {
                EventApi.createAndSchedule(args[1], args[2], days.toArray(new LocalDate[0]));
            } catch (Exception e) {
                PaperContext.sync(() -> sender.sendMessage(ChatColor.RED + "Das hat nicht geklappt: " + e.getMessage()));
                return;
            }
            PaperContext.sync(() -> sender.sendMessage(ChatColor.GREEN + "'" + args[2] + "' steht jetzt im Kalender."));
        });
    }

    private void print(CommandSender sender, List<ScheduledEvent> events, String title) {
        if (events.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Keine Events.");
            return;
        }
        sender.sendMessage(ChatColor.AQUA + title + ":");
        for (ScheduledEvent event : events) {
            sender.sendMessage(ChatColor.GRAY + " - " + ChatColor.WHITE + event.getName() + ChatColor.DARK_GRAY
                    + " (" + event.getDefinition().getDisplayName() + ", " + event.getDays() + ")");
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 1) return filter(List.of("heute", "liste", "create"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            List<String> types = new ArrayList<>();
            EventRegistry.all().forEach((definition) -> types.add(definition.getId()));
            return filter(types, args[1]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("create")) {
            return filter(List.of(LocalDate.now().toString(), LocalDate.now().plusDays(1).toString()), args[3]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) matches.add(option);
        }
        return matches;
    }
}

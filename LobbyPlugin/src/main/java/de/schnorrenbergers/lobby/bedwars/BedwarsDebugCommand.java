package de.schnorrenbergers.lobby.bedwars;

import de.hems.api.ServerApi;
import de.hems.paper.PaperContext;
import de.hems.paper.warp.ServerStartup;
import de.hems.types.Server;
import de.hems.types.ServerTemplate;
import net.kyori.adventure.text.Component;
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
 * Startet eine Bedwars Runde von Hand und springt hin.
 * <p>
 * Später bestellt das Eventsystem die Runden - bis dahin ist das hier der Weg, überhaupt auf einen
 * Bedwars Server zu kommen. Modus und Map kann der Befehl bewusst nicht mitgeben: ein Startbefehl
 * transportiert keine Parameter, der frische Server holt sich seine Runde später selbst beim Launcher.
 * Bis dahin spielt er, was in seiner eigenen {@code game.yml} steht.
 */
public class BedwarsDebugCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String @NotNull [] args) {
        if (!sender.isOp()) {
            sender.sendMessage(Component.text("Dafür bist du nicht berechtigt.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Das kann nur ein Spieler.", NamedTextColor.RED));
                return true;
            }
            create(player);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> list(sender);
            case "stop" -> stop(sender, args);
            default -> sender.sendMessage(Component.text(
                    "/bwdebug | /bwdebug list | /bwdebug stop <name>", NamedTextColor.GRAY));
        }
        return true;
    }

    /**
     * Erstellt einen Runden-Server, wartet bis er Spieler annimmt und verbindet.
     *
     * @param player wer spielen will
     */
    private void create(Player player) {
        PaperContext.async(() -> {
            String name;
            try {
                name = ServerApi.freeName("BEDWARS");
            } catch (Exception e) {
                tell(player, "Der Server konnte nicht erstellt werden: " + e.getMessage(), NamedTextColor.RED);
                return;
            }
            // ServerStartup meldet jeden Schritt und verbindet erst, wenn der Server wirklich bereit ist
            ServerStartup.createAndWarp(player, name, ServerTemplate.BEDWARS);
        });
    }

    private void list(CommandSender sender) {
        PaperContext.async(() -> {
            List<String> running = new ArrayList<>();
            try {
                for (Server server : ServerApi.listServers()) {
                    if (server.getTemplate() == ServerTemplate.BEDWARS) {
                        running.add(server.getName() + (server.isJoinable() ? " (bereit)" : " (startet)"));
                    }
                }
            } catch (Exception e) {
                PaperContext.sync(() -> sender.sendMessage(
                        Component.text("Die Serverliste kam nicht an: " + e.getMessage(), NamedTextColor.RED)));
                return;
            }
            PaperContext.sync(() -> {
                if (running.isEmpty()) {
                    sender.sendMessage(Component.text("Gerade läuft keine Bedwars Runde.", NamedTextColor.GRAY));
                    return;
                }
                sender.sendMessage(Component.text("Bedwars Runden:", NamedTextColor.GRAY));
                for (String entry : running) {
                    sender.sendMessage(Component.text("- " + entry, NamedTextColor.WHITE));
                }
            });
        });
    }

    private void stop(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("/bwdebug stop <name>", NamedTextColor.GRAY));
            return;
        }
        String name = args[1];
        PaperContext.async(() -> {
            try {
                ServerApi.stopServer(name);
                PaperContext.sync(() -> sender.sendMessage(
                        Component.text(name + " wird gestoppt.", NamedTextColor.GRAY)));
            } catch (Exception e) {
                PaperContext.sync(() -> sender.sendMessage(
                        Component.text("Konnte " + name + " nicht stoppen: " + e.getMessage(), NamedTextColor.RED)));
            }
        });
    }

    /**
     * Sagt einem Spieler etwas aus einem Hintergrund-Thread heraus.
     */
    private static void tell(Player player, String message, NamedTextColor color) {
        PaperContext.sync(() -> {
            if (player.isOnline()) player.sendMessage(Component.text(message, color));
        });
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length <= 1) {
            String typed = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            return List.of("list", "stop").stream().filter(option -> option.startsWith(typed)).toList();
        }
        return List.of();
    }
}

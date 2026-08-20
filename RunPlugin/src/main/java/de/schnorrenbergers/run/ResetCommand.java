package de.schnorrenbergers.run;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Wipes this event server so it can host the next attempt.
 * <p>
 * A run server is cheap to make but not free, and a race needs untouched terrain - so rather than throwing
 * the server away after every attempt, the worlds are deleted and generated again with a new seed.
 */
public class ResetCommand implements CommandExecutor, TabCompleter {

    /** Typing the command twice within this window is what confirms it. */
    private static final long CONFIRM_WINDOW_MS = 15_000L;

    private long askedAt = 0L;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String @NotNull [] args) {
        if (!sender.isOp()) {
            sender.sendMessage(Component.text("Das darfst du nicht.", NamedTextColor.RED));
            return true;
        }
        long now = System.currentTimeMillis();
        if (now - askedAt > CONFIRM_WINDOW_MS) {
            askedAt = now;
            sender.sendMessage(Component.text(
                    "Das löscht die Welten dieses Servers. /reset nochmal zum Bestätigen.",
                    NamedTextColor.YELLOW));
            return true;
        }
        askedAt = 0L;
        reset(sender);
        return true;
    }

    /**
     * Unloads every world, deletes it and generates it again.
     *
     * @param sender who asked for it
     */
    private void reset(CommandSender sender) {
        World main = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().getFirst();
        if (main == null) {
            sender.sendMessage(Component.text("Dieser Server hat keine Welt.", NamedTextColor.RED));
            return;
        }
        // players have to be out of a world before it can be unloaded, and there is nowhere else to put
        // them on a server whose only world is about to go
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kick(Component.text("Der Server wird zurückgesetzt. Komm gleich wieder.",
                    NamedTextColor.YELLOW));
        }

        long seed = new Random().nextLong();
        for (World world : List.copyOf(Bukkit.getWorlds())) {
            String name = world.getName();
            File folder = world.getWorldFolder();
            // false: the point is to throw the world away, saving it first would only slow that down
            if (!Bukkit.unloadWorld(world, false)) {
                sender.sendMessage(Component.text("Konnte " + name + " nicht entladen.", NamedTextColor.RED));
                continue;
            }
            if (!delete(folder)) {
                sender.sendMessage(Component.text("Konnte " + name + " nicht löschen.", NamedTextColor.RED));
                continue;
            }
            Bukkit.createWorld(new WorldCreator(name).seed(seed));
            Bukkit.getLogger().info("Reset world " + name);
        }
        sender.sendMessage(Component.text("Der Server ist zurückgesetzt.", NamedTextColor.GREEN));
    }

    /**
     * @param folder the world folder to remove
     * @return whether it is gone
     */
    private static boolean delete(File folder) {
        if (folder == null || !folder.exists()) return true;
        try (Stream<Path> paths = Files.walk(folder.toPath())) {
            // deepest first, a directory can only go once it is empty
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    Bukkit.getLogger().warning("Could not delete " + path + ": " + e.getMessage());
                }
            });
        } catch (IOException e) {
            Bukkit.getLogger().warning("Could not walk " + folder + ": " + e.getMessage());
            return false;
        }
        return !folder.exists();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull [] args) {
        return List.of();
    }
}

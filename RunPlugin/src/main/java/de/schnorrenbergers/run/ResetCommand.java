package de.schnorrenbergers.run;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calls the run of this server off, so its team can start the next one.
 * <p>
 * A reset is what a speedrunner does with an attempt that is going nowhere. Here that is not wiping the
 * world in place - the main world of a running server cannot be unloaded, and every attempt gets a server
 * of its own anyway - but ending the attempt: the run is closed without a time, everybody goes back to
 * the lobby, and this server and its world are thrown away behind them.
 */
public class ResetCommand implements CommandExecutor, TabCompleter {

    /** Typing the command twice within this window is what confirms it. */
    private static final long CONFIRM_WINDOW_MS = 15_000L;

    private final RunTracker tracker;
    /** Who asked and when - per sender, so nobody confirms what somebody else typed. */
    private final Map<String, Long> askedAt = new HashMap<>();

    public ResetCommand(RunTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String @NotNull [] args) {
        if (!tracker.hasOpenRun()) {
            sender.sendMessage(Component.text("Auf diesem Server läuft kein Lauf, den man abbrechen könnte.",
                    NamedTextColor.RED));
            return true;
        }
        // the run belongs to its team; an admin may end it as well, a spectator may not
        if (sender instanceof Player player && !tracker.isParticipant(player.getUniqueId()) && !sender.isOp()) {
            sender.sendMessage(Component.text("Nur wer mitläuft, darf den Lauf abbrechen.", NamedTextColor.RED));
            return true;
        }
        long now = System.currentTimeMillis();
        Long asked = askedAt.get(sender.getName());
        if (asked == null || now - asked > CONFIRM_WINDOW_MS) {
            askedAt.put(sender.getName(), now);
            sender.sendMessage(Component.text("Das bricht den Lauf für das ganze Team ab, er zählt trotzdem "
                    + "als Versuch. /" + label + " nochmal zum Bestätigen.", NamedTextColor.YELLOW));
            return true;
        }
        askedAt.remove(sender.getName());
        tracker.abort(sender.getName());
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull [] args) {
        return List.of();
    }
}

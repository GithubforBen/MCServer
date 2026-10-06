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
 * Starts the run of this server over: the attempt is ended and the team gets the next one straight away.
 * <p>
 * A reset is what a speedrunner does with an attempt that is going nowhere. Here that is not wiping the
 * world in place - the main world of a running server cannot be unloaded, and every attempt gets a server
 * of its own anyway - but closing the run and opening the next one on another server. That server is the
 * ghost of the event when nobody else has claimed it yet, so the team does not wait for a world to be
 * generated. After a death or a finished run the same command starts the next attempt without asking.
 * <p>
 * {@code /abbrechen} is the way out instead: the run is called off and nobody starts a new one.
 */
public class ResetCommand implements CommandExecutor, TabCompleter {

    /** The command that only calls the run off, without starting the next one. */
    public static final String ABORT_COMMAND = "abbrechen";
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
        boolean abortOnly = ABORT_COMMAND.equalsIgnoreCase(command.getName());
        if (abortOnly ? !tracker.hasOpenRun() : !tracker.hasRun()) {
            sender.sendMessage(Component.text(abortOnly
                    ? "Auf diesem Server läuft kein Lauf, den man abbrechen könnte."
                    : "Auf diesem Server gibt es keinen Lauf, den man neu starten könnte.", NamedTextColor.RED));
            return true;
        }
        // the run belongs to its team; an admin may reset it as well, a spectator may not
        if (sender instanceof Player player && !tracker.isParticipant(player.getUniqueId()) && !sender.isOp()) {
            sender.sendMessage(Component.text("Nur wer mitläuft, darf den Lauf " + (abortOnly ? "abbrechen."
                    : "neu starten."), NamedTextColor.RED));
            return true;
        }
        // only throwing away a run that is still open needs a second thought - one that is over is lost anyway
        if (tracker.hasOpenRun()) {
            long now = System.currentTimeMillis();
            Long asked = askedAt.get(sender.getName());
            if (asked == null || now - asked > CONFIRM_WINDOW_MS) {
                askedAt.put(sender.getName(), now);
                sender.sendMessage(Component.text("Das bricht den Lauf für das ganze Team ab"
                        + (abortOnly ? "" : " und startet einen neuen") + ", er zählt trotzdem als Versuch. /"
                        + label + " nochmal zum Bestätigen.", NamedTextColor.YELLOW));
                return true;
            }
        }
        askedAt.remove(sender.getName());
        if (abortOnly) {
            tracker.abort(sender.getName());
            return true;
        }
        String answer = tracker.reset(sender.getName());
        if (answer != null) sender.sendMessage(Component.text(answer, NamedTextColor.RED));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull [] args) {
        return List.of();
    }
}

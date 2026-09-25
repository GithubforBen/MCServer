package de.schnorrenbergers.survival.featrues.adminabuse;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.adminabuse.LegitamiseAdminAbuseEvent;
import de.hems.communication.events.adminabuse.RequestToLegitimizeEvent;
import de.hems.communication.events.adminabuse.RespondToLegitimizeEvent;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.paper.PaperContext;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code /legitimize <uuid|@all> "<reason>"}: gives an admin action its reason after the fact.
 * <p>
 * Only for operators. The admin abuse log exists to hold admins to account, and a command anybody can use
 * to write "fine" next to every entry would make it worthless.
 * <p>
 * Nothing here waits on the main thread: the open entries are fetched in the background, and tab completion
 * answers from the last list it fetched - asking the launcher on every keystroke used to freeze the server
 * for as long as the launcher took to answer.
 */
public class LegitimizeCommand implements TabCompleter, CommandExecutor {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    /** The open entries as last fetched, for tab completion. */
    private volatile List<String> knownOpen = List.of();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Das dürfen nur Admins.");
            return true;
        }
        String reason = reasonOf(args);
        if (args.length < 2 || reason == null) {
            sender.sendMessage(usage());
            return true;
        }
        if (args[0].equalsIgnoreCase("@all")) {
            PaperContext.async(() -> {
                Map<UUID, String> open = fetchOpen();
                if (open == null) {
                    PaperContext.sync(() -> sender.sendMessage(ChatColor.RED + "Der Hauptserver antwortet nicht."));
                    return;
                }
                int sent = 0;
                for (UUID action : open.keySet()) {
                    if (send(action, reason)) sent++;
                }
                int count = sent;
                PaperContext.sync(() -> sender.sendMessage(ChatColor.GREEN + "✓ " + count + " Aktion"
                        + (count == 1 ? "" : "en") + " begründet."));
            });
            return true;
        }
        UUID action;
        try {
            action = UUID.fromString(args[0]);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(usage());
            return true;
        }
        PaperContext.async(() -> {
            boolean ok = send(action, reason);
            PaperContext.sync(() -> sender.sendMessage(ok
                    ? ChatColor.GREEN + "✓ Begründet."
                    : ChatColor.RED + "Konnte nicht gesendet werden."));
        });
        return true;
    }

    /**
     * @param args the words typed
     * @return the text between the first and the last quote, or {@code null} if there is none
     */
    private static String reasonOf(String[] args) {
        String joined = String.join(" ", args);
        int first = joined.indexOf('"');
        int last = joined.lastIndexOf('"');
        if (first < 0 || last <= first + 1) return null;
        String reason = joined.substring(first + 1, last).trim();
        return reason.isEmpty() ? null : reason;
    }

    /**
     * @return the open entries, or {@code null} when the launcher did not answer. Blocks.
     */
    private Map<UUID, String> fetchOpen() {
        RespondDataEvent response = ListenerAdapter.ask(
                new RequestToLegitimizeEvent(ListenerAdapter.ServerName.HOST), TIMEOUT);
        if (!(response instanceof RespondToLegitimizeEvent open)) return null;
        Map<UUID, String> entries = open.getToLegitimize();
        List<String> ids = new ArrayList<>();
        for (UUID id : entries.keySet()) ids.add(id.toString());
        knownOpen = List.copyOf(ids);
        return entries;
    }

    private static boolean send(UUID action, String reason) {
        try {
            ListenerAdapter.sendListeners(new LegitamiseAdminAbuseEvent(ListenerAdapter.ServerName.HOST, action, reason));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String usage() {
        return ChatColor.GRAY + "/legitimize <UUID|@all> \"Begründung\"";
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (!sender.isOp()) return List.of();
        if (args.length == 1) {
            // answered from the last list; the next one is fetched in the background for the next keystroke
            PaperContext.async(this::fetchOpen);
            List<String> list = new ArrayList<>();
            list.add("@all");
            list.addAll(knownOpen);
            return list;
        }
        if (args.length == 2) return List.of("\"Begründung\"");
        return List.of();
    }
}

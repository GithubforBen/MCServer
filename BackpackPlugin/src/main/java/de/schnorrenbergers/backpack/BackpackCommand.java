package de.schnorrenbergers.backpack;

import de.hems.paper.PayingPlayers;
import de.hems.paper.team.TeamService;
import de.hems.types.team.TeamData;
import de.hems.types.team.TeamSettings;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * {@code /backpack} and {@code /bp} - opens the backpack a team shares.
 */
public class BackpackCommand implements CommandExecutor, TabCompleter {

    private final BackpackManager manager;
    private final BackpackSettings settings;

    public BackpackCommand(BackpackManager manager, BackpackSettings settings) {
        this.manager = manager;
        this.settings = settings;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Diesen Befehl kann nur ein Spieler benutzen.");
            return true;
        }
        if (!TeamService.isLoaded()) {
            player.sendMessage(ChatColor.RED
                    + "❌ Die Teams sind noch nicht geladen. Bitte gleich nochmal versuchen.");
            TeamService.refreshAsync();
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("info")) {
            info(player);
            return true;
        }
        manager.open(player);
        return true;
    }

    /**
     * Explains how big the backpack is and why.
     *
     * @param player who asked
     */
    private void info(Player player) {
        TeamData team = TeamService.getTeamOf(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "❌ Du bist in keinem Team.");
            return;
        }
        int paying = PayingPlayers.countPaying(team.getMembers());
        int total = team.getMembers().size();
        boolean majority = TeamService.isMajorityPaying(team);
        player.sendMessage(ChatColor.GOLD + "Rucksack von " + team.getName());
        player.sendMessage(ChatColor.GRAY + "  Größe: " + ChatColor.WHITE
                + settings.sizeFor(majority) + " Slots"
                + (majority ? " (Doppelkiste)" : " (Kiste)"));
        player.sendMessage(ChatColor.GRAY + "  Unterstützer: " + ChatColor.WHITE + paying + " von " + total);
        player.sendMessage(ChatColor.GRAY + "  Mitglieder dürfen entnehmen: " + ChatColor.WHITE
                + (team.getSettings().getFlag(TeamSettings.Key.BACKPACK_MEMBERS_MAY_TAKE) ? "ja" : "nein"));
        player.sendMessage(ChatColor.GRAY + "  Gerade geöffnet: " + ChatColor.WHITE
                + (manager.isOpen(team.getName()) ? "ja" : "nein"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        return args.length == 1 ? List.of("info") : List.of();
    }
}

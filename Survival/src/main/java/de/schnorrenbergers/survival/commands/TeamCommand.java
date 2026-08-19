package de.schnorrenbergers.survival.commands;

import de.hems.paper.team.TeamService;
import de.hems.types.team.TeamData;
import de.hems.types.team.TeamSettings;
import de.schnorrenbergers.survival.Survival;
import de.schnorrenbergers.survival.featrues.team.ClaimManager;
import de.schnorrenbergers.survival.featrues.team.TeamManager;
import de.schnorrenbergers.survival.featrues.team.TeamManagerUi;
import de.schnorrenbergers.survival.utils.Inventorys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code /cteam} - everything about teams that is not a click in the manager.
 * <p>
 * The teams themselves live on the launcher, so every subcommand here is a request that may be refused;
 * whatever comes back is what the player is told.
 */
public class TeamCommand implements TabCompleter, CommandExecutor {

    /** Where a pending invite is remembered on the invited player. */
    private static final String INVITE_KEY = "pending-team-invite";
    /** When each player last used the team home. */
    private static final Map<UUID, Long> homeCooldown = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sendUsage(sender);
            return true;
        }
        if (!TeamService.isLoaded()) {
            player.sendMessage(ChatColor.RED
                    + "❌ Die Teams sind noch nicht vom Hauptserver geladen. Gleich nochmal versuchen.");
            TeamService.refreshAsync();
            return true;
        }
        if (args.length == 0) {
            TeamManagerUi.open(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> create(player, args);
            case "invite" -> invite(player, args);
            case "join" -> join(player, args);
            case "leave" -> leave(player, args);
            case "kick" -> kick(player, args);
            case "transfer" -> transfer(player, args);
            case "rename" -> withTeam(player, manager -> manager.rename(player, argOrNull(args, 1)));
            case "tag" -> withTeam(player, manager -> manager.setTag(player, argOrNull(args, 1)));
            case "disband" -> withTeam(player, manager -> manager.disband(player));
            case "sethome" -> withTeam(player, manager -> manager.setHome(player));
            case "home" -> home(player);
            case "settings" -> TeamManagerUi.open(player);
            case "info" -> info(player, args);
            case "list" -> list(player);
            case "claim" -> withTeam(player, manager -> manager.claimChunk(player.getChunk(), player));
            case "unclaim" -> withTeam(player, manager -> manager.unclaimChunk(player.getChunk(), player));
            case "chunks" -> chunks(player);
            case "atm" -> atm(player);
            default -> sendUsage(player);
        }
        return true;
    }

    /* ------------------------------------------------------------------ subcommands */

    private void create(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "❌ /cteam create <name> <tag>");
            return;
        }
        TeamManager.create(player, args[1], args[2]);
    }

    private void invite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "❌ /cteam invite <spieler|accept|reject>");
            return;
        }
        if (args[1].equalsIgnoreCase("accept") || args[1].equalsIgnoreCase("reject")) {
            answerInvite(player, args[1].equalsIgnoreCase("accept"));
            return;
        }
        withTeam(player, manager -> manager.invitePlayer(player, args[1]));
    }

    /**
     * Takes or turns down an invite that is waiting on the player.
     */
    private void answerInvite(Player player, boolean accept) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        NamespacedKey key = NamespacedKey.fromString(INVITE_KEY);
        if (key == null || !data.has(key)) {
            player.sendMessage(ChatColor.RED + "❌ Du hast keine offene Einladung.");
            return;
        }
        String teamName = data.get(key, PersistentDataType.STRING);
        data.remove(key);
        if (!accept) {
            player.sendMessage(ChatColor.GRAY + "Einladung abgelehnt.");
            TeamData team = TeamService.getTeam(teamName);
            if (team != null && team.getLeader() != null) {
                Player leader = Bukkit.getPlayer(team.getLeader());
                if (leader != null) {
                    leader.sendMessage(ChatColor.RED + "→ " + player.getName()
                            + " hat deine Einladung abgelehnt.");
                }
            }
            return;
        }
        TeamManager manager = TeamManager.of(teamName);
        if (!manager.exists()) {
            player.sendMessage(ChatColor.RED + "❌ Dieses Team gibt es nicht mehr.");
            return;
        }
        manager.addPlayer(player);
    }

    /**
     * Joins a team that opened itself for anybody.
     */
    private void join(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "❌ /cteam join <team>");
            return;
        }
        TeamManager manager = TeamManager.of(args[1]);
        if (!manager.exists()) {
            player.sendMessage(ChatColor.RED + "❌ Dieses Team gibt es nicht.");
            return;
        }
        if (!manager.getData().getSettings().getFlag(TeamSettings.Key.PUBLIC_JOIN)) {
            player.sendMessage(ChatColor.RED + "❌ Dieses Team nimmt nur auf Einladung auf.");
            return;
        }
        manager.addPlayer(player);
    }

    private void leave(Player player, String[] args) {
        TeamManager manager = TeamManager.of(player);
        if (manager == null) {
            player.sendMessage(ChatColor.RED + "❌ Du bist in keinem Team.");
            return;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("confirm")) {
            manager.removePlayer(player);
            return;
        }
        TextComponent question = Component.text(ChatColor.DARK_RED + "Das Team \""
                + manager.getName() + "\" wirklich verlassen?\nDu brauchst eine neue Einladung.\n");
        TextComponent confirm = Component.text(ChatColor.RED + "[ Verlassen ]")
                .clickEvent(ClickEvent.runCommand("/cteam leave confirm"));
        player.sendMessage(question.append(confirm));
    }

    private void kick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "❌ /cteam kick <spieler>");
            return;
        }
        withTeam(player, manager -> {
            UUID target = TeamManager.uuidOf(args[1]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "❌ Diesen Spieler gibt es nicht.");
                return;
            }
            OfflinePlayer offline = Bukkit.getOfflinePlayer(target);
            manager.kickPlayer(player, offline);
        });
    }

    private void transfer(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "❌ /cteam transfer <spieler>");
            return;
        }
        withTeam(player, manager -> manager.transferLeadership(player, TeamManager.uuidOf(args[1])));
    }

    /**
     * Teleports the player to the team home, after a cooldown and a moment of standing still.
     */
    private void home(Player player) {
        TeamManager manager = TeamManager.of(player);
        if (manager == null) {
            player.sendMessage(ChatColor.RED + "❌ Du bist in keinem Team.");
            return;
        }
        if (!manager.getData().getSettings().getFlag(TeamSettings.Key.HOME_ENABLED)) {
            player.sendMessage(ChatColor.RED + "❌ Das Team-Home ist für dein Team abgeschaltet.");
            return;
        }
        Location home = manager.getHome();
        if (home == null) {
            player.sendMessage(ChatColor.RED + "❌ Dein Team hat kein Home. /cteam sethome setzt eins.");
            return;
        }
        long cooldown = Survival.getInstance().getTeamRules().getHomeCooldownSeconds() * 1000L;
        Long last = homeCooldown.get(player.getUniqueId());
        if (last != null && System.currentTimeMillis() - last < cooldown) {
            long left = (cooldown - (System.currentTimeMillis() - last) + 999L) / 1000L;
            player.sendMessage(ChatColor.RED + "❌ Noch " + left + " Sekunden.");
            return;
        }
        int warmup = Survival.getInstance().getTeamRules().getHomeWarmupSeconds();
        if (warmup <= 0) {
            homeCooldown.put(player.getUniqueId(), System.currentTimeMillis());
            player.teleport(home);
            return;
        }
        Location standing = player.getLocation();
        player.sendMessage(ChatColor.GRAY + "Nicht bewegen - Teleport in " + warmup + " Sekunden.");
        Bukkit.getScheduler().runTaskLater(Survival.getInstance(), () -> {
            if (!player.isOnline()) return;
            if (player.getLocation().distanceSquared(standing) > 1.0d) {
                player.sendMessage(ChatColor.RED + "❌ Du hast dich bewegt - kein Teleport.");
                return;
            }
            homeCooldown.put(player.getUniqueId(), System.currentTimeMillis());
            player.teleport(home);
        }, warmup * 20L);
    }

    private void info(Player player, String[] args) {
        TeamData team = args.length >= 2
                ? TeamService.getTeam(args[1])
                : TeamService.getTeamOf(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "❌ Dieses Team gibt es nicht.");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "Team " + team.getName()
                + ChatColor.GRAY + " [" + team.getTag() + "]");
        player.sendMessage(ChatColor.GRAY + "  Mitglieder: " + ChatColor.WHITE + team.getMembers().size()
                + " / " + team.getSettings().getNumber(TeamSettings.Key.MAX_MEMBERS));
        player.sendMessage(ChatColor.GRAY + "  Chunks: " + ChatColor.WHITE + team.getClaims().size());
        player.sendMessage(ChatColor.GRAY + "  Beitritt: " + ChatColor.WHITE
                + (team.getSettings().getFlag(TeamSettings.Key.PUBLIC_JOIN) ? "offen" : "auf Einladung"));
        for (UUID member : team.getMembers()) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(member);
            player.sendMessage(ChatColor.DARK_GRAY + "  - " + ChatColor.WHITE
                    + (offline.getName() == null ? member.toString() : offline.getName())
                    + (team.isLeader(member) ? ChatColor.GOLD + " (Anführer)" : ""));
        }
    }

    private void list(Player player) {
        List<TeamData> teams = TeamService.getTeams();
        if (teams.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "Es gibt noch keine Teams.");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "Teams im Netzwerk (" + teams.size() + ")");
        for (TeamData team : teams) {
            player.sendMessage(ChatColor.DARK_GRAY + "  - " + ChatColor.WHITE + team.getName()
                    + ChatColor.GRAY + " (" + team.getMembers().size() + " Mitglieder"
                    + (team.getSettings().getFlag(TeamSettings.Key.PUBLIC_JOIN) ? ", offen" : "") + ")");
        }
    }

    /**
     * Draws the claims around the player as a small map.
     */
    private void chunks(Player player) {
        Chunk centre = player.getLocation().getChunk();
        for (int z = centre.getZ() - 5; z <= centre.getZ() + 5; z++) {
            TextComponent.Builder row = Component.text();
            for (int x = centre.getX() - 5; x <= centre.getX() + 5; x++) {
                Chunk chunk = player.getWorld().getChunkAt(x, z);
                String owner = ClaimManager.getTeamOfChunk(chunk);
                boolean here = x == centre.getX() && z == centre.getZ();
                if (owner == null) {
                    row.append(Component.text(here ? "[x] " : "[ ] ").color(NamedTextColor.DARK_GRAY));
                    continue;
                }
                HoverEvent<Component> hover = HoverEvent.showText(Component.text(owner));
                NamedTextColor color = ownColour(player, owner);
                row.append(Component.text(here ? "[X] " : "[▒] ").color(color).hoverEvent(hover));
            }
            player.sendMessage(row.build());
        }
    }

    /**
     * @param player who is looking
     * @param owner  the team owning the chunk
     * @return green for the player's own team, red for anybody else
     */
    private static NamedTextColor ownColour(Player player, String owner) {
        TeamData own = TeamService.getTeamOf(player.getUniqueId());
        return own != null && own.getName().equalsIgnoreCase(owner)
                ? NamedTextColor.GREEN : NamedTextColor.RED;
    }

    private void atm(Player player) {
        TeamData team = TeamService.getTeamOf(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "❌ Du bist in keinem Team.");
            return;
        }
        try {
            player.openInventory(Inventorys.ATM_INVENTORY(team.getName()).getInventory());
        } catch (MalformedURLException e) {
            player.sendMessage(ChatColor.RED + "❌ Der ATM konnte nicht geöffnet werden.");
        }
    }

    /* ------------------------------------------------------------------ helpers */

    /**
     * Runs something that needs the player to be in a team.
     *
     * @param player what to look up the team for
     * @param action what to do with it
     */
    private void withTeam(Player player, java.util.function.Consumer<TeamManager> action) {
        TeamManager manager = TeamManager.of(player);
        if (manager == null) {
            player.sendMessage(ChatColor.RED + "❌ Du bist in keinem Team.");
            return;
        }
        action.accept(manager);
    }

    private static String argOrNull(String[] args, int index) {
        return index < args.length ? args[index] : null;
    }

    public void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "/cteam" + ChatColor.GRAY + " öffnet den Team-Manager");
        sender.sendMessage(ChatColor.GRAY + "create, invite, join, leave, kick, transfer,");
        sender.sendMessage(ChatColor.GRAY + "rename, tag, disband, sethome, home, settings,");
        sender.sendMessage(ChatColor.GRAY + "info, list, claim, unclaim, chunks, atm");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length <= 1) {
            return List.of("create", "invite", "join", "leave", "kick", "transfer", "rename", "tag",
                    "disband", "sethome", "home", "settings", "info", "list", "claim", "unclaim",
                    "chunks", "atm");
        }
        String first = args[0].toLowerCase();
        if (args.length == 2) {
            switch (first) {
                case "invite" -> {
                    List<String> options = new ArrayList<>(
                            Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                    options.remove(sender.getName());
                    options.add("accept");
                    options.add("reject");
                    return options;
                }
                case "kick", "transfer" -> {
                    TeamData team = sender instanceof Player player
                            ? TeamService.getTeamOf(player.getUniqueId()) : null;
                    if (team == null) return List.of();
                    List<String> names = new ArrayList<>();
                    for (UUID member : team.getMembersWithoutLeader()) {
                        OfflinePlayer offline = Bukkit.getOfflinePlayer(member);
                        if (offline.getName() != null) names.add(offline.getName());
                    }
                    return names;
                }
                case "join", "info" -> {
                    return TeamService.getTeams().stream().map(TeamData::getName).toList();
                }
                case "create" -> {
                    return List.of("<name>");
                }
                default -> {
                    return List.of();
                }
            }
        }
        if (args.length == 3 && first.equals("create")) return List.of("[TAG]");
        return List.of();
    }
}

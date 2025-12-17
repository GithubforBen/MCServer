package de.schnorrenbergers.survival.commands;

import de.hems.api.UUIDFetcher;
import de.schnorrenbergers.survival.Survival;
import de.schnorrenbergers.survival.featrues.team.ClaimManager;
import de.schnorrenbergers.survival.featrues.team.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TeamCommand implements TabCompleter, CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return false;
        }

        switch (args[0].toLowerCase()) {
            case "create": {
                if (!(sender instanceof Player)) {
                    sendUsage(sender);
                    return false;
                }
                Player player = (Player) sender;
                String teamName = args[1];

                TeamManager teamManager = new TeamManager(teamName);
                boolean teamCreated = teamManager.createTeam(teamName, player);

                if(teamCreated) {
                    player.sendMessage(ChatColor.GREEN + String.format("✓ Du hast dein Team \"%s\" erfolgreich erstellt.", teamName));
                } else {
                    player.sendMessage(ChatColor.RED + "❌ Dein Team konnte nicht erstellt werden. Bitte prüfe, dass du noch in keinem Team bist.");
                }

                return teamCreated;
            }
            case "invite": {
                if (!(sender instanceof Player)) {
                    sendUsage(sender);
                    return false;
                }
                Player player = (Player) sender;
                Team playerTeam = player.getScoreboard().getPlayerTeam(player);
                if(playerTeam == null) {
                    player.sendMessage(ChatColor.RED + "❌ Du bist derzeit in keinem Team.");
                    return false;
                }
                TeamManager teamManager = new TeamManager(playerTeam.getName());
                System.out.println(String.format("player: %s ; team leader: %s", player.getUniqueId(), teamManager.getLeaderUUID()));
                if(!teamManager.getLeaderUUID().equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "❌ Du musst der Teamanführer sein, um andere Spieler einladen zu können.");
                    return false;
                }

                String inviteName = args[1];
                if(inviteName.equals(player.getName())) {
                    player.sendMessage(ChatColor.RED + String.format("❌ Du kannst dich nicht in dein eigenes Team einladen.", inviteName));
                    return false;
                }

                return teamManager.invitePlayer(player, inviteName);
            }
            case "chunks": {
                if (!(sender instanceof Player)) {
                    sendUsage(sender);
                    return false;
                }
                Player player = (Player) sender;
                Chunk[][] chunks = new Chunk[11][11];
                Location location = player.getLocation();
                for (int i = location.getChunk().getX() - 5; i < location.getChunk().getX() +5; i++) {
                    for (int j = location.getChunk().getZ() - 5; j < location.getChunk().getZ() +5; j++) {
                        chunks[i - location.getChunk().getX() + 5][j - location.getChunk().getZ() + 5] = location.getWorld().getChunkAt(i, j);
                    }
                }
                for (Chunk[] chunk : chunks) {
                    TextComponent.Builder component = Component.text();
                    for (Chunk value : chunk) {
                        if (value != null) {
                            String teamOfChunk = ClaimManager.getTeamOfChunk(value);
                            if (teamOfChunk == null) {
                                component.append(Component.text("["));
                                component.append(Component.text( "▒").color(NamedTextColor.WHITE));
                                component.append(Component.text("] "));
                            } else {
                                HoverEvent<Component> componentHoverEvent = HoverEvent.showText(Component.text(teamOfChunk));
                                component.append(Component.text("[").hoverEvent(componentHoverEvent));
                                component.append(Component.text( "▒").color(player.getScoreboard().getTeam(teamOfChunk).color()).hoverEvent(componentHoverEvent));
                                component.append(Component.text("] ").hoverEvent(componentHoverEvent));
                            }
                        }
                    }
                    player.sendMessage(component.build());
                }
                return true;
            }
            case "claim": {
                if (!(sender instanceof Player)) {
                    sendUsage(sender);
                    return true;
                }
                Player player = (Player) sender;
                Team playerTeam = player.getScoreboard().getPlayerTeam(player);
                if (playerTeam == null) {
                    player.sendMessage(Component.text("You dont have a team!"));
                    return true;
                }
                boolean b = new TeamManager(playerTeam.getName()).claimChunk(player.getChunk(), player);
                if (b) {
                    player.sendMessage(Component.text("You claimed this chunk!"));
                } else {
                    player.sendMessage(Component.text("You already claimed this chunk or this chunk is already claimed or you dont have enough money!"));
                }
            }
            default: {
                sendUsage(sender);
                break;
            }
        }
        return false;
    }

    public void sendUsage(CommandSender sender) {
        sender.sendMessage("Usage: /cteam <create | invite | chunks | help | claim>");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        return List.of("create", "invite", "chunks", "claim");
    }
}

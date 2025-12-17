package de.schnorrenbergers.survival.featrues.team;

import de.hems.api.UUIDFetcher;
import de.schnorrenbergers.survival.Survival;
import de.schnorrenbergers.survival.featrues.money.MoneyHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.math.BigDecimal;
import java.util.UUID;

public class TeamManager {
    private String name;
    private UUID leaderUUID;
    private Team team;
    private int playerAmount;

    private static int MAX_PLAYER_AMOUNT = 8;

    public TeamManager(String name) {
        this.name = name;

        YamlConfiguration teamConfig = Survival.getInstance().getTeamConfig().getConfig();
        if(teamConfig.contains("teams." + name + ".leaderUUID")) {
            this.leaderUUID = UUID.fromString(teamConfig.getString("teams." + name + ".leaderUUID"));
        }

        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        if(sb.getTeam(name) != null) {
            this.team = sb.getTeam(name);
        }
    }

    public boolean createTeam(String name, Player leader) {
        if(this.leaderUUID != null) return false;
        if(leader.getScoreboard().getPlayerTeam(leader) != null) return false;

        // Create minecraft team
        this.team = leader.getScoreboard().registerNewTeam(name);
        this.team.addPlayer(leader);
        this.leaderUUID = leader.getUniqueId();

        // Save leaderUUID into team config
        YamlConfiguration teamConfig = Survival.getInstance().getTeamConfig().getConfig();
        teamConfig.set("teams." + this.name + ".leaderUUID", leader.getUniqueId().toString());
        Survival.getInstance().getTeamConfig().save();

        return true;
    }

    public boolean invitePlayer(Player sender, String inviteName) {
        // Team wurde noch nicht erstellt
        if(this.leaderUUID == null) return false;
        System.out.println("team manager -> team: " + this.team);
        if(sender.getScoreboard().getTeam(this.name) == null) return false;

        if(sender.getScoreboard().getPlayerTeam(sender) == null) return false; // Wenn Sender noch in keinem Team ist
        if(!this.leaderUUID.equals(sender.getUniqueId())) return false; // Wenn Sender nicht der Teamleader ist
        if(sender.getName().equals(inviteName)) return false; // Wenn der Sender sich selbst einladen will

        UUID inviteUUID = UUIDFetcher.findUUIDByName(inviteName, true);
        System.out.printf("player %s (%s) is inviting %s (%s)%n", sender.getName(), sender.getUniqueId(), inviteName, inviteUUID);
        if(inviteUUID == null) return false;

        OfflinePlayer invitePlayer = Survival.getInstance().getServer().getOfflinePlayer(inviteUUID);
        if(!invitePlayer.isOnline() || !invitePlayer.hasPlayedBefore()) {
            sender.sendMessage(ChatColor.RED + String.format("❌ Bitte stelle sicher, dass der Spieler \"%s\" online ist.", inviteName));
            return false;
        }

        TextComponent textComponent = Component.text(ChatColor.AQUA + String.format("→ Du wurdest von \"%s\" in das Team \"%s\" eingeladen.\n", sender.getName(), this.name));
        TextComponent acceptComponent = Component.text(ChatColor.GREEN + "[ ✓ Annehmen ]").clickEvent(ClickEvent.runCommand("/cteam invite accept"));
        TextComponent rejectComponent = Component.text(ChatColor.RED + "[ ❌ Ablehnen ]").clickEvent(ClickEvent.runCommand("/cteam invite reject"));
        sender.sendMessage(textComponent.append(acceptComponent).append(rejectComponent));

        return true;
    }

    public boolean claimChunk(Chunk chunk, Player player) {
        if (MoneyHandler.getMoney(player.getUniqueId()) < getChunkCost()) {
            return false;
        }
        boolean b = ClaimManager.claimChunk(chunk, name);
        if (b) {
            if (!MoneyHandler.removeMoney(getChunkCost(), player.getUniqueId())) {
                ClaimManager.unclaimChunk(chunk);
                return false;
            }
        }
        player.sendMessage("You have claimed this chunk for " +getChunkCost() +"!");
        return b;
    }
    
    public int getChunkCost() {
        int teamChunkAmount = ClaimManager.getTeamChunkAmount(name);
        int i = 50 * high(1.1, teamChunkAmount);
        if (i > 1000) i = 1000;
        if (i < 0 ) return 1000;
        return i;
    }
    
    private int high(double btm, int top) {
        BigDecimal result = new BigDecimal(btm);
        for (int i = 0; i < top; i++) {
            result = result.multiply(new BigDecimal(btm));
        }
        return result.intValue();
    }

    public UUID getLeaderUUID() {
        return leaderUUID;
    }

    public void setLeaderUUID(UUID leaderUUID) {
        this.leaderUUID = leaderUUID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }
}

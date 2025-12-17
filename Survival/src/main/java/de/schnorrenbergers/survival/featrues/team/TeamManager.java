package de.schnorrenbergers.survival.featrues.team;

import de.hems.api.UUIDFetcher;
import de.schnorrenbergers.survival.Survival;
import de.schnorrenbergers.survival.featrues.money.MoneyHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
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

        if(this.team != null) {
            playerAmount = this.team.getPlayers().size();
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
        if(this.team == null) return false;

        if(sender.getScoreboard().getPlayerTeam(sender) == null) return false; // Wenn Sender noch in keinem Team ist
        if(!this.leaderUUID.equals(sender.getUniqueId())) return false; // Wenn Sender nicht der Teamleader ist
        if(sender.getName().equals(inviteName)) return false; // Wenn der Sender sich selbst einladen will

        if(MAX_PLAYER_AMOUNT >= playerAmount) {
            sender.sendMessage(ChatColor.RED + "❌ Dein Team hat bereits das maximum an Mitgliedern erreicht.");
            return false;
        }

        UUID inviteUUID = UUIDFetcher.findUUIDByName(inviteName, true);
        System.out.printf("player %s (%s) is inviting %s (%s)%n", sender.getName(), sender.getUniqueId(), inviteName, inviteUUID);
        if(inviteUUID == null) return false;

        OfflinePlayer invitePlayer = Survival.getInstance().getServer().getOfflinePlayer(inviteUUID);
        if(!invitePlayer.isOnline()) {
            sender.sendMessage(ChatColor.RED + String.format("❌ Bitte stelle sicher, dass der Spieler \"%s\" online ist.", inviteName));
            return false;
        }
        if(invitePlayer.getPersistentDataContainer().has(NamespacedKey.fromString("pending-team-invite"))) {
            sender.sendMessage(ChatColor.RED + String.format("❌ DerSpieler \"%s\" hat bereits eine ausstehende Einladung.", inviteName));
            return false;
        }

        TextComponent textComponent = Component.text(ChatColor.BLUE + String.format("→ Du wurdest von \"%s\" in das Team \"%s\" eingeladen.\n", sender.getName(), this.name));
        TextComponent acceptComponent = Component.text(ChatColor.GREEN + "[ ✓ Annehmen ] ").clickEvent(ClickEvent.runCommand("/cteam invite accept"));
        TextComponent rejectComponent = Component.text(ChatColor.RED + "[ ❌ Ablehnen ]").clickEvent(ClickEvent.runCommand("/cteam invite reject"));
        invitePlayer.getPlayer().sendMessage(textComponent.append(acceptComponent).append(rejectComponent));
        invitePlayer.getPlayer().getPersistentDataContainer().set(NamespacedKey.fromString("pending-team-invite"), PersistentDataType.STRING, this.name);

        sender.sendMessage(ChatColor.GREEN + String.format("✓ Du hast den Spieler \"%s\" eingeladen.", inviteName));

        return true;
    }

    public boolean addPlayer(Player player) {
        if(this.leaderUUID == null) return false;
        if(this.team == null) return false;

        if(!player.isOnline()) return false;
        if(player.getScoreboard().getPlayerTeam(player) != null) {
            player.sendMessage(ChatColor.RED + "❌ Du kannst eine Einladung nur annehmen, wenn du noch in keinem Team bist.");
            return false;
        }

        this.team.addPlayer(player);

        OfflinePlayer leader = this.getLeader();
        if(leader != null && leader.getPlayer().isOnline()) {
            leader.getPlayer().sendMessage(ChatColor.GREEN + String.format("→ Der Spieler \"%s\" hat deine Team-Einladung angenommen.", player.getName()));
        }
        player.sendMessage(ChatColor.GREEN + String.format("✓ Du hast die Einladung für das Team \"%s\" angenommen.", this.name));
        return true;
    }

    public boolean claimChunk(Chunk chunk, Player player) {
        if (MoneyHandler.getMoney(player.getUniqueId()) < getChunkCost()) {
            return false;
        }
        boolean b = ClaimManager.claimChunk(chunk, name);
        if (b) {
            if (!MoneyHandler.removeMoney(getChunkCost(), player.getUniqueId())) {
                ClaimManager.unclaimChunk(chunk, name);
                return false;
            }
        }
        player.sendMessage("You have claimed this chunk for " +getChunkCost() +"!");
        return b;
    }

    public boolean unclaimChunk(Chunk chunk, Player player) {
        boolean b = ClaimManager.unclaimChunk(chunk, name);
        if (b) {
            MoneyHandler.addMoney(getChunkCost(), player.getUniqueId());
        }
        player.sendMessage("You have unclaimed this chunk for " + getChunkCost() + "!");
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

    public OfflinePlayer getLeader() {
        if(this.leaderUUID == null) return null;
        System.out.println(String.format("getting leader for team %s: %s (%s)", this.name, UUIDFetcher.findNameByUUID(this.leaderUUID), this.leaderUUID));
        Survival.getInstance().getServer().getOfflinePlayer(leaderUUID);
        return null;
    }
}

package de.schnorrenbergers.survival.featrues.team;

import de.hems.api.UUIDFetcher;
import de.schnorrenbergers.survival.Survival;
import de.schnorrenbergers.survival.featrues.money.MoneyHandler;
import de.schnorrenbergers.survival.utils.configs.TeamConfig;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.math.BigDecimal;
import java.util.UUID;

public class TeamManager {
    private String name;
    private UUID leader;
    private Team team;

    public TeamManager(String name) {
        this.name = name;

        YamlConfiguration teamConfig = Survival.getInstance().getTeamConfig().getConfig();
        if(teamConfig.contains("teams." + name + ".leader")) {
            this.leader = UUID.fromString(teamConfig.getString("teams." + name + ".leader"));
        }

        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        if(sb.getTeam(name) != null) {
            this.team = sb.getTeam(name);
        }
    }

    public boolean createTeam(String name, Player leader) {
        if(this.leader != null) return false;
        if(leader.getScoreboard().getPlayerTeam(leader) != null) return false;

        // Create minecraft team
        this.team = leader.getScoreboard().registerNewTeam(name);
        this.team.addPlayer(leader);
        this.leader = leader.getUniqueId();

        // Save leader into team config
        YamlConfiguration teamConfig = Survival.getInstance().getTeamConfig().getConfig();
        teamConfig.set("teams." + this.name + ".leader", leader.getUniqueId().toString());
        Survival.getInstance().getTeamConfig().save();

        return true;
    }

    public boolean invitePlayer(Player player, String inviteName) {
        if(this.leader != null) return false;
        if(player.getScoreboard().getPlayerTeam(player) == null) return false;
        if(leader != player.getUniqueId()) return false;

        UUID inviteUUID = UUIDFetcher.findUUIDByName(inviteName, true);
        if(inviteUUID == null) return false;

        OfflinePlayer invitePlayer = Survival.getInstance().getServer().getOfflinePlayer(inviteUUID);
        this.team.addPlayer(invitePlayer);
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

    public UUID getLeader() {
        return leader;
    }

    public void setLeader(UUID leader) {
        this.leader = leader;
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

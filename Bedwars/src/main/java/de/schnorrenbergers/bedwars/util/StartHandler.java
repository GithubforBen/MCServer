package de.schnorrenbergers.bedwars.util;

import de.schnorrenbergers.bedwars.Bedwars;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StartHandler {

    public void start() {
        buildTeams();
    }

    /**
     * Puts every player in a team. If there are too many players online, one random player will be kicked!
     */
    private void buildTeams() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        //Only start the game if enough players are online
        if (players.size() <= 1) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "There are not enough players online! So why start???");
            return;
        }
        //randomize the list
        Collections.shuffle(players);
        //kick a random player if there are too many
        if (players.size() < Bedwars.getInstance().getConfigurationManager().getOrDefault("teamsize", 4) * 8) {
            players.getFirst().kick(Component.text("There are to many players online to start the game! \n You were kicked to make room :("));
            buildTeams();
            return;
        }
        //create all teams
        for (int i = 0; i < 8; i++) {
            Team team = players.getFirst().getScoreboard().getTeam("Team_" + i);
            if (team == null) {
                team = players.getFirst().getScoreboard().registerNewTeam("Team_" + i);
            }
            team.removeEntries(team.getEntries());
        }
        //addAllPlayersToATeam
        int i = 0;
        for (Player player : players) {
            player.getScoreboard().getTeam("Team_" + i).addPlayer(player);
            i++;
            if (i >= 8) {
                i = 0;
            }
        }
    }
}

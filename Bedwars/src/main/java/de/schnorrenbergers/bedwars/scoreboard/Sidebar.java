package de.schnorrenbergers.bedwars.scoreboard;

import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.game.timeline.Timeline;
import de.schnorrenbergers.bedwars.game.timeline.TimelineEvent;
import de.schnorrenbergers.bedwars.util.Messages;
import de.schnorrenbergers.bedwars.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;

/**
 * The board on the right, and the colours on the names.
 * <p>
 * Lines are never rewritten, only their prefixes are. A scoreboard line is an entry plus a score, and
 * changing the entry means removing and adding it - which the client shows as a flicker once a second.
 * So each line owns a fixed invisible entry and carries its text in a team prefix, which can be swapped
 * without the line ever going away.
 */
public final class Sidebar {

    /** The invisible strings that hold a line in place, one per line. */
    private static final String[] SLOTS = {
            "§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7", "§8", "§9", "§a", "§b", "§c", "§d", "§e"};

    private static final String LINE_TEAM = "line_";
    private static final String NAME_TEAM = "bw_";

    private Sidebar() {
    }

    /**
     * Gives a player their own board and puts everybody's name into the right colour on it.
     *
     * @param player who to give it to
     * @param game   the round
     */
    public static void apply(Player player, Game game) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective("bedwars", Criteria.DUMMY,
                Messages.get("sidebar.title"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        for (GameTeam team : game.getTeams()) {
            Team nameTeam = board.registerNewTeam(NAME_TEAM + team.getColor().name());
            nameTeam.color(team.getColor().getTextColor());
            nameTeam.prefix(Messages.get("sidebar.name-prefix", "initial", team.getColor().getInitial()));
        }
        player.setScoreboard(board);
        update(player, game);
    }

    /**
     * Refreshes every board. Called once a second from the running phase.
     *
     * @param game the round
     */
    public static void updateAll(Game game) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            update(player, game);
        }
    }

    /**
     * @param player whose board
     * @param game   the round
     */
    public static void update(Player player, Game game) {
        Scoreboard board = player.getScoreboard();
        Objective objective = board.getObjective("bedwars");
        if (objective == null) {
            apply(player, game);
            return;
        }
        colourNames(board, game);

        List<Component> lines = lines(player, game);
        for (int i = 0; i < SLOTS.length; i++) {
            String slot = SLOTS[i];
            Team line = board.getTeam(LINE_TEAM + i);
            if (i >= lines.size()) {
                if (line != null) board.resetScores(slot);
                continue;
            }
            if (line == null) {
                line = board.registerNewTeam(LINE_TEAM + i);
                line.addEntry(slot);
            }
            line.prefix(lines.get(i));
            // the top line gets the highest score, so the board reads downwards
            objective.getScore(slot).setScore(lines.size() - i);
        }
    }

    /**
     * @param game the round
     * @return the line that says what happens next and when, empty while nothing is left to happen
     */
    private static Component nextEvent(Game game) {
        Timeline timeline = game.getTimeline();
        // nothing in the waiting lobby: the clock has not started, so any countdown shown there would be
        // a promise about a round that has not begun
        if (timeline == null || !timeline.isStarted()) return Component.empty();
        TimelineEvent next = timeline.getNext();
        // nothing left on the clock means the last event has happened, and the last event is sudden
        // death - so the line says what the round is in rather than going blank at the loudest moment
        if (next == null) {
            return timeline.isSuddenDeath() ? Messages.get("sidebar.sudden-death") : Component.empty();
        }
        return Messages.get("sidebar.event",
                "event", Text.plain(next.displayName()),
                "time", Text.clock(timeline.getSecondsUntilNext()));
    }

    /**
     * Puts every online player into the scoreboard team of their colour, so their name is coloured in the
     * tab list and above their head.
     */
    private static void colourNames(Scoreboard board, Game game) {
        for (GameTeam team : game.getTeams()) {
            Team nameTeam = board.getTeam(NAME_TEAM + team.getColor().name());
            if (nameTeam == null) continue;
            for (GamePlayer member : team.getMembers()) {
                if (!nameTeam.hasEntry(member.getName())) nameTeam.addEntry(member.getName());
            }
        }
    }

    /**
     * @param player whose board this is
     * @param game   the round
     * @return the lines, top first
     */
    private static List<Component> lines(Player player, Game game) {
        List<Component> lines = new ArrayList<>();
        GamePlayer viewer = game.get(player);

        lines.add(Messages.get("sidebar.map", "map",
                game.getArena() == null ? "-" : game.getArena().getDisplayName()));
        lines.add(nextEvent(game));
        lines.add(Component.empty());

        for (GameTeam team : game.getTeams()) {
            String key;
            if (!team.isAlive()) {
                key = "sidebar.team.out";
            } else if (team.isBedAlive()) {
                key = "sidebar.team.bed";
            } else {
                key = "sidebar.team.alive";
            }
            Component line = Messages.get(key,
                    "initial", team.getColor().getInitial(),
                    "team", team.getColor().getDisplayName(),
                    "players", String.valueOf(team.getAliveMembers().size()));
            if (viewer != null && viewer.getTeam() == team) {
                line = line.append(Messages.get("sidebar.you"));
            }
            lines.add(line);
        }

        lines.add(Component.empty());
        if (viewer != null) {
            lines.add(Messages.get("sidebar.kills", "kills", String.valueOf(viewer.getKills())));
            lines.add(Messages.get("sidebar.beds", "beds", String.valueOf(viewer.getBedsBroken())));
        }
        return lines;
    }
}

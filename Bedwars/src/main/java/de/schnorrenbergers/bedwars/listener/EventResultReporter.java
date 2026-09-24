package de.schnorrenbergers.bedwars.listener;

import de.hems.paper.event.EventResultService;
import de.hems.types.event.EventResultData;
import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.api.BedwarsGameEndEvent;
import de.schnorrenbergers.bedwars.api.BedwarsPlayerKillEvent;
import de.schnorrenbergers.bedwars.api.BedwarsTeamEliminatedEvent;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.game.Standings;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tells the launcher how the round of an event went, so the event's rewards can be paid out.
 * <p>
 * Placings are by team, in the order the teams went out: the first team out takes the worst place, the last
 * one standing is first, and everybody in a team shares its place - a team of four that wins takes home
 * four first prizes. Kills are counted per player, final kills included.
 * <p>
 * A round that runs out of time without one team left ranks the teams still standing by the same table the
 * time limit decides the winner by ({@link Standings}), so what is paid out agrees with what the end screen
 * showed. A round an operator stopped, or that emptied out, leaves the teams still standing without a place
 * - they get what is for taking part and for kills, but nothing for a placing nobody played out.
 * <p>
 * Every change is sent straight away: the round server is switched off long before the event is settled,
 * and a crash in the last minute should not cost the placings of the first half.
 * <p>
 * A round nobody ordered - a private round, a test on a server started by hand - reports nothing.
 */
public final class EventResultReporter implements Listener {

    private final Bedwars plugin;
    /** Each team's placing, once it has one. */
    private final Map<GameTeam, Integer> places = new HashMap<>();
    /** Everybody's line, kept so a change only has to touch one field. */
    private final Map<UUID, EventResultData> lines = new HashMap<>();

    public EventResultReporter(Bedwars plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private boolean reporting() {
        return plugin.getEventId() != null;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(BedwarsPlayerKillEvent event) {
        if (!reporting() || event.getKiller() == null) return;
        report(List.of(line(event.getKiller())));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEliminated(BedwarsTeamEliminatedEvent event) {
        if (!reporting()) return;
        GameTeam team = event.getTeam();
        if (places.containsKey(team)) return;
        // the team that goes out now takes the worst place that is still free
        places.put(team, standing(event.getGame()) + 1);
        report(linesOf(team));
    }

    /**
     * At MONITOR, so the round is final by the time it is written down.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEnd(BedwarsGameEndEvent event) {
        if (!reporting()) return;
        Game game = event.getGame();
        int next = 1;
        if (event.getWinner() != null) {
            places.put(event.getWinner(), 1);
            next = 2;
        }
        if (event.getReason() == BedwarsGameEndEvent.Reason.TIME_LIMIT) {
            List<Standings.TeamScore> table = Standings.rankTeams(game,
                    plugin.getTimelineSettings().getWeights());
            for (Standings.TeamScore score : table) {
                if (places.containsKey(score.team())) continue;
                places.put(score.team(), next++);
            }
        }
        List<EventResultData> all = new ArrayList<>();
        for (GameTeam team : game.getTeams()) {
            if (team.isEmpty()) continue;
            all.addAll(linesOf(team));
        }
        report(all);
    }

    /**
     * @param game the round
     * @return how many teams with players are still in it
     */
    private static int standing(Game game) {
        int count = 0;
        for (GameTeam team : game.getAliveTeams()) {
            if (!team.isEmpty()) count++;
        }
        return count;
    }

    private List<EventResultData> linesOf(GameTeam team) {
        List<EventResultData> result = new ArrayList<>();
        for (GamePlayer member : team.getMembers()) result.add(line(member));
        return result;
    }

    /**
     * @param player who
     * @return their line as it stands now: their team's place, if it has one, and their kills
     */
    private EventResultData line(GamePlayer player) {
        EventResultData line = lines.computeIfAbsent(player.getUuid(),
                id -> new EventResultData(plugin.getEventId(), id, player.getName()));
        GameTeam team = player.getTeam();
        Integer place = team == null ? null : places.get(team);
        if (place != null) line.setPlace(place);
        line.setKills(player.getKills());
        return line;
    }

    private static void report(List<EventResultData> rows) {
        EventResultService.report(rows);
    }
}

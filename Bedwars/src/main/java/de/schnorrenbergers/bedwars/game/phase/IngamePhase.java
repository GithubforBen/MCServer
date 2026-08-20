package de.schnorrenbergers.bedwars.game.phase;

import de.schnorrenbergers.bedwars.api.BedwarsGameEndEvent;
import de.schnorrenbergers.bedwars.game.Equipment;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.game.TeamBalancer;
import de.schnorrenbergers.bedwars.api.BedwarsPlayerRespawnEvent;
import de.schnorrenbergers.bedwars.generator.GeneratorManager;
import de.schnorrenbergers.bedwars.map.MapPoint;
import de.schnorrenbergers.bedwars.scoreboard.Sidebar;
import de.schnorrenbergers.bedwars.util.Messages;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;

/**
 * The round itself.
 * <p>
 * What this phase owns is the question "is it over yet", asked once a second: a team whose bed and players
 * are both gone is out, and a round with one team left is decided. Beds, generators, the shop and the
 * timeline hang themselves off the same tick as they arrive.
 */
public class IngamePhase extends GamePhase {

    /** How long an empty server keeps playing before the round is called off. */
    private static final int EMPTY_SECONDS = 30;

    private int emptySeconds;

    public IngamePhase(Game game) {
        super(game);
    }

    @Override
    public PhaseType getType() {
        return PhaseType.RUNNING;
    }

    @Override
    public void onEnter() {
        TeamBalancer.balance(game);
        for (GameTeam team : game.getTeams()) {
            if (!team.isEmpty()) continue;
            // a team nobody joined never plays: it has no bed to defend and nothing left to eliminate
            team.setBedAlive(false);
            team.setEliminated(true);
        }
        for (GamePlayer participant : game.getPlayers()) {
            Player player = participant.getPlayer();
            if (player == null) continue;
            if (participant.getTeam() == null) {
                watch(participant, player);
            } else {
                start(participant, player);
            }
        }
        GeneratorManager generators = game.getGenerators();
        if (generators != null) generators.build(game);
        Messages.broadcast("game.started", "mode", game.getMode().getDisplayName());
    }

    @Override
    public void onExit() {
        GeneratorManager generators = game.getGenerators();
        if (generators != null) generators.remove();
    }

    /**
     * Counts the dead down and brings them back when their time is up.
     * <p>
     * Every tick rather than every second, because the countdown a dead player stares at should not jump.
     */
    private void tickRespawns() {
        for (GamePlayer participant : game.getPlayers()) {
            if (participant.getState() != GamePlayer.State.RESPAWNING) continue;
            int left = participant.getRespawnTicks() - 1;
            participant.setRespawnTicks(left);
            Player player = participant.getPlayer();
            if (player == null) continue;
            if (left <= 0) {
                respawn(participant, player);
                continue;
            }
            if (left % 20 != 0) continue;
            player.showTitle(Title.title(
                    Messages.get("respawn.title", "seconds", String.valueOf(left / 20)),
                    Messages.get("respawn.subtitle"),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ZERO)));
        }
    }

    /**
     * Brings one player back at their base.
     */
    private void respawn(GamePlayer participant, Player player) {
        GameTeam team = participant.getTeam();
        if (team == null || !team.isAlive()) {
            participant.setState(GamePlayer.State.SPECTATOR);
            player.setGameMode(GameMode.SPECTATOR);
            return;
        }
        Location spawn = spawnOf(team);
        BedwarsPlayerRespawnEvent event = new BedwarsPlayerRespawnEvent(game, participant,
                spawn == null ? player.getLocation() : spawn);
        Bukkit.getPluginManager().callEvent(event);

        participant.setState(GamePlayer.State.ALIVE);
        player.setGameMode(GameMode.SURVIVAL);
        Equipment.reset(player, event.getLocation());
        Equipment.giveStartingKit(player, team);
        protect(player);
        player.showTitle(Title.title(Messages.get("respawn.back"), net.kyori.adventure.text.Component.empty(),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(800), Duration.ofMillis(200))));
    }

    /**
     * Makes somebody who just appeared briefly untouchable, so a player camping the spawn cannot kill them
     * before they can move.
     */
    private void protect(Player player) {
        int seconds = game.getSettings().getRespawnProtectionSeconds();
        if (seconds <= 0) return;
        player.setInvulnerable(true);
        Bukkit.getScheduler().runTaskLater(de.schnorrenbergers.bedwars.Bedwars.getInstance(),
                () -> player.setInvulnerable(false), seconds * 20L);
    }

    /**
     * Puts a player into the round: at their base, in their colours, with what everybody starts with.
     */
    private void start(GamePlayer participant, Player player) {
        GameTeam team = participant.getTeam();
        participant.setState(GamePlayer.State.ALIVE);
        player.setGameMode(GameMode.SURVIVAL);
        Equipment.reset(player, spawnOf(team));
        Equipment.giveStartingKit(player, team);
        Messages.send(player, "game.your-team", "team", team.getColor().getDisplayName());
    }

    /**
     * Anybody who is here without a team - staff, somebody who arrived a second too late - watches.
     */
    private void watch(GamePlayer participant, Player player) {
        participant.setState(GamePlayer.State.SPECTATOR);
        player.setGameMode(GameMode.SPECTATOR);
    }

    /**
     * @param team the team
     * @return where its players start, falling back to the lobby when the map is short of a spawn
     */
    private @Nullable Location spawnOf(GameTeam team) {
        if (game.getArena() == null || game.getWorld() == null) return null;
        var spot = game.getArena().getTeam(team.getColor());
        MapPoint spawn = spot == null ? null : spot.getSpawn();
        if (spawn == null) spawn = game.getArena().getLobby();
        return spawn == null ? game.getWorld().getSpawnLocation() : spawn.toLocation(game.getWorld());
    }

    @Override
    public void tick(long ticks) {
        GeneratorManager generators = game.getGenerators();
        if (generators != null) generators.tick(game, ticks);
        tickRespawns();

        if (ticks % 20L != 0L) return;
        Sidebar.updateAll(game);

        for (GameTeam team : game.eliminateFinishedTeams()) {
            Messages.broadcast("team.eliminated", "team", team.getColor().getDisplayName());
        }

        if (game.getOnlineCount() == 0) {
            // not immediately: a server that everybody leaves for a moment during a restart is not over
            if (++emptySeconds >= EMPTY_SECONDS) {
                game.end(null, BedwarsGameEndEvent.Reason.EMPTY);
            }
            return;
        }
        emptySeconds = 0;

        List<GameTeam> alive = game.getAliveTeams();
        if (alive.size() == 1) {
            game.end(alive.getFirst(), BedwarsGameEndEvent.Reason.LAST_TEAM);
        } else if (alive.isEmpty()) {
            game.end(null, BedwarsGameEndEvent.Reason.LAST_TEAM);
        }
    }
}

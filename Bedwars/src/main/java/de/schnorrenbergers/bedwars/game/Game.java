package de.schnorrenbergers.bedwars.game;

import de.schnorrenbergers.bedwars.api.BedwarsGameEndEvent;
import de.schnorrenbergers.bedwars.api.BedwarsGameStateChangeEvent;
import de.schnorrenbergers.bedwars.api.BedwarsTeamEliminatedEvent;
import de.schnorrenbergers.bedwars.config.GameSettings;
import de.schnorrenbergers.bedwars.game.phase.EndPhase;
import de.schnorrenbergers.bedwars.game.phase.GamePhase;
import de.schnorrenbergers.bedwars.game.phase.LobbyPhase;
import de.schnorrenbergers.bedwars.game.phase.PhaseType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The one round this server hosts.
 * <p>
 * There is deliberately no arena register and no lookup from player to game: a bedwars server is created
 * for a single round and thrown away afterwards, so a second round would be a second server. Everything
 * that would otherwise need that bookkeeping - which arena a block belongs to, which game a player is in -
 * simply does not exist here.
 */
public class Game {

    private final GameMode mode;
    private final GameSettings settings;
    private final Map<TeamColor, GameTeam> teams = new LinkedHashMap<>();
    private final Map<UUID, GamePlayer> players = new LinkedHashMap<>();

    private GamePhase phase;
    private GameLoop loop;
    private GameTeam winner;
    private boolean ended;

    public Game(GameMode mode, GameSettings settings) {
        this.mode = mode;
        this.settings = settings;
        for (TeamColor color : mode.getColors()) {
            teams.put(color, new GameTeam(color));
        }
    }

    /**
     * Starts the clock and puts the round into its waiting lobby.
     *
     * @param plugin the plugin the loop belongs to
     */
    public void start(Plugin plugin) {
        if (loop != null) return;
        loop = new GameLoop(plugin, this);
        loop.start();
        setPhase(new LobbyPhase(this));
    }

    /**
     * Stops the clock and leaves the current phase, for a plugin that is being disabled.
     */
    public void shutdown() {
        if (loop != null) {
            loop.stop();
            loop = null;
        }
        if (phase != null) phase.onExit();
    }

    // -------------------------------------------------------------- phases

    /**
     * Moves the round on, telling the old phase it is over and the new one that it has begun.
     *
     * @param next where the round goes
     */
    public void setPhase(GamePhase next) {
        PhaseType from = phase == null ? null : phase.getType();
        if (phase != null) phase.onExit();
        phase = next;
        phase.onEnter();
        Bukkit.getPluginManager().callEvent(new BedwarsGameStateChangeEvent(this, from, phase.getType()));
    }

    /**
     * @param ticks how long the round has been running
     */
    public void tickPhase(long ticks) {
        if (phase != null) phase.tick(ticks);
    }

    public @Nullable GamePhase getPhase() {
        return phase;
    }

    /**
     * @return which state the round is in, {@link PhaseType#LOBBY} before it has even started
     */
    public PhaseType getPhaseType() {
        return phase == null ? PhaseType.LOBBY : phase.getType();
    }

    public boolean isRunning() {
        return getPhaseType() == PhaseType.RUNNING;
    }

    public boolean isWaiting() {
        return getPhaseType() == PhaseType.LOBBY;
    }

    // ------------------------------------------------------------- players

    /**
     * Remembers a player as part of this round, or hands back the record they already had.
     *
     * @param player who joined the server
     * @return their record
     */
    public GamePlayer join(Player player) {
        return players.computeIfAbsent(player.getUniqueId(),
                id -> new GamePlayer(id, player.getName()));
    }

    /**
     * @param player who to look up
     * @return their record, or {@code null} when they are not part of this round
     */
    public @Nullable GamePlayer get(Player player) {
        return player == null ? null : players.get(player.getUniqueId());
    }

    public @Nullable GamePlayer get(UUID uuid) {
        return players.get(uuid);
    }

    /**
     * Forgets a player entirely. Only used while the round has not started - once it has, a player who
     * logs off keeps their place and their team.
     *
     * @param uuid who to forget
     */
    public void forget(UUID uuid) {
        GamePlayer player = players.remove(uuid);
        if (player != null && player.getTeam() != null) player.getTeam().remove(player);
    }

    public Collection<GamePlayer> getPlayers() {
        return List.copyOf(players.values());
    }

    /**
     * @return everybody who is still part of the round and online
     */
    public List<GamePlayer> getOnlinePlayers() {
        return players.values().stream().filter(GamePlayer::isOnline).toList();
    }

    /**
     * @return how many players are on this server right now
     */
    public int getOnlineCount() {
        return Bukkit.getOnlinePlayers().size();
    }

    // --------------------------------------------------------------- teams

    public Collection<GameTeam> getTeams() {
        return List.copyOf(teams.values());
    }

    public @Nullable GameTeam getTeam(TeamColor color) {
        return teams.get(color);
    }

    /**
     * @return the teams that can still win
     */
    public List<GameTeam> getAliveTeams() {
        return teams.values().stream().filter(GameTeam::isAlive).toList();
    }

    /**
     * Marks every team that has lost its bed and its last player as out, announcing each one.
     *
     * @return the teams that were taken out by this check
     */
    public List<GameTeam> eliminateFinishedTeams() {
        List<GameTeam> gone = new ArrayList<>();
        for (GameTeam team : teams.values()) {
            if (team.isEliminated() || !team.shouldBeEliminated()) continue;
            team.setEliminated(true);
            gone.add(team);
            Bukkit.getPluginManager().callEvent(new BedwarsTeamEliminatedEvent(this, team));
        }
        return gone;
    }

    // ----------------------------------------------------------------- end

    /**
     * Ends the round. Does nothing the second time, so a win that is worked out in two places at once is
     * still only announced once.
     *
     * @param winner who won, or {@code null} when nobody did
     * @param reason why it is over
     */
    public void end(@Nullable GameTeam winner, BedwarsGameEndEvent.Reason reason) {
        if (ended) return;
        ended = true;
        this.winner = winner;
        Bukkit.getPluginManager().callEvent(new BedwarsGameEndEvent(this, winner, reason));
        setPhase(new EndPhase(this));
    }

    public boolean isEnded() {
        return ended;
    }

    public @Nullable GameTeam getWinner() {
        return winner;
    }

    // ------------------------------------------------------------ settings

    public GameMode getMode() {
        return mode;
    }

    public GameSettings getSettings() {
        return settings;
    }

    /**
     * @return how many players are let onto this server
     */
    public int getMaximumPlayers() {
        return settings.getMaximumPlayers(mode.getMaximumPlayers());
    }

    public @Nullable GameLoop getLoop() {
        return loop;
    }
}

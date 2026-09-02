package de.schnorrenbergers.bedwars.game.phase;

import de.hems.api.ServerApi;
import de.hems.communication.ListenerAdapter;
import de.hems.paper.PaperContext;
import de.hems.paper.cosmetic.WinContext;
import de.hems.paper.cosmetic.WinEffects;
import de.hems.paper.warp.ServerConnector;
import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.game.Standings;
import de.schnorrenbergers.bedwars.map.ArenaMap;
import de.schnorrenbergers.bedwars.map.TeamSpot;
import de.schnorrenbergers.bedwars.util.Messages;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * It is decided.
 * <p>
 * The winners get a moment, then everybody goes back to the lobby and this server asks the launcher to
 * stop it. That is what makes the map reset free: the world is thrown away with the server, so nothing has
 * to be rolled back and no round can inherit the last one's craters.
 * <p>
 * Nobody can be hurt any more from here on. A round that is over but still lets a sword through leaves
 * whoever won it dying on the winner screen, and there is nothing left for that death to mean.
 */
public class EndPhase extends GamePhase {

    /** How many players the closing board names. */
    private static final int TOP = 3;

    /** How wide an effect reaches when the map does not say. */
    private static final double DEFAULT_RADIUS = 48.0d;
    private static final double MIN_RADIUS = 24.0d;
    private static final double MAX_RADIUS = 160.0d;

    private int remaining;
    private boolean sentHome;
    /** Whether a cosmetic took over the celebration, in which case the plain fireworks stay away. */
    private boolean cosmeticPlayed;

    public EndPhase(Game game) {
        super(game);
        this.remaining = game.getSettings().getEndReturnSeconds();
    }

    @Override
    public PhaseType getType() {
        return PhaseType.ENDING;
    }

    @Override
    public void onEnter() {
        GameTeam winner = game.getWinner();
        if (winner == null) {
            Messages.broadcast("game.ended.nobody");
        } else {
            Messages.broadcast("game.ended.winner", "team", winner.getColor().getDisplayName());
        }
        showTitle(winner);
        showBoard();
        calm();
        cosmeticPlayed = playWinEffects(winner);
    }

    /**
     * Plays what the winners are wearing.
     * <p>
     * One effect per distinct choice, over the whole map rather than over one player: the effect belongs to
     * the round that was just won, and everybody who lost it is standing there watching.
     *
     * @param winner the winning team, may be {@code null}
     * @return whether anything was played, so the plain fireworks know to stay out of the way
     */
    private boolean playWinEffects(GameTeam winner) {
        if (winner == null || game.getWorld() == null) return false;
        List<Player> present = new ArrayList<>();
        List<UUID> ids = new ArrayList<>();
        for (GamePlayer member : winner.getMembers()) {
            Player player = member.getPlayer();
            if (player == null) continue;
            present.add(player);
            ids.add(player.getUniqueId());
        }
        if (present.isEmpty()) return false;
        Location middle = game.getMiddle();
        if (middle == null) middle = game.getWorld().getSpawnLocation();
        ArenaMap arena = game.getArena();
        int topY = arena == null ? middle.getBlockY() + 30 : arena.getBuildMaxY();
        WinContext context = new WinContext(Bedwars.getInstance(), present, middle, topY,
                mapRadius(middle), null);
        return WinEffects.playForAll(ids, context) > 0;
    }

    /**
     * How far the map reaches, measured rather than configured: the furthest team spawn from the middle,
     * with a little on top so an effect covers the bases as well.
     *
     * @param middle the middle of the map
     * @return the radius in blocks
     */
    private double mapRadius(Location middle) {
        ArenaMap arena = game.getArena();
        if (arena == null || game.getWorld() == null) return DEFAULT_RADIUS;
        double furthest = 0.0d;
        for (TeamSpot spot : arena.getTeams().values()) {
            if (spot.getSpawn() == null) continue;
            Location at = spot.getSpawn().toLocation(game.getWorld());
            double dx = at.getX() - middle.getX();
            double dz = at.getZ() - middle.getZ();
            furthest = Math.max(furthest, Math.sqrt(dx * dx + dz * dz));
        }
        if (furthest <= 0.0d) return DEFAULT_RADIUS;
        return Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, furthest + 16.0d));
    }

    /**
     * Puts the result on everybody's screen, telling the winners apart from everybody else - the same line
     * for both would make winning read like being told the score.
     */
    private void showTitle(GameTeam winner) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            GamePlayer participant = game.get(player);
            boolean won = winner != null && participant != null && winner.equals(participant.getTeam());
            player.showTitle(Title.title(
                    Messages.get(won ? "end.title.won" : winner == null
                            ? "end.title.nobody" : "end.title.lost"),
                    Messages.get(winner == null ? "end.subtitle-nobody" : "end.subtitle",
                            "team", winner == null ? "" : winner.getColor().getDisplayName()),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(4), Duration.ofSeconds(1))));
        }
    }

    /**
     * Names the three players who did most of the work.
     */
    private void showBoard() {
        List<Standings.PlayerScore> ranking = Standings.rankPlayers(game,
                Bedwars.getInstance().getTimelineSettings().getWeights());
        // a round in which nobody did anything gets no board: three names next to three zeroes says less
        // than the empty space it takes up
        if (ranking.isEmpty() || ranking.getFirst().points() <= 0) return;
        Messages.broadcast("end.top.header");
        for (int place = 0; place < Math.min(TOP, ranking.size()); place++) {
            Standings.PlayerScore score = ranking.get(place);
            if (score.points() <= 0) break;
            Messages.broadcast("end.top.entry",
                    "place", String.valueOf(place + 1),
                    "player", score.player().getName(),
                    "team", score.player().getTeam() == null
                            ? "" : score.player().getTeam().getColor().getDisplayName(),
                    "kills", String.valueOf(score.kills()),
                    "finals", String.valueOf(score.finals()),
                    "beds", String.valueOf(score.beds()));
        }
    }

    /**
     * Takes the fight out of the round: no damage, no hunger, and nothing on fire.
     */
    private void calm() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setInvulnerable(true);
            player.setFireTicks(0);
            player.setFoodLevel(20);
            player.setSaturation(20f);
        }
    }

    @Override
    public void tick(long ticks) {
        if (sentHome || ticks % 20L != 0L) return;
        if (!cosmeticPlayed) celebrate();
        if (remaining-- > 0) return;
        sentHome = true;
        sendEverybodyHome();
        stopThisServer();
    }

    /**
     * Sets a firework off over each of the winners, in their own colour.
     * <p>
     * What a round ends with when the cosmetics are not there to end it - no network, or a winner who has
     * nothing on. A round that ends in silence looks broken.
     */
    private void celebrate() {
        GameTeam winner = game.getWinner();
        if (winner == null) return;
        Color colour = winner.getColor().getArmorColor();
        for (GamePlayer member : winner.getMembers()) {
            Player player = member.getPlayer();
            if (player == null) continue;
            Location at = player.getLocation().add(0.0d, 1.0d, 0.0d);
            Firework firework = player.getWorld().spawn(at, Firework.class);
            FireworkMeta meta = firework.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder()
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .withColor(colour)
                    .withFade(Color.WHITE)
                    .withFlicker()
                    .build());
            meta.setPower(1);
            firework.setFireworkMeta(meta);
        }
    }

    /**
     * Puts everybody back onto the lobby. A player the proxy cannot move stays where they are - the server
     * stops underneath them anyway, and the proxy catches them.
     */
    private void sendEverybodyHome() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                ServerConnector.connect(player, ListenerAdapter.ServerName.LOBBY);
            } catch (Exception e) {
                Bukkit.getLogger().warning("[Bedwars] Could not send " + player.getName()
                        + " back to the lobby: " + e.getMessage());
            }
        }
    }

    /**
     * Asks the launcher to stop this server, off the main thread because it waits for an answer.
     */
    private void stopThisServer() {
        if (!game.getSettings().isStopServerWhenDone()) return;
        String self = ListenerAdapter.getName().toString();
        PaperContext.async(() -> {
            try {
                ServerApi.stopServer(self);
            } catch (Exception e) {
                Bukkit.getLogger().warning("[Bedwars] Could not stop " + self + ": " + e.getMessage());
            }
        });
    }

    /**
     * @return how many seconds are left before everybody is sent back
     */
    public int getRemaining() {
        return remaining;
    }
}

package de.schnorrenbergers.bedwars.game.phase;

import de.hems.api.ServerApi;
import de.hems.communication.ListenerAdapter;
import de.hems.paper.PaperContext;
import de.hems.paper.warp.ServerConnector;
import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.game.Standings;
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
import java.util.List;

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

    private int remaining;
    private boolean sentHome;

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
        celebrate();
        if (remaining-- > 0) return;
        sentHome = true;
        sendEverybodyHome();
        stopThisServer();
    }

    /**
     * Sets a firework off over each of the winners, in their own colour.
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

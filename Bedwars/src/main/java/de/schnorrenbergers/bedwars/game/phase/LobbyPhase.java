package de.schnorrenbergers.bedwars.game.phase;

import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.scoreboard.Sidebar;
import de.schnorrenbergers.bedwars.util.Messages;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;

import java.util.Set;

/**
 * Waiting for enough players.
 * <p>
 * The countdown only runs while the lobby is full enough and starts over the moment it is not, so a player
 * leaving at second three does not drop everybody into a round that cannot be played. A full lobby cuts the
 * wait short - there is nothing left to wait for.
 */
public class LobbyPhase extends GamePhase {

    /** The seconds that are announced. Everything in between passes quietly. */
    private static final Set<Integer> ANNOUNCED = Set.of(60, 30, 20, 15, 10, 5, 4, 3, 2, 1);

    private static final Sound TICK = Sound.sound(Key.key("block.note_block.hat"), Sound.Source.MASTER, 1f, 1f);
    private static final Sound GO = Sound.sound(Key.key("block.note_block.pling"), Sound.Source.MASTER, 1f, 1.4f);

    private int remaining;
    private boolean counting;
    private boolean shortened;

    public LobbyPhase(Game game) {
        super(game);
        this.remaining = game.getSettings().getLobbyCountdownSeconds();
    }

    @Override
    public PhaseType getType() {
        return PhaseType.LOBBY;
    }

    @Override
    public void tick(long ticks) {
        if (ticks % 20L != 0L) return;
        Sidebar.updateAll(game);

        // no map, or somebody is building one: there is nothing to count down to
        if (!game.canStart()) {
            counting = false;
            remaining = game.getSettings().getLobbyCountdownSeconds();
            return;
        }

        int online = game.getOnlineCount();
        if (online < game.getSettings().getMinimumPlayers()) {
            if (counting) {
                counting = false;
                shortened = false;
                remaining = game.getSettings().getLobbyCountdownSeconds();
                Messages.broadcast("lobby.cancelled",
                        "needed", String.valueOf(game.getSettings().getMinimumPlayers()));
            }
            return;
        }

        if (!counting) {
            counting = true;
            Messages.broadcast("lobby.counting", "seconds", String.valueOf(remaining));
        }

        // a full lobby has nothing left to wait for
        if (!shortened && online >= game.getMaximumPlayers()) {
            shortened = true;
            int shortCountdown = game.getSettings().getFullLobbyCountdownSeconds();
            if (shortCountdown < remaining) {
                remaining = shortCountdown;
                Messages.broadcast("lobby.full", "seconds", String.valueOf(remaining));
            }
        }

        remaining--;
        if (remaining > 0) {
            if (ANNOUNCED.contains(remaining)) {
                Messages.broadcast("lobby.countdown", "seconds", String.valueOf(remaining));
                Bukkit.getServer().playSound(TICK);
            }
            return;
        }

        Bukkit.getServer().playSound(GO);
        // phase 2 fills the teams here before the round begins
        game.setPhase(new IngamePhase(game));
    }

    /**
     * @return how many seconds are left, or the full countdown while it is not running
     */
    public int getRemaining() {
        return remaining;
    }

    public boolean isCounting() {
        return counting;
    }
}

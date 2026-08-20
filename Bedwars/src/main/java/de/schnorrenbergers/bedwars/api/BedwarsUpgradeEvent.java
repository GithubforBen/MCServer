package de.schnorrenbergers.bedwars.api;

import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * A team upgrade is being bought. Fired before it is paid for and applied.
 */
public class BedwarsUpgradeEvent extends BedwarsEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final GameTeam team;
    private final GamePlayer buyer;
    private final String upgradeId;
    private final int level;
    private boolean cancelled;

    /**
     * @param game      the round
     * @param team      whose upgrade it is
     * @param buyer     who paid
     * @param upgradeId the entry from the upgrade config
     * @param level     the level the team reaches with it
     */
    public BedwarsUpgradeEvent(Game game, GameTeam team, GamePlayer buyer, String upgradeId, int level) {
        super(game);
        this.team = team;
        this.buyer = buyer;
        this.upgradeId = upgradeId;
        this.level = level;
    }

    public GameTeam getTeam() {
        return team;
    }

    public GamePlayer getBuyer() {
        return buyer;
    }

    public String getUpgradeId() {
        return upgradeId;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

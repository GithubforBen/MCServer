package de.schnorrenbergers.bedwars.api;

import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Somebody is buying something at a shop. Fired before anything is taken or handed over, so cancelling it
 * costs the player nothing.
 * <p>
 * The seller is part of it because an addon may care whose shop this is - the bed token can only be bought
 * at a shop that does not belong to the buyer.
 */
public class BedwarsPurchaseEvent extends BedwarsEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final GamePlayer buyer;
    private final String itemId;
    private final GameTeam seller;
    private boolean cancelled;

    /**
     * @param game   the round
     * @param buyer  who is buying
     * @param itemId the entry from the shop config
     * @param seller whose shop it is, or {@code null} for a shop that belongs to nobody
     */
    public BedwarsPurchaseEvent(Game game, GamePlayer buyer, String itemId,
                                @Nullable GameTeam seller) {
        super(game);
        this.buyer = buyer;
        this.itemId = itemId;
        this.seller = seller;
    }

    public GamePlayer getBuyer() {
        return buyer;
    }

    public String getItemId() {
        return itemId;
    }

    public @Nullable GameTeam getSeller() {
        return seller;
    }

    /**
     * @return whether this shop belongs to a team the buyer is not in
     */
    public boolean isForeignShop() {
        return seller != null && !seller.equals(buyer.getTeam());
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

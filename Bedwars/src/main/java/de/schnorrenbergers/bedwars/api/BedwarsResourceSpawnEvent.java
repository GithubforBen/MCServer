package de.schnorrenbergers.bedwars.api;

import de.schnorrenbergers.bedwars.game.Game;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * A generator is about to drop something. Fired for every single drop, which is what lets an addon double
 * a generator for a while or stop one entirely without touching the generator code.
 */
public class BedwarsResourceSpawnEvent extends BedwarsEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String generatorType;
    private final Location location;
    private ItemStack drop;
    private boolean cancelled;

    /**
     * @param game          the round
     * @param generatorType which kind of generator this is, e.g. {@code DIAMOND}
     * @param location      where it drops
     * @param drop          what it drops
     */
    public BedwarsResourceSpawnEvent(Game game, String generatorType, Location location, ItemStack drop) {
        super(game);
        this.generatorType = generatorType;
        this.location = location;
        this.drop = drop;
    }

    public String getGeneratorType() {
        return generatorType;
    }

    public Location getLocation() {
        return location;
    }

    public ItemStack getDrop() {
        return drop;
    }

    /**
     * @param drop what should be dropped instead
     */
    public void setDrop(ItemStack drop) {
        if (drop != null) this.drop = drop;
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

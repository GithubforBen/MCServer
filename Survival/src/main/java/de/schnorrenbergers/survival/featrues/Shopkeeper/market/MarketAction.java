package de.schnorrenbergers.survival.featrues.Shopkeeper.market;

import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.ItemAction;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A clickable entry of the marketplace, so the panels do not have to spell out an anonymous
 * {@link ItemAction} for every single button.
 */
public class MarketAction implements ItemAction {

    private final UUID id = UUID.randomUUID();
    private final Consumer<InventoryClickEvent> onClick;
    private final Supplier<CustomInventory> next;

    /**
     * @param onClick what to do with the click, or {@code null} for a button that only navigates
     * @param next    the panel to open afterwards, or {@code null} to stay where we are
     */
    public MarketAction(Consumer<InventoryClickEvent> onClick, Supplier<CustomInventory> next) {
        this.onClick = onClick;
        this.next = next;
    }

    /**
     * @param next the panel to open
     * @return an action that only navigates
     */
    public static MarketAction opens(Supplier<CustomInventory> next) {
        return new MarketAction(null, next);
    }

    /**
     * @param onClick what to do with the click
     * @return an action that handles the click itself and opens nothing
     */
    public static MarketAction handles(Consumer<InventoryClickEvent> onClick) {
        return new MarketAction(onClick, null);
    }

    @Override
    public UUID getID() {
        return id;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (onClick != null) onClick.accept(event);
    }

    @Override
    public boolean isMovable() {
        return false;
    }

    @Override
    public boolean fireEvent() {
        return onClick != null;
    }

    @Override
    public CustomInventory loadInventoryOnClick() {
        return next == null ? null : next.get();
    }
}

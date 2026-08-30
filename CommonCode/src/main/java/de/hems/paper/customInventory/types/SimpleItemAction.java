package de.hems.paper.customInventory.types;

import de.hems.paper.customInventory.CustomInventory;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An {@link ItemAction} that just runs a piece of code when the item is clicked.
 * <p>
 * Every instance gets its own id, so the same button can be built again for another player or another
 * server without the two overwriting each other.
 */
public class SimpleItemAction implements ItemAction {

    private final UUID id = UUID.randomUUID();
    private final Consumer<InventoryClickEvent> onClick;
    private final Supplier<CustomInventory> next;

    public SimpleItemAction(Consumer<InventoryClickEvent> onClick) {
        this(onClick, null);
    }

    /**
     * @param onClick what to do with the click, or {@code null} for a button that only navigates
     * @param next    the menu to show afterwards, or {@code null} to stay where we are
     */
    public SimpleItemAction(Consumer<InventoryClickEvent> onClick, Supplier<CustomInventory> next) {
        this.onClick = onClick;
        this.next = next;
    }

    /**
     * @return an action that does nothing but still blocks the item from being taken out
     */
    public static SimpleItemAction display() {
        return new SimpleItemAction(null);
    }

    /**
     * @param next the menu to show
     * @return an action that only navigates. The menu is rebuilt on every click, so a button that changes
     *         something and then returns the same menu redraws it with the new values
     */
    public static SimpleItemAction opens(Supplier<CustomInventory> next) {
        return new SimpleItemAction(null, next);
    }

    /**
     * @param onClick what to do with the click
     * @param next    the menu to show afterwards
     * @return an action that does both
     */
    public static SimpleItemAction opens(Consumer<InventoryClickEvent> onClick, Supplier<CustomInventory> next) {
        return new SimpleItemAction(onClick, next);
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

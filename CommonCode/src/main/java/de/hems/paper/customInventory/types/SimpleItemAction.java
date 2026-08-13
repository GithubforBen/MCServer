package de.hems.paper.customInventory.types;

import de.hems.paper.customInventory.CustomInventory;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * An {@link ItemAction} that just runs a piece of code when the item is clicked.
 * <p>
 * Every instance gets its own id, so the same button can be built again for another player or another
 * server without the two overwriting each other.
 */
public class SimpleItemAction implements ItemAction {

    private final UUID id = UUID.randomUUID();
    private final Consumer<InventoryClickEvent> onClick;

    public SimpleItemAction(Consumer<InventoryClickEvent> onClick) {
        this.onClick = onClick;
    }

    /**
     * @return an action that does nothing but still blocks the item from being taken out
     */
    public static SimpleItemAction display() {
        return new SimpleItemAction(null);
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
        return null;
    }
}

package de.schnorrenbergers.survival.utils.customInventory;

import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.UUID;

public interface ItemAction {
    public UUID getID();
    public void onClick(InventoryClickEvent event);
    public boolean isMovable();
    public boolean fireEvent();
    public CustomInventory loadInventoryOnClick();
    public static ItemAction placeholder = new ItemAction() {
        @Override
        public UUID getID() {
            return UUID.fromString("f8c3de3d-1fea-4d7c-a8b0-29f63c4c3454");
        }

        @Override
        public void onClick(InventoryClickEvent event) {}//Ignored

        @Override
        public boolean isMovable() {
            return false;
        }

        @Override
        public boolean fireEvent() {
            return false;
        }

        @Override
        public CustomInventory loadInventoryOnClick() {
            return null;
        }
    };
}

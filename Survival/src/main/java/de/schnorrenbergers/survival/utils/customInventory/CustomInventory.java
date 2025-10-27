package de.schnorrenbergers.survival.utils.customInventory;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class CustomInventory {
    private Inventory inventory;
    public CustomInventory(int size) {
        inventory = Bukkit.createInventory(null, size);
    }
    public void setItem(int position, ItemStack stack, ItemAction action) {
        stack.lore().add(Component.text(action.getID().toString()));
    }
}

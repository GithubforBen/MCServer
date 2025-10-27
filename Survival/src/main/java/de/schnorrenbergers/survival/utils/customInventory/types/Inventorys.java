package de.schnorrenbergers.survival.utils.customInventory.types;

import de.hems.api.ItemApi;
import de.schnorrenbergers.survival.utils.customInventory.CustomInventory;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

import java.util.UUID;

public class Inventorys {
    /**
     * @return a configured {@link CustomInventory} instance
     *         representing an inventory setup for adding money
     */
    public static CustomInventory ADD_MONEY_INVENTORY() {
        CustomInventory customInventory = new CustomInventory(InventoryType.DISPENSER, "Geld Hinzufügen");
        for (int i = 0; i < 4; i++) {
            customInventory.setPlaceHolder(i);
        }
        customInventory.setItem(4, new ItemApi(Material.RED_BANNER, "TEST").build(), new ItemAction() {
            @Override
            public UUID getID() {
                return UUID.fromString("05d35c28-0ac0-4401-9d3e-2668123d66e");
            }

            @Override
            public void onClick(InventoryClickEvent event) {
                System.out.println("CLICK");
            }

            @Override
            public boolean isMovable() {
                return true;
            }

            @Override
            public boolean fireEvent() {
                return true;
            }

            @Override
            public CustomInventory loadInventoryOnClick() {
                return null;
            }
        });
        return customInventory;
    }
}

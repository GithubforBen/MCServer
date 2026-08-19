package de.schnorrenbergers.backpack;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Marks an inventory as a team backpack.
 * <p>
 * Recognising the window by its holder rather than by its title means a player cannot fool the plugin by
 * naming a chest the same thing, and the title stays free to be configured.
 */
public class BackpackHolder implements InventoryHolder {

    private final String teamName;
    private Inventory inventory;

    public BackpackHolder(String teamName) {
        this.teamName = teamName;
    }

    public String getTeamName() {
        return teamName;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}

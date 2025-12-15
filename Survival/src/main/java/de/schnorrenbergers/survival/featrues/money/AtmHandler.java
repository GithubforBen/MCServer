package de.schnorrenbergers.survival.featrues.money;

import de.hems.api.ItemApi;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class AtmHandler {
    public static void deposit(Player target, String name, int amount) {
        boolean invContains = target.getInventory().containsAtLeast(ItemStack.of(MoneyHandler.MONEY_ITEM), amount);
        if(invContains) {
            removeItems(target.getInventory(), MoneyHandler.MONEY_ITEM, amount);
            if (target.getScoreboard().getTeam(name) != null) {
                MoneyHandler.addMoney(amount * 100, target.getScoreboard().getTeam(name));
            } else {
                MoneyHandler.addMoney(amount * 100, UUID.fromString(name));
            }
            return;
        }
    }

    public static boolean payout(Player target, String name, int amount) {
        boolean removedMoney = false;
        if (target.getScoreboard().getTeam(name) != null) {
            MoneyHandler.removeMoney(amount * 100, target.getScoreboard().getTeam(name));
        } else {
            removedMoney = MoneyHandler.removeMoney(amount * 100, UUID.fromString(name));
        }
        if(removedMoney) {
            ItemStack itemStack = new ItemApi(MoneyHandler.MONEY_ITEM, amount).build();
            target.getInventory().addItem(itemStack);
        }
        return removedMoney;
    }

    private static void removeItems(Inventory inventory, Material type, int amount) {
        if (amount <= 0) return;
        int size = inventory.getSize();
        for (int slot = 0; slot < size; slot++) {
            ItemStack is = inventory.getItem(slot);
            if (is == null) continue;
            if (type == is.getType()) {
                int newAmount = is.getAmount() - amount;
                if (newAmount > 0) {
                    is.setAmount(newAmount);
                    break;
                } else {
                    inventory.clear(slot);
                    amount = -newAmount;
                    if (amount == 0) break;
                }
            }
        }
    }
}

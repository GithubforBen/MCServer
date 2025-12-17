package de.schnorrenbergers.survival.featrues.money;

import de.hems.api.ItemApi;
import org.bukkit.ChatColor;
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
            target.sendMessage(ChatColor.GREEN + String.format("✓ Du hast %s Bits eingezahlt.", amount * 100));
            return;
        }
        target.sendMessage(ChatColor.RED + "❌ Du hast nicht genügend Diamanten zum einzahlen.");
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
            target.sendMessage(ChatColor.BLUE + String.format("✓ Du hast %s Bits ausgezahlt.", amount * 100));
        } else {
            target.sendMessage(ChatColor.RED + "❌ Dein Kontostand ist nicht ausreichend.");
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

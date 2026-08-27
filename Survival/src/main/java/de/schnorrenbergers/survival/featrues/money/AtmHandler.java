package de.schnorrenbergers.survival.featrues.money;

import de.hems.api.ItemApi;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.UUID;

public class AtmHandler {

    /** What one of the money items is worth. */
    public static final int BITS_PER_ITEM = 100;

    /**
     * The balance of the account an ATM screen is showing.
     * <p>
     * An account is either a team, addressed by its name, or a single player, addressed by their uuid -
     * the same string the screen was opened with.
     *
     * @param account the team name or player uuid the ATM belongs to
     * @return how many bits are on it
     */
    public static int balance(String account) {
        Team team = teamOf(account);
        if (team != null) return MoneyHandler.getMoney(team);
        try {
            return MoneyHandler.getMoney(UUID.fromString(account));
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    /**
     * @param account the string an ATM was opened with
     * @return the team it names, or {@code null} when it names a player instead
     */
    public static Team teamOf(String account) {
        if (account == null) return null;
        return Bukkit.getScoreboardManager().getMainScoreboard().getTeam(account);
    }

    /**
     * @param account the string an ATM was opened with
     * @return what to call that account in the interface
     */
    public static String nameOf(String account) {
        Team team = teamOf(account);
        if (team != null) return "Team " + team.getName();
        try {
            String name = Bukkit.getOfflinePlayer(UUID.fromString(account)).getName();
            return name == null ? "Eigenes Konto" : name;
        } catch (IllegalArgumentException e) {
            return "Unbekannt";
        }
    }

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

    /**
     * Turns bits back into items.
     *
     * @param target  who is at the ATM
     * @param name    the team name or player uuid the money comes from
     * @param amount  how many money items to hand out
     * @return whether the account had enough
     */
    public static boolean payout(Player target, String name, int amount) {
        int bits = amount * BITS_PER_ITEM;
        Team team = target.getScoreboard().getTeam(name);
        UUID account = null;
        if (team == null) {
            // a personal account is addressed by uuid. This used to give up here, which meant the payout
            // button of every player ATM did nothing at all
            try {
                account = UUID.fromString(name);
            } catch (IllegalArgumentException e) {
                target.sendMessage(ChatColor.RED + "❌ Dieses Konto gibt es nicht.");
                return false;
            }
        }
        boolean removedMoney = team != null
                ? MoneyHandler.removeMoney(bits, team)
                : MoneyHandler.removeMoney(bits, account);
        if (!removedMoney) {
            target.sendMessage(ChatColor.RED + "❌ Dein Kontostand ist nicht ausreichend.");
            return false;
        }

        ItemStack itemStack = new ItemApi(MoneyHandler.MONEY_ITEM, amount).build();
        Map<Integer, ItemStack> notDelivered = target.getInventory().addItem(itemStack);
        if (!notDelivered.isEmpty()) {
            // the money has already left the account, so it goes back onto the same one it came from -
            // paying a team's money into the personal purse of whoever pressed the button would be theft
            int refund = 0;
            for (ItemStack leftover : notDelivered.values()) refund += leftover.getAmount() * BITS_PER_ITEM;
            if (team != null) {
                MoneyHandler.addMoney(refund, team);
            } else {
                MoneyHandler.addMoney(refund, account);
            }
            target.sendMessage(ChatColor.RED + "❌ In deinem Inventar war nicht genug Platz - "
                    + refund + " Bits sind auf dem Konto geblieben.");
            if (refund >= bits) return false;
        }
        target.sendMessage(ChatColor.BLUE + String.format("✓ Du hast %s Bits ausgezahlt.", bits));
        return true;
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

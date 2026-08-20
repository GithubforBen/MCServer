package de.schnorrenbergers.bedwars.shop;

import de.schnorrenbergers.bedwars.api.BedwarsPurchaseEvent;
import de.schnorrenbergers.bedwars.config.ShopSettings;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.game.Loadout;
import de.schnorrenbergers.bedwars.shop.item.ShopItem;
import de.schnorrenbergers.bedwars.shop.item.ShopItems;
import de.schnorrenbergers.bedwars.util.Messages;
import de.schnorrenbergers.bedwars.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Buying things, and getting them back after a death.
 * <p>
 * Both halves belong together: what a purchase does to a player is exactly what has to happen again when
 * they respawn, and splitting the two is how a shop ends up handing out a diamond sword that quietly turns
 * into a wooden one at the first death.
 */
public class ShopService {

    private final ShopSettings settings;

    public ShopService(ShopSettings settings) {
        this.settings = settings;
    }

    public ShopSettings getSettings() {
        return settings;
    }

    // -------------------------------------------------------------------- buying

    /**
     * Sells one entry to a player.
     *
     * @param game   the round
     * @param buyer  who is buying
     * @param item   what they picked
     * @param seller whose shop it is, {@code null} for a shop that belongs to nobody
     * @return whether it worked
     */
    public boolean buy(Game game, GamePlayer buyer, ShopItem item, @Nullable GameTeam seller) {
        Player player = buyer.getPlayer();
        if (player == null || !buyer.isAlive()) return false;

        BedwarsPurchaseEvent event = new BedwarsPurchaseEvent(game, buyer, item.id(), seller);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        if (owned(buyer.getLoadout(), item)) {
            Messages.send(player, "shop.already-owned", "item", plain(item));
            return false;
        }
        if (!item.sellableBy(seller, buyer.getTeam())) {
            Messages.send(player, "shop.enemy-only");
            return false;
        }
        Cost missing = Cost.shortfall(player, item.costs());
        if (missing != null) {
            Messages.send(player, "shop.cannot-afford",
                    "amount", String.valueOf(missing.missing(player)),
                    "currency", missing.currency().getDisplayName());
            player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }
        Cost.take(player, item.costs());
        deliver(game, buyer, player, item);
        Messages.send(player, "shop.bought",
                "item", plain(item),
                "amount", String.valueOf(item.amount()));
        player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.6f);
        return true;
    }

    /**
     * @return whether the player already has this, for the entries that can only be had once
     */
    private boolean owned(Loadout loadout, ShopItem item) {
        if (item.isArmor()) return loadout.getArmorTier() >= item.armorTier();
        if (item.isTool()) return loadout.getToolTier(item.toolGroup()) >= item.toolTier();
        if (item.permanent()) return loadout.getPermanent().contains(item.id());
        return false;
    }

    /**
     * Hands a purchase over and writes down what of it has to survive a death.
     */
    private void deliver(Game game, GamePlayer buyer, Player player, ShopItem item) {
        Loadout loadout = buyer.getLoadout();
        GameTeam team = buyer.getTeam();
        if (item.isArmor()) {
            loadout.setArmorTier(item.armorTier());
            wearArmor(player, item);
        } else if (item.isTool()) {
            loadout.setToolTier(item.toolGroup(), item.toolTier());
            replaceTool(player, item);
        } else {
            if (item.permanent()) loadout.addPermanent(item.id());
            if (item.sword()) dropWoodenSword(player);
            give(player, ShopItems.build(item, team));
        }
        refresh(game, buyer, player);
    }

    /**
     * Puts a bought armour level on. Helmet and chestplate stay the team's leather, because the colour a
     * player runs at you in is the only way to tell whose side they are on.
     */
    private void wearArmor(Player player, ShopItem item) {
        PlayerInventory inventory = player.getInventory();
        for (ItemStack piece : ShopItems.armorPieces(item)) {
            if (piece.getType().name().endsWith("_BOOTS")) {
                inventory.setBoots(piece);
            } else {
                inventory.setLeggings(piece);
            }
        }
    }

    /**
     * Gives a tool and takes the weaker one of the same chain away, so that buying an iron pickaxe does not
     * leave the wooden one taking up a slot for the rest of the round.
     */
    private void replaceTool(Player player, ShopItem item) {
        removeGroup(player, item.toolGroup());
        give(player, ShopItems.build(item, null));
    }

    /**
     * Takes every item of one tool chain out of an inventory.
     */
    private void removeGroup(Player player, String group) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getStorageContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ShopItem bought = settings.get(ShopItems.idOf(contents[slot]));
            if (bought != null && bought.isTool() && bought.toolGroup().equalsIgnoreCase(group)) {
                inventory.setItem(slot, null);
            }
        }
    }

    /**
     * Takes the wooden sword away when a better one is bought - hypixel's rule, and the reason a player
     * with a diamond sword does not carry three swords around.
     */
    private void dropWoodenSword(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getStorageContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (contents[slot] != null && contents[slot].getType() == Material.WOODEN_SWORD) {
                inventory.setItem(slot, null);
            }
        }
    }

    // ------------------------------------------------------------------ respawning

    /**
     * Gives a player back everything they own after a death: armour as it was, tools one level lower,
     * whatever is permanent, and the team upgrades on top of all of it.
     *
     * @param game        the round
     * @param participant who came back
     */
    public void restore(Game game, GamePlayer participant) {
        Player player = participant.getPlayer();
        if (player == null) return;
        Loadout loadout = participant.getLoadout();

        ShopItem armor = settings.getArmor(loadout.getArmorTier());
        if (armor != null) wearArmor(player, armor);

        for (Map.Entry<String, Integer> tool : loadout.getTools().entrySet()) {
            ShopItem step = settings.getTool(tool.getKey(), tool.getValue());
            if (step != null) give(player, ShopItems.build(step, null));
        }
        for (String id : loadout.getPermanent()) {
            ShopItem item = settings.get(id);
            if (item != null) give(player, ShopItems.build(item, participant.getTeam()));
        }
        refresh(game, participant, player);
    }

    /**
     * Puts the team's upgrades back onto whatever the player is now carrying.
     */
    private void refresh(Game game, GamePlayer participant, Player player) {
        if (game.getUpgrades() == null || participant.getTeam() == null) return;
        game.getUpgrades().applyTo(player, participant.getTeam());
    }

    /**
     * Puts an item into an inventory, on the floor when there is no room for it.
     */
    private void give(Player player, ItemStack stack) {
        player.getInventory().addItem(stack).values()
                .forEach(rest -> player.getWorld().dropItem(player.getLocation(), rest));
    }

    /**
     * @param item a shop entry
     * @return its name without any formatting, for a chat line that already has its own
     */
    private static String plain(ShopItem item) {
        return Text.plain(item.displayName());
    }
}

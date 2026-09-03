package de.schnorrenbergers.bedwars.game;

import de.hems.api.ItemApi;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;

/**
 * What a player carries and wears.
 * <p>
 * Armour is dyed rather than named, because in a fight you read a team off the colour of the person
 * running at you and never off a tooltip. It is also the reason armour is unbreakable: leather that falls
 * apart mid round would quietly turn somebody's team invisible.
 */
public final class Equipment {

    private Equipment() {
    }

    /**
     * Clears a player and gives them what they start a life with.
     *
     * @param player who to equip
     * @param team   their team, for the colour
     */
    public static void giveStartingKit(Player player, GameTeam team) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setItem(0, new ItemStack(Material.WOODEN_SWORD));
        dressUp(player, team);
    }

    /**
     * Puts the team's armour on, without touching anything else.
     *
     * @param player who to dress
     * @param team   their team
     */
    public static void dressUp(Player player, GameTeam team) {
        PlayerInventory inventory = player.getInventory();
        inventory.setHelmet(dyed(Material.LEATHER_HELMET, team));
        inventory.setChestplate(dyed(Material.LEATHER_CHESTPLATE, team));
        inventory.setLeggings(dyed(Material.LEATHER_LEGGINGS, team));
        inventory.setBoots(dyed(Material.LEATHER_BOOTS, team));
    }

    /**
     * @param material which piece
     * @param team     whose colour
     * @return the piece, dyed and unbreakable
     */
    private static ItemStack dyed(Material material, GameTeam team) {
        ItemStack piece = new ItemStack(material);
        if (piece.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(team.getColor().getArmorColor());
            meta.setUnbreakable(true);
            piece.setItemMeta(meta);
        }
        return piece;
    }

    /**
     * Puts a player back on their feet: full health, fed, no effects, nothing burning.
     *
     * @param player who to reset
     * @param where  where to put them
     */
    public static void reset(Player player, Location where) {
        // the end of a round makes everybody untouchable so that the celebration cannot be fought over,
        // and nothing ever took that back: a player who went through an end phase started the next round
        // unkillable and only lost it by dying once, which they could not do
        player.setInvulnerable(false);
        player.setHealth(healthOf(player));
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.setFallDistance(0f);
        player.setLevel(0);
        player.setExp(0f);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        player.getInventory().clear();
        if (where != null) player.teleport(where);
    }

    /**
     * @param player the player
     * @return how much health they can have, which a potion or an addon may have changed
     */
    private static double healthOf(Player player) {
        var attribute = player.getAttribute(Attribute.MAX_HEALTH);
        return attribute == null ? 20.0d : attribute.getValue();
    }

    /**
     * Empties a player's ender chest.
     * <p>
     * Deliberately not part of {@link #reset(Player, Location)}: an ender chest is the one thing in a
     * round that is meant to survive a death, so emptying it every respawn would take away the only
     * reason to use one. It is emptied when a round begins and when somebody lands in the waiting lobby -
     * an ender chest is per player and lives in their save file, so without this the diamonds of the last
     * match are still in it when the next one starts.
     *
     * @param player whose chest to empty
     */
    public static void clearEnderChest(Player player) {
        player.getEnderChest().clear();
    }

    /**
     * Takes the starting sword away from somebody who is carrying a better one.
     * <p>
     * Hypixel's rule, and the reason a player with a diamond sword does not walk around with three swords.
     * It is asked of the whole inventory rather than of the purchase that triggered it, because a sword
     * arrives from more than one direction: the shop, a kit, and anything picked up off the floor.
     *
     * @param player whose inventory to tidy
     */
    public static void dropWoodenSword(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getStorageContents();
        if (!hasBetterSword(contents)) return;
        for (int slot = 0; slot < contents.length; slot++) {
            if (contents[slot] != null && contents[slot].getType() == Material.WOODEN_SWORD) {
                inventory.setItem(slot, null);
            }
        }
    }

    /**
     * @param contents an inventory
     * @return whether anything in it is a sword that is not the wooden one
     */
    private static boolean hasBetterSword(ItemStack[] contents) {
        for (ItemStack stack : contents) {
            if (stack == null || stack.getType() == Material.WOODEN_SWORD) continue;
            if (stack.getType().name().endsWith("_SWORD")) return true;
        }
        return false;
    }

    /**
     * @param team the team the item is for
     * @return the wool that stands for a team in the team menu
     */
    public static ItemStack teamWool(GameTeam team, java.util.List<String> lore) {
        return new ItemApi(team.getColor().getWool(), team.getColor().getDisplayName(), lore).build();
    }
}

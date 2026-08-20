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
     * @param team the team the item is for
     * @return the wool that stands for a team in the team menu
     */
    public static ItemStack teamWool(GameTeam team, java.util.List<String> lore) {
        return new ItemApi(team.getColor().getWool(), team.getColor().getDisplayName(), lore).build();
    }
}

package com.benschnorr.bedwars.listener;

import com.benschnorr.bedwars.Bedwars;
import com.benschnorr.bedwars.config.Feature;
import com.benschnorr.bedwars.game.Game;
import com.benschnorr.bedwars.game.GamePlayer;
import com.benschnorr.bedwars.shop.ShopMenu;
import com.benschnorr.bedwars.shop.upgrade.UpgradeMenu;
import com.benschnorr.bedwars.shop.villager.ShopKeepers;
import com.benschnorr.bedwars.util.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;

/**
 * The two things that can be done to a shop keeper: clicking it, and trying to kill it.
 * <p>
 * The second one matters as much as the first. A villager that can be pushed into the void or shot off its
 * platform would take a team's shop with it, so a keeper is untouchable and the click never reaches the
 * villager's own trading screen.
 */
public class ShopListener implements Listener {

    public ShopListener(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEntityEvent event) {
        ShopKeepers.Keeper keeper = keeperOf(event);
        if (keeper == null) return;
        // both hands fire this, and opening the menu twice closes it again on the second one
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) return;

        Game game = Bedwars.getInstance().getGame();
        Player player = event.getPlayer();
        GamePlayer shopper = game.get(player);
        if (shopper == null || !shopper.isAlive()) {
            Messages.send(player, "shop.not-playing");
            return;
        }
        if (!Bedwars.getInstance().getFeatureSettings().is(Feature.SHOP)) {
            Messages.send(player, "shop.closed");
            return;
        }
        if (keeper.upgrades()) {
            UpgradeMenu.open(player);
        } else {
            ShopMenu.open(player, keeper.team());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        ShopKeepers keepers = keepers();
        if (keepers == null || keepers.get(event.getEntity()) == null) return;
        event.setCancelled(true);
    }

    /**
     * @param event a click on something
     * @return the keeper that was clicked, or {@code null} when it was anything else
     */
    private static ShopKeepers.Keeper keeperOf(PlayerInteractEntityEvent event) {
        ShopKeepers keepers = keepers();
        return keepers == null ? null : keepers.get(event.getRightClicked());
    }

    private static ShopKeepers keepers() {
        Bedwars plugin = Bedwars.getInstance();
        return plugin == null || plugin.getGame() == null ? null : plugin.getGame().getShopKeepers();
    }
}

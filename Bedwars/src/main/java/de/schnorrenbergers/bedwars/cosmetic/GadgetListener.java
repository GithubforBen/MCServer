package de.schnorrenbergers.bedwars.cosmetic;

import de.hems.paper.cosmetic.CosmeticService;
import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.CosmeticType;
import de.hems.types.cosmetic.Cosmetics;
import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * The gadgets, as far as a bedwars round is concerned.
 * <p>
 * Right now there is one: an ender pearl that is not used up. The trade is a longer cooldown than a normal
 * pearl has, which is the whole balance of the thing - the pearl itself does nothing a bought one does not
 * do, it just never runs out.
 * <p>
 * The cooldown lives in the cosmetic's settings rather than in this file, because how strong "never runs
 * out" is depends on how the rest of the round plays, and that is a number to turn rather than a version
 * to ship.
 */
public class GadgetListener implements Listener {

    /** What the cooldown is when nobody set one: vanilla's second, plus the ten percent it costs. */
    private static final int DEFAULT_COOLDOWN_TICKS = 22;

    private final Plugin plugin;

    public GadgetListener(Plugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof EnderPearl pearl)) return;
        if (!(pearl.getShooter() instanceof Player player)) return;
        if (!isPlaying(player)) return;

        CosmeticData gadget = CosmeticService.getSelected(player.getUniqueId(), CosmeticType.GADGET);
        if (gadget == null || !Cosmetics.GADGET_ENDLESS_PEARL.equalsIgnoreCase(gadget.getId())) return;

        int cooldown = Math.max(1, gadget.getNumber(Cosmetics.SETTING_COOLDOWN_TICKS, DEFAULT_COOLDOWN_TICKS));
        // a tick later: the pearl this throw consumed is taken out of the inventory after the event, so
        // putting one back now would be the one that is then removed
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || !isPlaying(player)) return;
            giveBack(player);
            player.setCooldown(Material.ENDER_PEARL, cooldown);
            player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 1.6f);
        });
    }

    /**
     * Puts the thrown pearl back, unless the player is already carrying one.
     * <p>
     * Only ever one: the gadget is a pearl that comes back, not a pearl that multiplies, and a full
     * inventory would otherwise turn it into a machine for dropping them on the floor.
     *
     * @param player who threw it
     */
    private void giveBack(Player player) {
        if (player.getInventory().contains(Material.ENDER_PEARL)) return;
        if (player.getInventory().firstEmpty() < 0) return;
        player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, 1));
    }

    /**
     * @param player somebody
     * @return whether they are actually in the round rather than watching it
     */
    private boolean isPlaying(Player player) {
        Game game = Bedwars.getInstance().getGame();
        if (game == null || !game.isRunning()) return false;
        GamePlayer participant = game.get(player);
        return participant != null && participant.isPlaying();
    }
}

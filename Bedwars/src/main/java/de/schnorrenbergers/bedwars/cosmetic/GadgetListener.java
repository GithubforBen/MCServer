package de.schnorrenbergers.bedwars.cosmetic;

import de.hems.paper.cosmetic.CosmeticService;
import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.CosmeticType;
import de.hems.types.cosmetic.Cosmetics;
import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.api.BedwarsGameStateChangeEvent;
import de.schnorrenbergers.bedwars.api.BedwarsPlayerRespawnEvent;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.phase.PhaseType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * The gadgets, as far as a bedwars round is concerned.
 * <p>
 * Right now there is one: an ender pearl that is not used up. Its owner is handed one when the round
 * begins and again whenever they respawn, and every pearl they throw comes back after a cooldown. The
 * cooldown is the whole balance of the thing - the pearl does nothing a bought one does not do, it simply
 * never runs out - so it lives in the cosmetic's settings and can be turned without a new version.
 */
public class GadgetListener implements Listener {

    /** What the cooldown is when nobody set one: vanilla's second, plus the ten percent it costs. */
    private static final int DEFAULT_COOLDOWN_TICKS = 22;

    private final Plugin plugin;

    public GadgetListener(Plugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /* ------------------------------------------------------------------ handing it out */

    @EventHandler(priority = EventPriority.MONITOR)
    public void onStateChange(BedwarsGameStateChangeEvent event) {
        if (event.getTo() != PhaseType.RUNNING) return;
        Game game = event.getGame();
        // a tick later: the running phase is handing out the starting kit right now, and anything given
        // before that is given to an inventory that is about to be cleared
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (GamePlayer participant : game.getPlayers()) {
                if (!participant.isPlaying()) continue;
                Player player = participant.getPlayer();
                if (player != null) handOut(player, true);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(BedwarsPlayerRespawnEvent event) {
        GamePlayer participant = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Player player = participant.getPlayer();
            if (player != null) handOut(player, false);
        });
    }

    /**
     * Gives somebody their gadget pearl, if that is what they are wearing.
     *
     * @param player   who
     * @param announce whether to tell them, which is worth doing once a round and not on every respawn
     */
    private void handOut(Player player, boolean announce) {
        if (gadgetOf(player) == null) return;
        if (!give(player)) return;
        if (!announce) return;
        player.sendMessage(Component.text("Endlos-Perle: geworfen kommt sie nach kurzer Zeit zurück.",
                NamedTextColor.LIGHT_PURPLE));
    }

    /* ------------------------------------------------------------------ giving it back */

    @EventHandler
    public void onLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof EnderPearl pearl)) return;
        if (!(pearl.getShooter() instanceof Player player)) return;
        if (!isPlaying(player)) return;

        CosmeticData gadget = gadgetOf(player);
        if (gadget == null) return;

        int cooldown = Math.max(1, gadget.getNumber(Cosmetics.SETTING_COOLDOWN_TICKS, DEFAULT_COOLDOWN_TICKS));
        UUID id = player.getUniqueId();
        // the cooldown is on the material, so it is on every pearl they carry - which is the point: the
        // gadget is a pearl that comes back, not a way around the pearl cooldown
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Player thrower = plugin.getServer().getPlayer(id);
            if (thrower != null) thrower.setCooldown(Material.ENDER_PEARL, cooldown);
        });
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Player thrower = plugin.getServer().getPlayer(id);
            if (thrower == null || !isPlaying(thrower)) return;
            if (gadgetOf(thrower) == null) return;
            if (give(thrower)) {
                thrower.playSound(thrower, Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 1.6f);
            }
        }, cooldown);
    }

    /**
     * @param player somebody
     * @return the endless pearl if that is the gadget they are wearing, otherwise {@code null}
     */
    private CosmeticData gadgetOf(Player player) {
        CosmeticData gadget = CosmeticService.getSelected(player.getUniqueId(), CosmeticType.GADGET);
        if (gadget == null) return null;
        return Cosmetics.GADGET_ENDLESS_PEARL.equalsIgnoreCase(gadget.getId()) ? gadget : null;
    }

    /**
     * Puts one pearl into somebody's inventory.
     *
     * @param player who
     * @return whether it fitted
     */
    private boolean give(Player player) {
        // addItem says what it could not place, so an empty answer is the whole answer: a full inventory
        // simply does not get the pearl, rather than having it dropped at its owner's feet mid fight
        return player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, 1)).isEmpty();
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

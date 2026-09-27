package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.Cosmetics;
import de.hems.types.cosmetic.GadgetSlot;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * An ender pearl that is not used up.
 * <p>
 * The cooldown is the whole balance of it - the pearl does nothing a bought one does not do, it simply
 * never runs out - so it lives in the cosmetic's settings and can be turned without a new version.
 * <p>
 * Lobby and survival, not bedwars. In a round a pearl that never runs out is the fastest way onto every
 * island and out of every fight - it decides games, and that is not something to sell for bits. Where
 * people build and stand around it is a way of getting about. A parkour run in the lobby ends when one
 * is thrown, see the lobby's {@code CheckpointListener}.
 */
public class EndlessPearlGadget implements Gadget, Listener {

    /** What the cooldown is when nobody set one: vanilla's second, plus the ten percent it costs. */
    private static final int DEFAULT_COOLDOWN_TICKS = 22;

    private final Plugin plugin;

    public EndlessPearlGadget(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return Cosmetics.GADGET_ENDLESS_PEARL;
    }

    @Override
    public Set<GadgetSlot> slots() {
        return Set.of(GadgetSlot.LOBBY, GadgetSlot.SURVIVAL);
    }

    @Override
    public ItemStack item(CosmeticData cosmetic) {
        // marked, so that only this pearl comes back: a pearl somebody found or bought is thrown and gone
        // like any other, and the one in their pocket is recognised on the next join instead of doubled
        return GadgetItems.of(Material.ENDER_PEARL, getId(), "Endlos-Perle", "Kommt nach dem Wurf zurück");
    }

    @Override
    public @Nullable String hint() {
        return "Endlos-Perle: geworfen kommt sie nach kurzer Zeit zurück.";
    }

    @EventHandler
    public void onLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof EnderPearl pearl)) return;
        if (!(pearl.getShooter() instanceof Player player)) return;
        if (!GadgetItems.is(pearl.getItem(), getId())) return;
        CosmeticData gadget = Gadgets.settingsFor(player, getId());
        if (gadget == null) return;

        int cooldown = Math.max(1,
                gadget.getNumber(Cosmetics.SETTING_COOLDOWN_TICKS, DEFAULT_COOLDOWN_TICKS));
        // the cooldown is on the material, so it is on every pearl they carry - which is the point: the
        // gadget is a pearl that comes back, not a way around the pearl cooldown
        Gadgets.later(plugin, player, getId(), 1L,
                (thrower, worn) -> thrower.setCooldown(Material.ENDER_PEARL, cooldown));
        Gadgets.later(plugin, player, getId(), cooldown, (thrower, worn) -> {
            if (Gadgets.give(thrower, item(worn))) {
                thrower.playSound(thrower, Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 1.6f);
            }
        });
    }
}

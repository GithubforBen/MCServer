package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.CosmeticType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Which gadgets this server has code for, and when they are allowed to work.
 * <p>
 * A gadget is the one kind of cosmetic that changes the game rather than decorating it, so it needs one
 * thing the others do not: an answer to "is this person actually playing right now". That answer belongs
 * to the game mode - a bedwars round knows about spectators and about the lobby before the start, and this
 * class must not - so the mode hands it in once as {@link #setGuard(Predicate)} and every gadget asks
 * through {@link #settingsFor(Player, String)}.
 * <p>
 * Until a mode does hand one in, gadgets do nothing at all. That way round because the same jar runs on
 * the survival server: an endless ender pearl is a cosmetic in a twenty minute round and an economy in a
 * world people build in, and defaulting to on would have made that decision for a server that never asked
 * for it. Win effects, kill effects and trails need no such switch - they are pictures.
 */
public final class Gadgets {

    private static final Map<String, Gadget> gadgets = new LinkedHashMap<>();
    /** Who may use a gadget at all. Nobody, until a game mode switches them on. */
    private static volatile Predicate<Player> guard = player -> false;
    /** Whether a game mode switched them on, which is not the same as anybody passing the guard. */
    private static volatile boolean enabled;

    private Gadgets() {
    }

    /**
     * @param plugin the plugin it runs on
     * @param gadget a gadget this server has code for; registered as a listener when it is one
     */
    public static void register(Plugin plugin, Gadget gadget) {
        if (gadget == null) return;
        gadgets.put(key(gadget.getId()), gadget);
        if (plugin != null && gadget instanceof Listener listener) {
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        }
    }

    /**
     * Switches the gadgets on for this server.
     *
     * @param guard what the game mode counts as being in the game, {@code null} to switch them off again
     */
    public static void setGuard(@Nullable Predicate<Player> guard) {
        Gadgets.guard = guard == null ? player -> false : guard;
        Gadgets.enabled = guard != null;
    }

    /**
     * @return whether this server uses gadgets at all, which is what the shop needs to know to say so
     */
    public static boolean areEnabled() {
        return enabled;
    }

    /**
     * The one question every gadget asks: does this player have me on, and may they use me right now.
     *
     * @param player   somebody
     * @param gadgetId the gadget asking
     * @return the gadget as the launcher has it - for its settings - or {@code null} when the answer is no
     */
    public static @Nullable CosmeticData settingsFor(Player player, String gadgetId) {
        if (player == null || !guard.test(player)) return null;
        CosmeticData worn = CosmeticService.getSelected(player.getUniqueId(), CosmeticType.GADGET);
        if (worn == null || !key(worn.getId()).equals(key(gadgetId))) return null;
        return worn;
    }

    /**
     * Hands somebody the item of whatever gadget they are wearing.
     * <p>
     * Called at the start of a round and again after every respawn, because a gadget that is gone the
     * first time its owner dies is a gadget they bought once.
     *
     * @param player   who
     * @param announce whether to say what it does, which is worth doing once a round and not on every
     *                 respawn
     * @return whether they were given anything
     */
    public static boolean handOut(Player player, boolean announce) {
        if (player == null || !guard.test(player)) return false;
        CosmeticData worn = CosmeticService.getSelected(player.getUniqueId(), CosmeticType.GADGET);
        if (worn == null) return false;
        Gadget gadget = gadgets.get(key(worn.getId()));
        if (gadget == null) return false;

        ItemStack item = gadget.item(worn);
        if (item == null || !give(player, item)) return false;
        if (announce && gadget.hint() != null) {
            player.sendMessage(Component.text(gadget.hint(), NamedTextColor.LIGHT_PURPLE));
        }
        return true;
    }

    /**
     * Puts one item into somebody's inventory.
     *
     * @param player who
     * @param item   what
     * @return whether it fitted
     */
    public static boolean give(Player player, ItemStack item) {
        if (player == null || item == null) return false;
        // addItem says what it could not place, so an empty answer is the whole answer: a full inventory
        // simply does not get it, rather than having it dropped at its owner's feet mid fight
        return player.getInventory().addItem(item).isEmpty();
    }

    /**
     * Runs something for a player a while from now, if they are still there and still wearing the gadget.
     *
     * @param plugin   the plugin it runs on
     * @param player   who
     * @param gadgetId which gadget wants it
     * @param ticks    how long from now
     * @param what     what to do
     */
    public static void later(Plugin plugin, Player player, String gadgetId, long ticks,
                             java.util.function.BiConsumer<Player, CosmeticData> what) {
        if (plugin == null || player == null || what == null) return;
        java.util.UUID id = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player still = Bukkit.getPlayer(id);
            if (still == null) return;
            CosmeticData worn = settingsFor(still, gadgetId);
            if (worn != null) what.accept(still, worn);
        }, Math.max(1L, ticks));
    }

    /**
     * @return the ids this server has code for
     */
    public static List<String> registered() {
        return List.copyOf(gadgets.keySet());
    }

    private static String key(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT);
    }
}

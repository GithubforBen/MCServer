package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.CosmeticType;
import de.hems.types.cosmetic.GadgetSlot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Which gadgets this server has code for, which slot it is, and when they are allowed to work.
 * <p>
 * A gadget is the one kind of cosmetic that changes the game rather than decorating it, so it needs two
 * things the others do not. An answer to "is this person actually playing right now", which belongs to
 * the game mode - a bedwars round knows about spectators and about the lobby before the start, and this
 * class must not - and an answer to "which slot is this server", because a player wears one gadget per
 * slot rather than one in total. A mode hands both in at once, see {@link #setGuard(Predicate, GadgetSlot)}.
 * <p>
 * Until a mode does hand them in, gadgets do nothing at all. That way round because the same jar runs
 * everywhere: an endless ender pearl is a cosmetic in a twenty minute round and an economy in a world
 * people build in, and defaulting to on would have made that decision for a server that never asked for
 * it. Win effects, kill effects and trails need no such switch - they are pictures.
 */
public final class Gadgets {

    /** How often the passive gadgets get their turn, in ticks. */
    private static final int INTERVAL = 2;

    private static final Map<String, Gadget> gadgets = new LinkedHashMap<>();
    /** What each player is wearing right now, so taking a gadget off cleans up after it exactly once. */
    private static final Map<UUID, String> active = new ConcurrentHashMap<>();
    /** Who may use a gadget at all. Nobody, until a game mode switches them on. */
    private static volatile Predicate<Player> guard = player -> false;
    /** Which slot this server is, or {@code null} while gadgets are off. */
    private static volatile GadgetSlot slot;
    private static boolean running;

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
     * @param slot  which slot this server is; without one there is no choice to read
     */
    public static void setGuard(@Nullable Predicate<Player> guard, @Nullable GadgetSlot slot) {
        Gadgets.slot = guard == null ? null : slot;
        Gadgets.guard = guard == null || slot == null ? player -> false : guard;
    }

    /**
     * @return whether this server uses gadgets at all, which is what the shop needs to know to say so
     */
    public static boolean areEnabled() {
        return slot != null;
    }

    /**
     * @return the slot this server is, or {@code null} when it has no gadgets
     */
    public static @Nullable GadgetSlot slot() {
        return slot;
    }

    /**
     * @param gadgetId a gadget
     * @return where it works, empty when this build has no code for it
     */
    public static Set<GadgetSlot> slotsOf(String gadgetId) {
        Gadget gadget = gadgets.get(key(gadgetId));
        return gadget == null ? Set.of() : gadget.slots();
    }

    /**
     * What somebody is wearing here, if they may use it right now.
     *
     * @param player somebody
     * @return the gadget as the launcher has it, or {@code null} when they wear none that works here
     */
    public static @Nullable CosmeticData worn(Player player) {
        GadgetSlot here = slot;
        if (player == null || here == null || !guard.test(player)) return null;
        CosmeticData chosen = CosmeticService.getSelected(player.getUniqueId(), CosmeticType.GADGET, here);
        if (chosen == null) return null;
        // a choice outlives the version that allowed it: a gadget that used to work here and no longer
        // does must not keep working for whoever still has it on
        Gadget gadget = gadgets.get(key(chosen.getId()));
        return gadget != null && gadget.slots().contains(here) ? chosen : null;
    }

    /**
     * The one question every gadget asks: does this player have me on, and may they use me right now.
     *
     * @param player   somebody
     * @param gadgetId the gadget asking
     * @return the gadget as the launcher has it - for its settings - or {@code null} when the answer is no
     */
    public static @Nullable CosmeticData settingsFor(Player player, String gadgetId) {
        CosmeticData chosen = worn(player);
        if (chosen == null || !key(chosen.getId()).equals(key(gadgetId))) return null;
        return chosen;
    }

    /**
     * Hands somebody the item of whatever gadget they are wearing.
     * <p>
     * Called at the start of a round and again after every respawn, because a gadget that is gone the
     * first time its owner dies is a gadget they bought once. The passive ones hand out nothing and still
     * say their line, so their owner knows they are on.
     *
     * @param player   who
     * @param announce whether to say what it does, which is worth doing once a round and not on every
     *                 respawn
     * @return whether they were given anything
     */
    public static boolean handOut(Player player, boolean announce) {
        CosmeticData chosen = worn(player);
        if (chosen == null) return false;
        Gadget gadget = gadgets.get(key(chosen.getId()));
        if (gadget == null) return false;

        // the lobby hands out on every join, respawn and world change, and nothing clears an inventory
        // in between - without this somebody would collect a rocket per trip through the hub
        ItemStack item = GadgetItems.has(player, gadget.getId()) ? null : gadget.item(chosen);
        boolean given = item != null && give(player, item);
        if (announce && gadget.hint() != null) {
            player.sendMessage(Component.text(gadget.hint(), NamedTextColor.LIGHT_PURPLE));
        }
        return given;
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
        UUID id = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player still = Bukkit.getPlayer(id);
            if (still == null) return;
            CosmeticData chosen = settingsFor(still, gadgetId);
            if (chosen != null) what.accept(still, chosen);
        }, Math.max(1L, ticks));
    }

    /**
     * Starts the loop the passive gadgets run on, and the clean-up that goes with it.
     * <p>
     * One loop for all of them, and it does one lookup per player: what are they wearing here. Everything
     * else follows from that answer changing - a gadget that is no longer worn is told to take its things
     * back, exactly once, whether it stopped because its owner took it off, left the place it works in,
     * or logged off.
     *
     * @param plugin the plugin it runs on
     */
    public static synchronized void start(Plugin plugin) {
        if (running || plugin == null) return;
        running = true;
        plugin.getServer().getPluginManager().registerEvents(new QuitListener(), plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, Gadgets::tickAll, INTERVAL, INTERVAL);
    }

    private static void tickAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            CosmeticData chosen = worn(player);
            String wanted = chosen == null ? null : key(chosen.getId());
            String previous = active.get(player.getUniqueId());
            if (previous != null && !previous.equals(wanted)) stop(player, previous);
            if (chosen == null) continue;

            active.put(player.getUniqueId(), wanted);
            if (!(gadgets.get(wanted) instanceof TickingGadget ticking)) continue;
            try {
                ticking.tick(player, chosen);
            } catch (Exception e) {
                Bukkit.getLogger().warning("The gadget " + chosen.getId() + " failed: " + e.getMessage());
            }
        }
    }

    /**
     * Tells one gadget to take its things back from one player.
     *
     * @param player who
     * @param id     the gadget, as a key
     */
    private static void stop(Player player, String id) {
        active.remove(player.getUniqueId());
        Gadget gadget = gadgets.get(id);
        if (gadget == null) return;
        try {
            // the item first, and for every gadget rather than only the ones that spawn something: it is
            // the same promise in both cases, that nothing of a gadget outlives wearing it
            GadgetItems.take(player, gadget.getId());
            gadget.cleanUp(player);
        } catch (Exception e) {
            Bukkit.getLogger().warning("The gadget " + id + " left something behind: " + e.getMessage());
        }
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

    /**
     * Somebody logging off is the one moment the loop cannot catch: they are gone from the online list
     * before the next step runs, and whatever their gadget spawned would stay behind.
     */
    private static class QuitListener implements Listener {

        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            String id = active.get(event.getPlayer().getUniqueId());
            if (id != null) stop(event.getPlayer(), id);
        }
    }
}

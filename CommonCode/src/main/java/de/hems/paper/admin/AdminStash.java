package de.hems.paper.admin;

import de.hems.paper.PaperContext;
import de.hems.types.admin.StashData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The chest {@code /admin} opens.
 * <p>
 * This is the other end of the drag and drop in the web interface: an admin pulling an item out of a
 * player's inventory in the browser drops it here, and picks it up in game out of this chest.
 * <p>
 * Only one live inventory exists per server while it is open, so two admins standing in it see each other
 * move things. It is written back to the launcher once the last of them closes it.
 */
public final class AdminStash implements Listener {

    /** The permission that opens the stash, on top of being an operator. */
    public static final String PERMISSION = "mcserver.adminstash";

    private static AdminStash instance;

    private final Plugin plugin;
    private Inventory inventory;
    private long revision;
    private final Set<UUID> viewers = new HashSet<>();

    private AdminStash(Plugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Sets the stash up once for a plugin.
     *
     * @param plugin the plugin it belongs to
     */
    public static synchronized void init(Plugin plugin) {
        if (instance != null) return;
        PaperContext.setPlugin(plugin);
        instance = new AdminStash(plugin);
    }

    public static AdminStash getInstance() {
        return instance;
    }

    /**
     * @param player the player to check
     * @return whether they may open the stash
     */
    public static boolean mayOpen(Player player) {
        return player.isOp() || player.hasPermission(PERMISSION);
    }

    /**
     * Opens the stash for an admin, fetching it from the launcher first if nobody has it open.
     *
     * @param player the admin
     */
    public void open(Player player) {
        if (!mayOpen(player)) {
            player.sendMessage(ChatColor.RED + "❌ Die Admin-Ablage ist nur für Admins.");
            return;
        }
        if (inventory != null) {
            viewers.add(player.getUniqueId());
            player.openInventory(inventory);
            return;
        }
        player.sendMessage(ChatColor.GRAY + "Ablage wird geladen ...");
        StashService.loadAsync(StashData.GLOBAL, stash -> {
            if (!player.isOnline()) return;
            if (stash == null) {
                player.sendMessage(ChatColor.RED
                        + "❌ Die Ablage konnte nicht geladen werden. Läuft der Hauptserver?");
                return;
            }
            if (inventory == null) build(stash);
            viewers.add(player.getUniqueId());
            player.openInventory(inventory);
        });
    }

    /**
     * Builds the live inventory from what the launcher sent.
     *
     * @param stash the stash as it is stored
     */
    private void build(StashData stash) {
        int size = Math.min(54, Math.max(9, ((stash.getSize() + 8) / 9) * 9));
        // never drop an item just because the stored size shrank - grow the window to fit
        for (de.hems.types.admin.ItemData item : stash.getItems()) {
            if (item.getSlot() >= size) size = Math.min(54, ((item.getSlot() + 9) / 9) * 9);
        }
        Holder holder = new Holder();
        inventory = Bukkit.createInventory(holder, size, ChatColor.DARK_RED + "Admin-Ablage");
        holder.inventory = inventory;
        inventory.setContents(StashService.toItems(stash, size));
        revision = stash.getRevision();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (inventory == null || !(event.getInventory().getHolder() instanceof Holder)) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        viewers.remove(player.getUniqueId());
        if (!viewers.isEmpty()) return;
        persist(player);
    }

    /**
     * Sends the stash to the launcher and lets go of the live inventory.
     *
     * @param editor who to tell if it failed, may be {@code null}
     */
    private void persist(Player editor) {
        Inventory closing = inventory;
        long openedAt = revision;
        inventory = null;
        if (closing == null) return;
        StashData data = new StashData(StashData.GLOBAL, closing.getSize(),
                StashService.toItemData(closing.getContents()), openedAt);
        String who = editor == null ? "server" : editor.getName();
        StashService.saveAsync(data, who, result -> {
            if (result.successful()) return;
            plugin.getLogger().warning("Could not save the admin stash: " + result.message());
            if (editor != null && editor.isOnline()) {
                editor.sendMessage(ChatColor.RED + "❌ " + result.message());
            }
        });
    }

    /**
     * Writes the stash back while the server shuts down. Blocks on purpose - the alternative is losing
     * whatever an admin left in it.
     */
    public void saveOnShutdown() {
        if (inventory == null) return;
        StashData data = new StashData(StashData.GLOBAL, inventory.getSize(),
                StashService.toItemData(inventory.getContents()), revision);
        StashService.Result result = StashService.saveBlocking(data, "shutdown");
        if (!result.successful()) {
            plugin.getLogger().warning("Could not save the admin stash on shutdown: " + result.message());
        }
        inventory = null;
        viewers.clear();
    }

    /**
     * @return whether somebody has the stash open on this server
     */
    public boolean isOpen() {
        return inventory != null;
    }

    /**
     * Closes the window for everybody, used when the launcher's copy changed underneath.
     */
    public void closeForAll() {
        if (inventory == null) return;
        for (HumanEntity viewer : new HashSet<>(inventory.getViewers())) viewer.closeInventory();
    }

    /**
     * Marks the stash window, so it is recognised by what it is rather than by its title.
     */
    private static final class Holder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}

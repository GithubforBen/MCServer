package de.schnorrenbergers.survival.featrues.adminjoin;

import com.destroystokyo.paper.profile.PlayerProfile;
import de.hems.paper.admin.AdminStash;
import de.hems.paper.admin.ItemCodec;
import de.hems.paper.admin.StashService;
import de.hems.types.admin.StashData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code /admin join} - an admin steps out of their own name and into the admin's.
 * <p>
 * The idea is that nobody can tell who is standing there. So the chat is told they left, the name over
 * their head, in the tab list and in chat becomes the admin's, and the skin with it. Typing it again
 * turns it around: the admin leaves, the player joins, and their own things are back in their hands.
 * <p>
 * What they carry while they are the admin is the admin stash - the same chest the website drops things
 * into. It is <em>taken</em> rather than copied: the stash is emptied on the launcher the moment somebody
 * puts it in their pockets and written back when they take it off. Copying it would mean two admins in
 * disguise carrying the same diamonds, which on a survival server is a duplication bug with a command in
 * front of it.
 * <p>
 * The disguise is a name and a skin and nothing else. The uuid does not change, so money, teams,
 * cosmetics and bans all still know who this is, and the admin abuse log is told the real name on
 * purpose - a log that reads "Admin" for everybody explains nothing after the fact.
 */
public final class AdminJoinService implements Listener {

    /** Where the disguises and the inventories behind them are kept, next to the plugin's other state. */
    private static final String FILE = "admin-join.yml";
    /** How many slots of a player's inventory can hold something: the stash has to fit in these. */
    private static final int STORAGE = 36;

    private static AdminJoinService instance;

    private final Plugin plugin;
    private final File file;
    private final YamlConfiguration config;
    /** Who is in disguise right now, and what they are really called. */
    private final Map<UUID, String> disguised = new ConcurrentHashMap<>();
    /** The profile each of them logged in with, to be handed back when they step out again. */
    private final Map<UUID, PlayerProfile> realProfiles = new ConcurrentHashMap<>();
    /** Who is in the middle of a swap, so a second click does not run it twice. */
    private final Set<UUID> busy = ConcurrentHashMap.newKeySet();

    private AdminJoinService(Plugin plugin) {
        this.plugin = plugin;
        // deliberately not ./configs/admin-join.yml: that one is the two settings and belongs to
        // AdminIdentity, and two objects writing one file overwrite each other's half of it
        this.file = new File(plugin.getDataFolder(), FILE);
        this.config = YamlConfiguration.loadConfiguration(file);
        readDisguised();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Sets the command up once for a plugin.
     *
     * @param plugin the plugin it belongs to
     */
    public static synchronized void init(Plugin plugin) {
        if (instance != null) return;
        instance = new AdminJoinService(plugin);
        AdminIdentity.init(plugin);
    }

    public static @Nullable AdminJoinService getInstance() {
        return instance;
    }

    /* ------------------------------------------------------------------ what other code asks */

    /**
     * @param player somebody
     * @return what they are really called, whatever they look like right now
     */
    public static String realName(Player player) {
        if (player == null) return "?";
        if (instance == null) return player.getName();
        String real = instance.disguised.get(player.getUniqueId());
        return real == null ? player.getName() : real;
    }

    /**
     * @return the real name of whoever is carrying the stash, or {@code null} when it is where it belongs
     */
    public static @Nullable String stashHolder() {
        if (instance == null) return null;
        for (String holder : instance.disguised.values()) return holder;
        return null;
    }

    /* ------------------------------------------------------------------ the command */

    /**
     * Steps in or out, whichever is next.
     *
     * @param player the admin
     */
    public void toggle(Player player) {
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "❌ Dieser Befehl ist nur für Admins.");
            return;
        }
        if (!busy.add(player.getUniqueId())) {
            player.sendMessage(ChatColor.GRAY + "Einen Moment - der letzte Wechsel läuft noch.");
            return;
        }
        if (disguised.containsKey(player.getUniqueId())) {
            leave(player);
        } else {
            enter(player);
        }
    }

    /**
     * Takes the stash out of the launcher and puts it into an admin's hands, then hides who they are.
     */
    private void enter(Player player) {
        if (!AdminIdentity.isReady()) {
            done(player, ChatColor.RED + "❌ Der Admin-Skin von " + AdminIdentity.getSkinAccount()
                    + " ist nicht geladen - ohne ihn wäre die Tarnung keine.");
            return;
        }
        AdminStash stash = AdminStash.getInstance();
        if (stash != null && stash.isOpen()) {
            done(player, ChatColor.RED + "❌ Die Ablage hat gerade jemand offen. Erst zumachen.");
            return;
        }
        String holder = stashHolder();
        if (holder != null) {
            done(player, ChatColor.RED + "❌ " + holder + " trägt die Ablage gerade.");
            return;
        }
        player.sendMessage(ChatColor.GRAY + "Ablage wird geholt ...");
        StashService.loadAsync(StashData.GLOBAL, data -> {
            if (!player.isOnline()) {
                busy.remove(player.getUniqueId());
                return;
            }
            if (data == null) {
                done(player, ChatColor.RED + "❌ Die Ablage ist nicht erreichbar. Läuft der Hauptserver?");
                return;
            }
            if (data.getItems().size() > STORAGE) {
                done(player, ChatColor.RED + "❌ In der Ablage liegen " + data.getItems().size()
                        + " Stapel, ins Inventar passen " + STORAGE + ". Räum sie erst auf.");
                return;
            }
            // emptied on the launcher before anything is handed over: from here on the items exist once,
            // in this admin's inventory, and a crash in the next second loses nothing that was not theirs
            StashData emptied = new StashData(StashData.GLOBAL, data.getSize(), List.of(),
                    data.getRevision());
            StashService.saveAsync(emptied, player.getName(), result -> {
                if (!result.successful()) {
                    done(player, ChatColor.RED + "❌ " + result.message());
                    return;
                }
                // the launcher's copy is empty by now, so somebody who logged off in this one round trip
                // would take the whole stash with them into nothing. Put it back instead
                if (!player.isOnline()) {
                    busy.remove(player.getUniqueId());
                    giveBack(data, player.getName());
                    return;
                }
                dress(player, data);
            });
        });
    }

    /**
     * The swap itself: their things away, the stash into their hands, the name and skin over the top.
     */
    private void dress(Player player, StashData stash) {
        String realName = player.getName();
        store(player, realName);
        realProfiles.put(player.getUniqueId(), player.getPlayerProfile().clone());
        disguised.put(player.getUniqueId(), realName);

        player.getInventory().setContents(new ItemStack[player.getInventory().getSize()]);
        player.getInventory().setStorageContents(packed(stash));
        AdminIdentity.wear(player);

        announceQuit(realName);
        announceJoin(AdminIdentity.getDisplayName());
        player.sendMessage(ChatColor.GOLD + "Du bist jetzt " + AdminIdentity.getDisplayName()
                + ". Du trägst die Ablage - nochmal /admin join legt beides zurück.");
        plugin.getLogger().info(realName + " is now disguised as " + AdminIdentity.getDisplayName() + ".");
        busy.remove(player.getUniqueId());
    }

    /**
     * Writes the stash back and gives an admin their own things and their own name.
     */
    private void leave(Player player) {
        player.sendMessage(ChatColor.GRAY + "Ablage wird zurückgelegt ...");
        StashService.loadAsync(StashData.GLOBAL, current -> {
            if (!player.isOnline()) {
                busy.remove(player.getUniqueId());
                return;
            }
            if (current == null) {
                done(player, ChatColor.RED
                        + "❌ Die Ablage ist nicht erreichbar - du bleibst so lange Admin.");
                return;
            }
            // read and taken away in the same tick: the write that follows takes a moment, and anything
            // picked up in that moment would be quietly overwritten by the inventory coming back
            ItemStack[] carried = player.getInventory().getStorageContents();
            player.getInventory().setStorageContents(new ItemStack[STORAGE]);

            int size = Math.max(current.getSize(), STORAGE);
            StashData back = new StashData(StashData.GLOBAL, size, ItemCodec.toData(carried),
                    current.getRevision());
            StashService.saveAsync(back, realName(player), result -> {
                if (result.successful()) {
                    undress(player);
                    return;
                }
                // nothing was stored, so the stash goes back into the hands it came out of rather than
                // nowhere - they stay the admin and can try again
                if (player.isOnline()) player.getInventory().setStorageContents(carried);
                done(player, ChatColor.RED + "❌ " + result.message()
                        + " Du bleibst Admin, die Sachen behältst du.");
            });
        });
    }

    /**
     * The stash, squeezed into the front of an inventory.
     * <p>
     * Slots are not kept: a stash window has up to fifty-four of them and an inventory thirty-six, so an
     * item lying in the last row of a big stash would land nowhere at all. They come back in the same
     * order, in the first free slots, and go back to the launcher that way.
     *
     * @param stash the stash as it was read
     * @return the items, in the first slots of a player's inventory
     */
    private static ItemStack[] packed(StashData stash) {
        ItemStack[] contents = new ItemStack[STORAGE];
        List<de.hems.types.admin.ItemData> items = new ArrayList<>(stash.getItems());
        items.sort(java.util.Comparator.comparingInt(de.hems.types.admin.ItemData::getSlot));
        int next = 0;
        for (de.hems.types.admin.ItemData data : items) {
            ItemStack item = ItemCodec.toItem(data);
            if (item == null || next >= STORAGE) continue;
            contents[next++] = item;
        }
        return contents;
    }

    /**
     * Puts the stash back on the launcher when nobody could take it after all.
     *
     * @param stash    the stash as it was read
     * @param editor   who the attempt was for
     */
    private void giveBack(StashData stash, String editor) {
        StashService.saveAsync(new StashData(StashData.GLOBAL, stash.getSize(), stash.getItems(),
                stash.getRevision() + 1), editor, result -> {
            if (result.successful()) return;
            plugin.getLogger().severe("The admin stash was taken out for " + editor
                    + " and could not be put back: " + result.message());
        });
    }

    private void undress(Player player) {
        UUID id = player.getUniqueId();
        String realName = disguised.remove(id);
        PlayerProfile original = realProfiles.remove(id);
        player.getInventory().setContents(restore(player));
        clear(id);
        if (original != null) {
            AdminIdentity.take(player, original);
        } else {
            // only reachable if the disguise was put on by a build that is no longer running: say it
            // rather than leave somebody believing they look like themselves
            player.sendMessage(ChatColor.RED
                    + "❌ Dein eigenes Aussehen ist hier nicht bekannt - einmal neu verbinden.");
        }

        announceQuit(AdminIdentity.getDisplayName());
        announceJoin(realName == null ? player.getName() : realName);
        player.sendMessage(ChatColor.GOLD + "Du bist wieder du. Die Ablage liegt zurück beim Hauptserver.");
        plugin.getLogger().info((realName == null ? player.getName() : realName)
                + " is no longer disguised.");
        busy.remove(id);
    }

    /* ------------------------------------------------------------------ staying disguised */

    /**
     * Somebody who was in disguise when the server stopped comes back in disguise.
     * <p>
     * At {@code HIGHEST}, because the join message is set by the join listener and has to be overwritten
     * rather than raced: the name in it is the one thing that would give the whole disguise away.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!disguised.containsKey(player.getUniqueId())) return;
        realProfiles.put(player.getUniqueId(), player.getPlayerProfile().clone());
        if (!AdminIdentity.isReady()) {
            // the skin is not there yet, so the disguise cannot be put back on. Saying so is the only
            // honest option - the admin would otherwise walk around under their own face believing not to
            player.sendMessage(ChatColor.RED
                    + "❌ Der Admin-Skin fehlt - du siehst gerade aus wie du selbst.");
            return;
        }
        AdminIdentity.wear(player);
        event.joinMessage(joinLine(AdminIdentity.getDisplayName()));
        player.sendMessage(ChatColor.GOLD + "Du bist weiterhin " + AdminIdentity.getDisplayName()
                + " und trägst die Ablage.");
    }

    /**
     * An admin in disguise is carrying the network's stash, so dying must not scatter it across a cave.
     * <p>
     * Only for them and only while they are in disguise: this is not a rule about admins, it is a rule
     * about the chest they happen to have in their pockets.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        if (!disguised.containsKey(event.getEntity().getUniqueId())) return;
        event.setKeepInventory(true);
        event.getDrops().clear();
    }

    /* ------------------------------------------------------------------ the two inventories */

    /**
     * Puts an admin's own inventory away, on disk rather than in memory: a server that goes down while
     * somebody is in disguise must not be a server that ate their things.
     */
    private void store(Player player, String realName) {
        String path = "players." + player.getUniqueId();
        config.set(path + ".real-name", realName);
        config.set(path + ".inventory", Arrays.asList(player.getInventory().getContents()));
        save();
    }

    /**
     * @param player an admin stepping out of the disguise
     * @return the inventory they had before they stepped in, sized for their inventory
     */
    private ItemStack[] restore(Player player) {
        int size = player.getInventory().getSize();
        ItemStack[] contents = new ItemStack[size];
        List<?> stored = config.getList("players." + player.getUniqueId() + ".inventory");
        if (stored == null) return contents;
        for (int slot = 0; slot < stored.size() && slot < size; slot++) {
            if (stored.get(slot) instanceof ItemStack item) contents[slot] = item;
        }
        return contents;
    }

    private void clear(UUID id) {
        config.set("players." + id, null);
        save();
    }

    private void readDisguised() {
        if (!config.isConfigurationSection("players")) return;
        for (String key : config.getConfigurationSection("players").getKeys(false)) {
            try {
                disguised.put(UUID.fromString(key),
                        config.getString("players." + key + ".real-name", key));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("admin-join.yml holds something that is not a uuid: " + key);
            }
        }
    }

    private void save() {
        try {
            file.getParentFile().mkdirs();
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save admin-join.yml: " + e.getMessage());
        }
    }

    /* ------------------------------------------------------------------ the chat */

    private void announceJoin(String name) {
        Bukkit.broadcast(joinLine(name));
    }

    private void announceQuit(String name) {
        Bukkit.broadcast(Component.text("<<" + name, NamedTextColor.RED));
    }

    /**
     * @param name whoever is arriving
     * @return the line the join listener would have written, so the two are not told apart
     */
    private static Component joinLine(String name) {
        return Component.text(">>" + name, NamedTextColor.GREEN);
    }

    /**
     * Ends a swap that did not happen.
     *
     * @param player  the admin
     * @param message what went wrong
     */
    private void done(Player player, String message) {
        busy.remove(player.getUniqueId());
        player.sendMessage(message);
    }

    /**
     * @return the admins in disguise, by their real names, for a status line
     */
    public List<String> disguisedNames() {
        return new ArrayList<>(new HashSet<>(disguised.values()));
    }
}

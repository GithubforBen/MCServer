package de.schnorrenbergers.backpack;

import de.hems.paper.PayingPlayers;
import de.hems.paper.team.BackpackService;
import de.hems.paper.team.TeamService;
import de.hems.types.team.BackpackData;
import de.hems.types.team.TeamData;
import de.hems.types.team.TeamSettings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs the shared backpacks.
 * <p>
 * A team's backpack lives on the launcher, so it is the same one on every server. While it is open on this
 * server it is held here as a single live inventory that every viewer of that team shares - two members
 * looking at it at the same time see each other move things, which is what people expect from a shared
 * chest. It is written back once the last of them closes it.
 */
public class BackpackManager {

    private final BackpackPlugin plugin;
    private final BackpackSettings settings;
    /** The backpacks that are open right now, keyed by the lower case team name. */
    private final Map<String, Open> open = new ConcurrentHashMap<>();

    public BackpackManager(BackpackPlugin plugin, BackpackSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    /**
     * One backpack that is currently open on this server.
     */
    private static final class Open {
        private final Inventory inventory;
        private final String teamName;
        /** The revision it was loaded at, sent back so a stale write can be refused. */
        private long revision;
        private final Set<UUID> viewers = new HashSet<>();

        private Open(Inventory inventory, String teamName, long revision) {
            this.inventory = inventory;
            this.teamName = teamName;
            this.revision = revision;
        }
    }

    /**
     * Opens the backpack of a player's team.
     * <p>
     * The contents are fetched from the launcher off the main thread, so a slow answer never freezes the
     * server - the window opens when the data has arrived.
     *
     * @param player the player asking for it
     */
    public void open(Player player) {
        TeamData team = TeamService.getTeamOf(player.getUniqueId());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "❌ Du bist in keinem Team - der Rucksack gehört dem Team.");
            return;
        }
        if (!team.getSettings().getFlag(TeamSettings.Key.BACKPACK_ENABLED)) {
            player.sendMessage(ChatColor.RED + "❌ Der Rucksack ist für dein Team abgeschaltet.");
            return;
        }

        Open already = open.get(key(team.getName()));
        if (already != null) {
            joinExisting(player, team, already);
            return;
        }

        int wanted = settings.sizeFor(TeamService.isMajorityPaying(team));
        player.sendMessage(ChatColor.GRAY + "Rucksack wird geladen ...");
        BackpackService.loadAsync(team, backpack -> {
            if (!player.isOnline()) return;
            if (backpack == null) {
                player.sendMessage(ChatColor.RED
                        + "❌ Der Rucksack konnte nicht geladen werden. Läuft der Hauptserver?");
                return;
            }
            // somebody else may have opened it while we were waiting for the answer
            Open existing = open.get(key(team.getName()));
            if (existing != null) {
                joinExisting(player, team, existing);
                return;
            }
            Open created = create(team, backpack, wanted);
            open.put(key(team.getName()), created);
            show(player, team, created, wanted);
        });
    }

    /**
     * Lets a player look at a backpack that is already open.
     */
    private void joinExisting(Player player, TeamData team, Open existing) {
        existing.viewers.add(player.getUniqueId());
        player.openInventory(existing.inventory);
        announceSize(player, team, existing.inventory.getSize());
    }

    /**
     * Builds the live inventory from what the launcher sent.
     */
    private Open create(TeamData team, BackpackData backpack, int wantedSize) {
        int size = wantedSize;
        ItemStack[] items = BackpackService.toItems(backpack);
        // never drop items just because the team shrank - keep the bigger size until they fit again
        if (items.length > size) size = items.length;
        size = Math.min(54, ((size + 8) / 9) * 9);

        BackpackHolder holder = new BackpackHolder(team.getName());
        Inventory inventory = Bukkit.createInventory(holder, size, settings.titleFor(team.getName()));
        holder.setInventory(inventory);
        for (int slot = 0; slot < items.length && slot < size; slot++) {
            inventory.setItem(slot, items[slot]);
        }
        return new Open(inventory, team.getName(), backpack.getRevision());
    }

    private void show(Player player, TeamData team, Open backpack, int wantedSize) {
        backpack.viewers.add(player.getUniqueId());
        player.openInventory(backpack.inventory);
        announceSize(player, team, wantedSize);
    }

    /**
     * Tells the player how big their backpack is and what would make it bigger.
     */
    private void announceSize(Player player, TeamData team, int size) {
        if (!settings.isAnnounceSize()) return;
        boolean majority = TeamService.isMajorityPaying(team);
        int paying = PayingPlayers.countPaying(team.getMembers());
        int total = team.getMembers().size();
        if (majority) {
            player.sendMessage(ChatColor.GREEN + "✓ Großer Rucksack (" + size + " Slots) - "
                    + paying + " von " + total + " Mitgliedern unterstützen den Server.");
            return;
        }
        int needed = total / 2 + 1 - paying;
        player.sendMessage(ChatColor.GRAY + "Rucksack mit " + size + " Slots. "
                + (needed > 0
                ? "Noch " + needed + " zahlende" + (needed == 1 ? "s Mitglied" : " Mitglieder")
                + ", dann wird er doppelt so groß."
                : "Mit einer zahlenden Mehrheit wird er doppelt so groß."));
    }

    /**
     * Handles a player closing the window. The backpack is written back once the last viewer is gone.
     *
     * @param teamName the team the window belonged to
     * @param player   the player that closed it
     */
    public void onClose(String teamName, Player player) {
        Open backpack = open.get(key(teamName));
        if (backpack == null) return;
        backpack.viewers.remove(player.getUniqueId());
        // getViewers still contains the closing player at this point, so count what is left ourselves
        if (!backpack.viewers.isEmpty()) return;
        open.remove(key(teamName));
        persist(backpack, player);
    }

    /**
     * Sends a backpack to the launcher.
     *
     * @param backpack the backpack that was closed
     * @param player   who to tell if it did not work, may be {@code null}
     */
    private void persist(Open backpack, Player player) {
        BackpackData data = new BackpackData(backpack.teamName, backpack.inventory.getSize(),
                BackpackService.toBytes(backpack.inventory.getContents()), backpack.revision);
        BackpackService.saveAsync(data, result -> {
            if (result.successful()) return;
            plugin.getLogger().warning("Could not save the backpack of " + backpack.teamName
                    + ": " + result.message());
            if (player != null && player.isOnline()) {
                player.sendMessage(ChatColor.RED + "❌ " + result.message());
            }
        });
    }

    /**
     * Writes every open backpack back while the server shuts down. Blocks on purpose - the alternative is
     * losing whatever is in them.
     */
    public void saveAllBlocking() {
        for (Open backpack : open.values()) {
            BackpackData data = new BackpackData(backpack.teamName, backpack.inventory.getSize(),
                    BackpackService.toBytes(backpack.inventory.getContents()), backpack.revision);
            BackpackService.Result result = BackpackService.saveBlocking(data);
            if (!result.successful()) {
                plugin.getLogger().warning("Could not save the backpack of " + backpack.teamName
                        + " on shutdown: " + result.message());
            }
        }
        open.clear();
    }

    /**
     * @param teamName the team whose backpack is open
     * @return whether anybody is looking at it right now
     */
    public boolean isOpen(String teamName) {
        return open.containsKey(key(teamName));
    }

    private static String key(String teamName) {
        return teamName.toLowerCase(Locale.ROOT);
    }
}

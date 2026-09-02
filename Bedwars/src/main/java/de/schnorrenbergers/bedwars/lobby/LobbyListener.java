package de.schnorrenbergers.bedwars.lobby;

import de.hems.api.ItemApi;
import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.admin.AdminMenu;
import de.schnorrenbergers.bedwars.game.Equipment;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.map.MapPoint;
import de.schnorrenbergers.bedwars.round.RoundAdminMenu;
import de.schnorrenbergers.bedwars.round.RoundContext;
import de.schnorrenbergers.bedwars.round.RoundStateListener;
import de.schnorrenbergers.bedwars.scoreboard.Sidebar;
import de.schnorrenbergers.bedwars.util.Messages;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * The waiting lobby: who is in, what they may do, and the wool that opens the team menu.
 * <p>
 * Everything here asks the game which phase it is in rather than keeping a flag of its own, and everything
 * steps aside in setup mode - somebody building a map has to be able to break blocks on it.
 */
public class LobbyListener implements Listener {

    /** Marks the item that opens the team menu, so it is recognised however it was renamed. */
    private static final NamespacedKey ITEM_KEY = new NamespacedKey("bedwars", "lobby-item");
    private static final String TEAM_ITEM = "team";
    /** Marks the item that opens the server settings, which only an admin is given. */
    private static final String SETTINGS_ITEM = "settings";
    /** Where the wool sits in the hotbar. */
    private static final int TEAM_ITEM_SLOT = 0;
    /** And where the settings sit, at the other end of it. */
    private static final int SETTINGS_ITEM_SLOT = 8;

    private final Plugin plugin;

    public LobbyListener(Plugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ---------------------------------------------------------- coming and going

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        Game game = game();
        Player player = event.getPlayer();
        if (game == null || game.isSetupMode()) return;

        if (RoundContext.isKicked(player) || !RoundContext.mayJoin(player)) {
            // thrown out of this round, or never invited into a private one. Either way they go back, a
            // tick later, because the proxy channel is not usable in the join event itself
            boolean uninvited = !RoundContext.isKicked(player);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                if (uninvited) {
                    player.sendMessage(net.kyori.adventure.text.Component.text(
                            "Diese Runde ist privat - du musst eingeladen werden.",
                            net.kyori.adventure.text.format.NamedTextColor.RED));
                }
                RoundContext.kick(player);
            });
            return;
        }
        GamePlayer joined = game.join(player);
        Sidebar.apply(player, game);
        reportRound(game, game.getOnlineCount());
        if (game.isWaiting()) {
            prepareForLobby(player);
            Messages.broadcast("lobby.joined",
                    "player", player.getName(),
                    "online", String.valueOf(game.getOnlineCount()),
                    "maximum", String.valueOf(game.getMaximumPlayers()));
            if (game.getArena() == null) Messages.send(player, "lobby.no-map");
            return;
        }
        // the round is under way: phase 3 puts a returning player back into it, anybody else watches
        if (!joined.isPlaying()) makeSpectator(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Game game = game();
        if (game == null || !game.isWaiting()) return;
        game.forget(event.getPlayer().getUniqueId());
        reportRound(game, Math.max(0, game.getOnlineCount() - 1));
        Messages.broadcast("lobby.left",
                "player", event.getPlayer().getName(),
                "online", String.valueOf(Math.max(0, game.getOnlineCount() - 1)),
                "maximum", String.valueOf(game.getMaximumPlayers()));
    }

    /**
     * Keeps the lobby's round list current on how many people are in here.
     *
     * @param game   the round
     * @param online how many are on it
     */
    private static void reportRound(Game game, int online) {
        RoundContext.report(RoundStateListener.stateOf(game.getPhaseType()), online);
    }

    /**
     * Puts a player into the state the waiting lobby wants: nothing in the pockets but the team wool, and
     * nothing to do but wait.
     *
     * @param player who joined
     */
    private void prepareForLobby(Player player) {
        Game game = game();
        Location lobby = lobbyLocation();
        player.setGameMode(GameMode.ADVENTURE);
        Equipment.reset(player, lobby);
        player.getInventory().setItem(TEAM_ITEM_SLOT, teamItem());
        // the switches within reach of the person running the round, rather than only through a command:
        // the settings that matter - 1.8 combat, the automatic start - are decided while everybody is
        // standing in the lobby waiting, which is exactly when a command is the most awkward way to do it
        if (mayAdminister(player)) player.getInventory().setItem(SETTINGS_ITEM_SLOT, settingsItem());
        if (game != null && game.getMode().getTeamCount() > 0) player.getInventory().setHeldItemSlot(TEAM_ITEM_SLOT);
    }

    private void makeSpectator(Player player) {
        player.setGameMode(GameMode.SPECTATOR);
        Location spot = lobbyLocation();
        if (spot != null) player.teleport(spot);
    }

    /**
     * @return where the waiting lobby of the map is, or {@code null} when there is no map yet
     */
    private Location lobbyLocation() {
        Game game = game();
        if (game == null || game.getArena() == null || game.getWorld() == null) return null;
        MapPoint lobby = game.getArena().getLobby();
        return lobby == null ? game.getWorld().getSpawnLocation() : lobby.toLocation(game.getWorld());
    }

    /**
     * @return the wool that opens the team menu
     */
    private static ItemStack teamItem() {
        ItemStack item = new ItemApi(Material.WHITE_WOOL,
                legacy(Messages.get("lobby.select.item")), lore("lobby.select.lore")).build();
        item.editMeta(meta -> meta.getPersistentDataContainer().set(ITEM_KEY, PersistentDataType.STRING, TEAM_ITEM));
        return item;
    }

    /**
     * @param component a message
     * @return it in the old colour codes, which is what {@link ItemApi} takes
     */
    private static String legacy(net.kyori.adventure.text.Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    /**
     * Reads a tooltip out of {@code messages.yml}.
     * <p>
     * One key with pipes in it rather than a key per line: the number of lines is part of the wording, and
     * splitting it across {@code lore-1}, {@code lore-2} and {@code lore-3} makes rewording it a change to
     * the code rather than to the file.
     *
     * @param key the message
     * @return its lines
     */
    private static java.util.List<String> lore(String key) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        for (String line : Messages.raw(key).split("\\|")) {
            lines.add(legacy(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(line)));
        }
        return lines;
    }

    /**
     * @return the comparator that opens the server settings
     */
    private static ItemStack settingsItem() {
        ItemStack item = new ItemApi(Material.COMPARATOR,
                legacy(Messages.get("lobby.settings-item")), lore("lobby.settings-lore")).build();
        item.editMeta(meta -> meta.getPersistentDataContainer()
                .set(ITEM_KEY, PersistentDataType.STRING, SETTINGS_ITEM));
        return item;
    }

    /**
     * @param player who is asking
     * @return whether they are allowed to change how the round is played - a real admin, or the player
     *         who put this round up in the first place
     */
    private static boolean mayAdminister(Player player) {
        return RoundContext.mayAdminister(player);
    }

    // ------------------------------------------------------------------ the wool

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Game game = game();
        if (game == null || !game.isWaiting()) return;
        ItemStack item = event.getItem();
        if (item == null || item.getItemMeta() == null) return;
        String kind = item.getItemMeta().getPersistentDataContainer()
                .get(ITEM_KEY, PersistentDataType.STRING);
        if (kind == null) return;
        event.setCancelled(true);
        if (TEAM_ITEM.equals(kind)) {
            TeamSelectMenu.open(event.getPlayer());
        } else if (SETTINGS_ITEM.equals(kind) && mayAdminister(event.getPlayer())) {
            // a round somebody started has more to it than switches: who may come in, when it begins,
            // and who has to leave. Without a round behind it there is nothing to run, only settings
            if (RoundContext.exists()) {
                RoundAdminMenu.open(event.getPlayer());
            } else {
                AdminMenu.open(event.getPlayer());
            }
        }
    }

    // ---------------------------------------------------------------- protection

    /**
     * @param player who is doing something
     * @return whether the lobby should stay out of their way: they are building the map, or they are in
     *         creative and therefore an operator who means it
     */
    private boolean unprotected(Player player) {
        Game game = game();
        return game == null || game.isSetupMode() || player.getGameMode() == GameMode.CREATIVE;
    }

    /**
     * @return whether the round is not being played right now, so nobody should be able to fight or build
     */
    private boolean quiet() {
        Game game = game();
        return game == null || !game.isRunning();
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (unprotected(player) || !quiet()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (unprotected(player) || !quiet()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (unprotected(event.getPlayer()) || !quiet()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (unprotected(event.getPlayer()) || !quiet()) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (unprotected(event.getPlayer()) || !quiet()) return;
        event.setCancelled(true);
    }

    /**
     * Keeps the team wool where it is. Menus are their own inventories and are not affected.
     */
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (unprotected(player) || !quiet()) return;
        // nothing but the player's own inventory is open, so every click is at the team wool
        if (event.getInventory().getHolder() != null) {
            event.setCancelled(true);
            return;
        }
        // a menu is open: it decides what may be clicked in itself, the pockets underneath stay shut
        if (player.getInventory().equals(event.getClickedInventory())) {
            event.setCancelled(true);
        }
    }

    private static Game game() {
        Bedwars plugin = Bedwars.getInstance();
        return plugin == null ? null : plugin.getGame();
    }
}

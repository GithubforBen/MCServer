package de.hems.paper.admin;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.admin.ApplyInventoryEvent;
import de.hems.communication.events.admin.RequestInventoryEvent;
import de.hems.communication.events.admin.RequestPlayerActionEvent;
import de.hems.communication.events.admin.RequestMaterialsEvent;
import de.hems.communication.events.admin.RequestPlayersEvent;
import de.hems.communication.events.admin.RespondActionEvent;
import de.hems.communication.events.admin.RespondInventoryEvent;
import de.hems.communication.events.admin.RespondMaterialsEvent;
import de.hems.communication.events.admin.RespondPlayersEvent;
import de.hems.paper.PaperContext;
import de.hems.types.admin.InventoryData;
import de.hems.types.admin.ItemData;
import de.hems.types.admin.PlayerSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Answers everything the admin website wants to know about the players of this server.
 * <p>
 * The website lives in the launcher, which has no bukkit at all, so every question arrives as an event and
 * every answer goes back the same way. Reading and writing inventories touches live entities and therefore
 * has to happen on the main thread - that hop is made here, once, instead of in every caller.
 * <p>
 * Requests are broadcast to the whole network because the launcher does not track who is where. A server
 * that does not have the player in question answers with nothing, so the launcher can move on without
 * waiting for a timeout.
 */
public class PlayerAdminHandler {

    /** How long a main thread hop may take before the answer is given up on. */
    private static final long SYNC_TIMEOUT_MS = 5_000L;

    private static BackpackProvider backpackProvider;

    public PlayerAdminHandler(Plugin plugin) {
        PaperContext.setPlugin(plugin);
        ListenerAdapter.register(RequestPlayersEvent.class, event -> onPlayers((RequestPlayersEvent) event));
        ListenerAdapter.register(RequestInventoryEvent.class, event -> onInventory((RequestInventoryEvent) event));
        ListenerAdapter.register(ApplyInventoryEvent.class, event -> onApply((ApplyInventoryEvent) event));
        ListenerAdapter.register(RequestPlayerActionEvent.class, event -> onAction((RequestPlayerActionEvent) event));
        ListenerAdapter.register(RequestMaterialsEvent.class, event -> onMaterials((RequestMaterialsEvent) event));
        try {
            new CoreProtectLookupHandler();
        } catch (Throwable e) {
            // CoreProtect is only a soft dependency - without it the rest of the player manager still works
            plugin.getLogger().info("CoreProtect is not available, its lookups stay disabled.");
        }
    }

    /**
     * Registers where backpacks come from. Without one the website reports that there are none.
     *
     * @param provider the provider, or {@code null} to remove it
     */
    public static void setBackpackProvider(BackpackProvider provider) {
        backpackProvider = provider;
    }

    public static BackpackProvider getBackpackProvider() {
        return backpackProvider;
    }

    /* ------------------------------------------------------------------ players */

    private void onPlayers(RequestPlayersEvent request) throws Exception {
        ArrayList<PlayerSnapshot> players = onMainThread(() -> {
            ArrayList<PlayerSnapshot> collected = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) collected.add(snapshot(player));
            return collected;
        });
        ListenerAdapter.sendListeners(new RespondPlayersEvent(
                request.getSender(), players == null ? new ArrayList<>() : players,
                currentTps(), request.getEventId()));
    }

    /**
     * @return how well this server is keeping up, capped at the twenty ticks it can never beat
     */
    private static double currentTps() {
        try {
            double[] tps = Bukkit.getServer().getTPS();
            return tps.length == 0 ? 20.0d : Math.min(20.0d, tps[0]);
        } catch (RuntimeException e) {
            return 20.0d;
        }
    }

    /**
     * @param player the player to describe
     * @return everything the website shows about them
     */
    private static PlayerSnapshot snapshot(Player player) {
        PlayerSnapshot snapshot = new PlayerSnapshot();
        snapshot.setUuid(player.getUniqueId());
        snapshot.setName(player.getName());
        snapshot.setServer(String.valueOf(ListenerAdapter.getName()));
        snapshot.setOnline(true);
        snapshot.setHealth(player.getHealth());
        snapshot.setMaxHealth(maxHealthOf(player));
        snapshot.setFoodLevel(player.getFoodLevel());
        snapshot.setGameMode(player.getGameMode().name());
        snapshot.setLevel(player.getLevel());
        snapshot.setWorld(player.getWorld().getName());
        snapshot.setX(player.getLocation().getBlockX());
        snapshot.setY(player.getLocation().getBlockY());
        snapshot.setZ(player.getLocation().getBlockZ());
        snapshot.setFirstPlayed(player.getFirstPlayed());
        snapshot.setLastSeen(System.currentTimeMillis());
        snapshot.setOp(player.isOp());
        snapshot.setBanned(player.isBanned());
        snapshot.setViewDistance(player.getViewDistance());
        if (backpackProvider != null) {
            try {
                snapshot.setBackpacks(backpackProvider.listBackpacks(player.getUniqueId()));
            } catch (RuntimeException e) {
                // a broken backpack plugin must not take the whole player list with it
                snapshot.setBackpacks(new ArrayList<>());
            }
        }
        return snapshot;
    }

    /**
     * @param player the player to measure
     * @return how much health they can have, falling back to the vanilla twenty
     */
    private static double maxHealthOf(Player player) {
        try {
            var attribute = player.getAttribute(Attribute.MAX_HEALTH);
            if (attribute != null) return attribute.getValue();
        } catch (RuntimeException | NoSuchFieldError e) {
            // the attribute was renamed between versions - twenty is a safe answer
        }
        return 20.0d;
    }

    /**
     * Sends the materials of this server's registry, which is the only place a usable list exists.
     */
    private void onMaterials(RequestMaterialsEvent request) throws Exception {
        ArrayList<String> materials = new ArrayList<>();
        for (Material material : Material.values()) {
            if (!material.isItem() || material.isAir()) continue;
            materials.add(material.name());
        }
        ListenerAdapter.sendListeners(new RespondMaterialsEvent(
                request.getSender(), materials, request.getEventId()));
    }

    /* ------------------------------------------------------------------ inventories */

    private void onInventory(RequestInventoryEvent request) throws Exception {
        InventoryData data = onMainThread(() -> {
            Player player = Bukkit.getPlayer(request.getPlayerId());
            if (player == null) return null;
            return read(player, request.getKind(), request.getContainerId());
        });
        ListenerAdapter.sendListeners(new RespondInventoryEvent(
                request.getSender(), data, request.getEventId()));
    }

    /**
     * @param player      whose container to read
     * @param kind        which container
     * @param containerId which backpack, when {@code kind} is a backpack
     * @return its contents, or {@code null} if there is no such container
     */
    private static InventoryData read(Player player, InventoryData.Kind kind, String containerId) {
        ItemStack[] contents;
        int size;
        String title;
        switch (kind) {
            case ENDER_CHEST -> {
                Inventory enderChest = player.getEnderChest();
                contents = enderChest.getContents();
                size = enderChest.getSize();
                title = "Enderchest";
            }
            case BACKPACK -> {
                if (backpackProvider == null) return null;
                contents = backpackProvider.readBackpack(player.getUniqueId(), containerId);
                if (contents == null) return null;
                size = contents.length;
                title = "Backpack " + containerId;
            }
            default -> {
                contents = player.getInventory().getContents();
                size = contents.length;
                title = "Inventar";
            }
        }
        List<ItemData> items = ItemCodec.toData(contents);
        return new InventoryData(player.getUniqueId(), player.getName(), kind, containerId, title, size, items);
    }

    private void onApply(ApplyInventoryEvent request) throws Exception {
        InventoryData wanted = request.getInventory();
        if (wanted == null || wanted.getPlayerId() == null) {
            respondAction(request.getSender(), request.getEventId(), false, null);
            return;
        }
        String result = onMainThread(() -> {
            Player player = Bukkit.getPlayer(wanted.getPlayerId());
            if (player == null) return null;
            return write(player, wanted, request.getEditor());
        });
        respondAction(request.getSender(), request.getEventId(), result != null, result);
    }

    /**
     * Writes an edited container back onto the player.
     *
     * @param player the owner
     * @param wanted the container as the browser sent it
     * @param editor who made the change
     * @return what to report back, or {@code null} if it could not be written
     */
    private static String write(Player player, InventoryData wanted, String editor) {
        ItemStack[] contents = ItemCodec.toContents(wanted.getItems(), wanted.getSize());
        switch (wanted.getKind()) {
            case ENDER_CHEST -> player.getEnderChest().setContents(contents);
            case BACKPACK -> {
                if (backpackProvider == null) return null;
                if (!backpackProvider.writeBackpack(player.getUniqueId(), wanted.getContainerId(), contents)) {
                    return null;
                }
            }
            default -> player.getInventory().setContents(contents);
        }
        player.updateInventory();
        Bukkit.getLogger().info("[Admin] " + editor + " changed the "
                + wanted.getKind() + " of " + player.getName());
        return wanted.getKind() + " von " + player.getName() + " gespeichert.";
    }

    /* ------------------------------------------------------------------ actions */

    private void onAction(RequestPlayerActionEvent request) throws Exception {
        String result = onMainThread(() -> {
            Player player = Bukkit.getPlayer(request.getPlayerId());
            if (player == null) return null;
            return act(player, request);
        });
        respondAction(request.getSender(), request.getEventId(), result != null, result);
    }

    /**
     * @param player  who to act on
     * @param request what to do
     * @return what to report back, or {@code null} if the action did not apply
     */
    private static String act(Player player, RequestPlayerActionEvent request) {
        String argument = request.getArgument();
        switch (request.getAction()) {
            case KICK -> {
                String reason = argument == null || argument.isBlank() ? "Vom Admin gekickt." : argument;
                player.kickPlayer(reason);
                return player.getName() + " wurde gekickt.";
            }
            case HEAL -> {
                player.setHealth(maxHealthOf(player));
                return player.getName() + " wurde geheilt.";
            }
            case FEED -> {
                player.setFoodLevel(20);
                player.setSaturation(20f);
                return player.getName() + " wurde gesättigt.";
            }
            case CLEAR_INVENTORY -> {
                player.getInventory().clear();
                player.updateInventory();
                return "Inventar von " + player.getName() + " geleert.";
            }
            case SET_GAMEMODE -> {
                GameMode mode;
                try {
                    mode = GameMode.valueOf(String.valueOf(argument).toUpperCase());
                } catch (IllegalArgumentException e) {
                    return null;
                }
                player.setGameMode(mode);
                return player.getName() + " ist jetzt im Modus " + mode.name() + ".";
            }
            case SET_OP -> {
                boolean op = Boolean.parseBoolean(argument);
                player.setOp(op);
                return player.getName() + (op ? " ist jetzt OP." : " ist kein OP mehr.");
            }
            case SEND_MESSAGE -> {
                if (argument == null || argument.isBlank()) return null;
                player.sendMessage(argument);
                return "Nachricht an " + player.getName() + " geschickt.";
            }
            case TELEPORT_TO_SPAWN -> {
                player.teleport(player.getWorld().getSpawnLocation());
                return player.getName() + " wurde zum Spawn teleportiert.";
            }
            default -> {
                return null;
            }
        }
    }

    private static void respondAction(ListenerAdapter.ServerName receiver, UUID requestId,
                                      boolean successful, String message) throws Exception {
        ListenerAdapter.sendListeners(new RespondActionEvent(receiver, successful, message, requestId));
    }

    /* ------------------------------------------------------------------ threading */

    /**
     * Runs work on the main thread and waits for its result.
     * <p>
     * The caller is a jgroups receive thread, so blocking it is fine - blocking the main thread would not
     * be, which is exactly why the work is handed over instead of done here.
     *
     * @param work what to run
     * @return what it produced, or {@code null} if it failed or took too long
     */
    private static <T> T onMainThread(Callable<T> work) {
        if (!PaperContext.hasPlugin()) return null;
        try {
            return Bukkit.getScheduler().callSyncMethod(PaperContext.getPlugin(), work)
                    .get(SYNC_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            Bukkit.getLogger().warning("[Admin] Could not run the request on the main thread: " + e);
            return null;
        }
    }
}

package de.hems.paper.cosmetic;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.cosmetic.BuyCosmeticEvent;
import de.hems.communication.events.cosmetic.CosmeticUpdatedEvent;
import de.hems.communication.events.cosmetic.PlayerCosmeticsUpdatedEvent;
import de.hems.communication.events.cosmetic.RequestCosmeticsEvent;
import de.hems.communication.events.cosmetic.SaveCosmeticEvent;
import de.hems.communication.events.cosmetic.SelectCosmeticEvent;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.paper.PaperContext;
import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.CosmeticPurchase;
import de.hems.types.cosmetic.CosmeticSnapshot;
import de.hems.types.cosmetic.CosmeticType;
import de.hems.types.cosmetic.PlayerCosmetics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The cosmetics of the network, as seen from a game server.
 * <p>
 * Reads come out of a local copy, because the interesting one - "what does the winner have on" - is asked
 * in the same tick the round ends, and a network round trip there would be a pause in the middle of the
 * celebration. Writes go to the launcher, which is also where a purchase is decided, so the answer to
 * "did they pay" is never a guess.
 */
public final class CosmeticService {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final long REFRESH_INTERVAL_TICKS = 20L * 300L;
    private static final long STARTUP_RETRY_TICKS = 40L;

    private static final Map<String, CosmeticData> catalog = new ConcurrentHashMap<>();
    private static final Map<UUID, PlayerCosmetics> players = new ConcurrentHashMap<>();
    private static volatile boolean loaded = false;
    private static boolean initialized = false;

    private CosmeticService() {
    }

    /**
     * Starts keeping the local copy up to date.
     *
     * @param plugin the plugin the background work belongs to
     */
    public static synchronized void init(Plugin plugin) {
        if (initialized) return;
        initialized = true;
        PaperContext.setPlugin(plugin);
        ListenerAdapter.register(CosmeticUpdatedEvent.class, event -> {
            CosmeticData updated = ((CosmeticUpdatedEvent) event).getCosmetic();
            if (updated != null && updated.getId() != null) catalog.put(key(updated.getId()), updated);
        });
        ListenerAdapter.register(PlayerCosmeticsUpdatedEvent.class, event -> {
            PlayerCosmetics updated = ((PlayerCosmeticsUpdatedEvent) event).getCosmetics();
            if (updated != null && updated.getPlayer() != null) players.put(updated.getPlayer(), updated);
        });
        refreshAsync();
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task -> {
            if (loaded) {
                task.cancel();
                return;
            }
            refreshBlocking();
        }, STARTUP_RETRY_TICKS, STARTUP_RETRY_TICKS);
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, CosmeticService::refreshBlocking,
                REFRESH_INTERVAL_TICKS, REFRESH_INTERVAL_TICKS);
    }

    private static String key(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT);
    }

    public static boolean isLoaded() {
        return loaded;
    }

    /**
     * @param id a cosmetic
     * @return it, or {@code null} when the network has none by that name
     */
    public static @Nullable CosmeticData get(String id) {
        return catalog.get(key(id));
    }

    /**
     * @return every cosmetic, including the ones an admin switched off
     */
    public static List<CosmeticData> getCatalog() {
        return new ArrayList<>(catalog.values());
    }

    /**
     * @param type a kind
     * @return the cosmetics of that kind that players can see
     */
    public static List<CosmeticData> getVisible(CosmeticType type) {
        List<CosmeticData> visible = new ArrayList<>();
        for (CosmeticData cosmetic : catalog.values()) {
            if (cosmetic.getType() == type && cosmetic.isEnabled()) visible.add(cosmetic);
        }
        visible.sort((a, b) -> Integer.compare(a.getPriceBits(), b.getPriceBits()));
        return visible;
    }

    /**
     * @param player somebody
     * @return what they own, never {@code null}
     */
    public static PlayerCosmetics of(UUID player) {
        PlayerCosmetics cosmetics = players.get(player);
        return cosmetics == null ? new PlayerCosmetics(player) : cosmetics;
    }

    /**
     * @param player a player
     * @param id     a cosmetic
     * @return whether they may use it - bought, or free for everybody
     */
    public static boolean owns(UUID player, String id) {
        if (of(player).owns(id)) return true;
        CosmeticData cosmetic = get(id);
        return cosmetic != null && cosmetic.isFree() && cosmetic.isEnabled();
    }

    /**
     * What somebody is wearing of one kind.
     * <p>
     * Never returns something they do not own or that an admin has switched off, so a cosmetic that is
     * taken away stops going off without anybody having to clean up the selections.
     *
     * @param player a player
     * @param type   a kind
     * @return the cosmetic, or {@code null} when they wear none of that kind
     */
    public static @Nullable CosmeticData getSelected(UUID player, CosmeticType type) {
        String id = of(player).getSelected(type);
        if (id == null) return null;
        CosmeticData cosmetic = get(id);
        if (cosmetic == null || !cosmetic.isEnabled() || !owns(player, id)) return null;
        return cosmetic;
    }

    /* ------------------------------------------------------------------ writing */

    /**
     * Buys a cosmetic. Blocks, so it must not run on the main thread.
     *
     * @param player who is buying
     * @param id     what they want
     * @return what the launcher made of it
     */
    public static CosmeticPurchase buyBlocking(UUID player, String id) {
        try {
            if (!ListenerAdapter.isInitialized()) {
                return CosmeticPurchase.failed(id, 0, "Keine Verbindung zum Netzwerk.");
            }
            BuyCosmeticEvent request = new BuyCosmeticEvent(player, id);
            ListenerAdapter.sendListeners(request);
            RespondDataEvent response = ListenerAdapter.waitForEvent(request.getEventId(), TIMEOUT);
            if (response == null || !(response.getData() instanceof CosmeticPurchase purchase)) {
                return CosmeticPurchase.failed(id, 0, "Der Host antwortet nicht.");
            }
            return purchase;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CosmeticPurchase.failed(id, 0, "Unterbrochen.");
        } catch (Exception e) {
            return CosmeticPurchase.failed(id, 0, e.getMessage());
        }
    }

    /**
     * Buys a cosmetic in the background and reports back on the main thread.
     *
     * @param player   who is buying
     * @param id       what they want
     * @param callback what to do with the answer
     */
    public static void buyAsync(UUID player, String id, Consumer<CosmeticPurchase> callback) {
        if (!PaperContext.hasPlugin()) return;
        PaperContext.async(() -> {
            CosmeticPurchase purchase = buyBlocking(player, id);
            if (callback == null) return;
            PaperContext.sync(() -> callback.accept(purchase));
        });
    }

    /**
     * Puts a cosmetic on, or takes it off.
     * <p>
     * The local copy is changed right away so the menu redraws correctly under the player's hand; the
     * launcher's answer overwrites it a moment later, which is what happens if they never owned it.
     *
     * @param player who
     * @param type   which kind
     * @param id     what to wear, {@code null} for nothing
     */
    public static void selectAsync(UUID player, CosmeticType type, String id) {
        if (player == null || !PaperContext.hasPlugin()) return;
        PlayerCosmetics local = of(player).copy();
        local.select(type, id);
        players.put(player, local);
        PaperContext.async(() -> {
            try {
                ListenerAdapter.sendListeners(new SelectCosmeticEvent(player, type, id));
            } catch (Exception e) {
                Bukkit.getLogger().warning("Could not store the cosmetic choice: " + e.getMessage());
                refreshAsync();
            }
        });
    }

    /**
     * Stores what an admin decided about a cosmetic.
     *
     * @param cosmetic the cosmetic as they left it
     */
    public static void saveAsync(CosmeticData cosmetic) {
        if (cosmetic == null || !PaperContext.hasPlugin()) return;
        catalog.put(key(cosmetic.getId()), cosmetic);
        PaperContext.async(() -> {
            try {
                ListenerAdapter.sendListeners(new SaveCosmeticEvent(cosmetic));
            } catch (Exception e) {
                Bukkit.getLogger().warning("Could not store the cosmetic: " + e.getMessage());
                refreshAsync();
            }
        });
    }

    /* ------------------------------------------------------------------ reading */

    public static void refreshAsync() {
        if (!PaperContext.hasPlugin()) return;
        PaperContext.async(CosmeticService::refreshBlocking);
    }

    /**
     * Fetches the catalogue and the ownership. Blocks, so it must not run on the main thread.
     */
    public static void refreshBlocking() {
        try {
            if (!ListenerAdapter.isInitialized()) return;
            RequestCosmeticsEvent request = new RequestCosmeticsEvent();
            ListenerAdapter.sendListeners(request);
            RespondDataEvent response = ListenerAdapter.waitForEvent(request.getEventId(), TIMEOUT);
            if (response == null || !(response.getData() instanceof CosmeticSnapshot snapshot)) return;
            Map<String, CosmeticData> freshCatalog = new ConcurrentHashMap<>();
            for (CosmeticData cosmetic : snapshot.getCatalog()) {
                if (cosmetic.getId() != null) freshCatalog.put(key(cosmetic.getId()), cosmetic);
            }
            catalog.keySet().retainAll(freshCatalog.keySet());
            catalog.putAll(freshCatalog);
            players.keySet().retainAll(snapshot.getPlayers().keySet());
            players.putAll(snapshot.getPlayers());
            loaded = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            Bukkit.getLogger().warning("Could not load the cosmetics: " + e.getMessage());
        }
    }
}

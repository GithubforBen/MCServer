package de.hems.paper.poker;

import de.hems.paper.NetworkSync;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.poker.PokerStatsUpdatedEvent;
import de.hems.communication.events.poker.RequestPokerStatsEvent;
import de.hems.communication.events.poker.SavePokerStatsEvent;
import de.hems.paper.PaperContext;
import de.hems.types.event.EventData;
import de.hems.types.event.PokerEventSettings;
import de.hems.types.poker.PokerStatsData;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The record of the poker nights, as seen from a game server.
 * <p>
 * Same shape as {@link de.hems.paper.event.RunService}: the launcher owns the rows, every server keeps a
 * copy, and every change is announced - which is what lets the ranking board in the lobby move while the
 * hand it is describing is still being played two servers away.
 */
public final class PokerStatsService {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final long REFRESH_INTERVAL_TICKS = 20L * 300L;

    /** Rows by event, then by player. */
    private static final Map<UUID, Map<UUID, PokerStatsData>> rows = new ConcurrentHashMap<>();
    private static volatile boolean loaded = false;
    private static boolean initialized = false;

    private PokerStatsService() {
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
        ListenerAdapter.register(PokerStatsUpdatedEvent.class, event -> apply((PokerStatsUpdatedEvent) event));
        NetworkSync.keepFresh(plugin, PokerStatsService::refreshBlocking, () -> loaded, REFRESH_INTERVAL_TICKS);
    }

    private static void apply(PokerStatsUpdatedEvent event) {
        if (event.getEventId() == null || event.getPlayerId() == null) return;
        Map<UUID, PokerStatsData> ofEvent = rows.computeIfAbsent(event.getEventId(),
                key -> new ConcurrentHashMap<>());
        if (event.isDeleted()) {
            ofEvent.remove(event.getPlayerId());
            return;
        }
        ofEvent.put(event.getPlayerId(), event.getStats());
    }

    public static boolean isLoaded() {
        return loaded;
    }

    /**
     * @param eventId the poker night to look at
     * @return every row of it, in no particular order
     */
    public static List<PokerStatsData> getRowsOf(UUID eventId) {
        if (eventId == null) return List.of();
        Map<UUID, PokerStatsData> ofEvent = rows.get(eventId);
        return ofEvent == null ? List.of() : new ArrayList<>(ofEvent.values());
    }

    /**
     * @param eventId the poker night
     * @param player  the player to look up
     * @return their row, or {@code null} if they never sat down
     */
    public static PokerStatsData getRow(UUID eventId, UUID player) {
        if (eventId == null || player == null) return null;
        Map<UUID, PokerStatsData> ofEvent = rows.get(eventId);
        return ofEvent == null ? null : ofEvent.get(player);
    }

    /**
     * The ranking of one poker night: only the rows that cleared both bars, best ratio first.
     *
     * @param event the poker night
     * @return the ranking
     */
    public static List<PokerStatsData> getRanking(EventData event) {
        if (event == null) return List.of();
        PokerEventSettings settings = new PokerEventSettings(event);
        List<PokerStatsData> ranked = new ArrayList<>();
        for (PokerStatsData row : getRowsOf(event.getId())) {
            if (row.qualifies(settings.getMinHands(), settings.getMinVolume())) ranked.add(row);
        }
        ranked.sort(PokerStatsData.rankingOrder());
        return ranked;
    }

    /**
     * Everybody who took part, whether they cleared the bars or not, best ratio first.
     * <p>
     * The board shows both: the ranking that pays, and underneath it the people who are still short of
     * qualifying. Somebody who is on 3.0 after six hands should be able to see that they need fourteen
     * more rather than wondering why they are not on the list.
     *
     * @param event the poker night
     * @return every row, ordered
     */
    public static List<PokerStatsData> getEveryone(EventData event) {
        if (event == null) return List.of();
        List<PokerStatsData> all = getRowsOf(event.getId());
        all.sort(PokerStatsData.rankingOrder());
        return all;
    }

    /**
     * Writes a row on the launcher without waiting for the answer.
     * <p>
     * Fire and forget on purpose: this is a record, not money. A write that gets lost costs a line in a
     * ranking, and the next write of the same row - they come every hand - carries the whole state again
     * and puts it right.
     *
     * @param stats the row as it now stands
     */
    public static void save(PokerStatsData stats) {
        if (stats == null || stats.getEventId() == null || stats.getPlayerId() == null) return;
        // keep the local copy in step immediately, so a menu opened in the same tick shows the new number
        rows.computeIfAbsent(stats.getEventId(), key -> new ConcurrentHashMap<>())
                .put(stats.getPlayerId(), stats);
        if (!PaperContext.hasPlugin()) return;
        PaperContext.async(() -> {
            try {
                ListenerAdapter.sendListeners(new SavePokerStatsEvent(stats));
            } catch (Exception e) {
                Bukkit.getLogger().warning("Could not write the poker row of "
                        + stats.getPlayerName() + ": " + e.getMessage());
            }
        });
    }

    public static void refreshAsync() {
        if (!PaperContext.hasPlugin()) return;
        PaperContext.async(PokerStatsService::refreshBlocking);
    }

    /**
     * Fetches every row. Blocks, so it must not run on the main thread.
     */
    public static void refreshBlocking() {
        RequestPokerStatsEvent request = new RequestPokerStatsEvent();
        List<PokerStatsData> list = NetworkSync.fetchList(request, TIMEOUT, PokerStatsData.class);
        if (list == null) return;
        Map<UUID, Map<UUID, PokerStatsData>> fresh = new ConcurrentHashMap<>();
        for (PokerStatsData row : list) {
            if (row.getEventId() == null || row.getPlayerId() == null) continue;
            fresh.computeIfAbsent(row.getEventId(), key -> new ConcurrentHashMap<>())
                    .put(row.getPlayerId(), row);
        }
        NetworkSync.replace(rows, fresh);
        loaded = true;
    }
}

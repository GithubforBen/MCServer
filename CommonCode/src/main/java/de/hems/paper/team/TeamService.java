package de.hems.paper.team;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.team.DeleteTeamEvent;
import de.hems.communication.events.team.RequestTeamsEvent;
import de.hems.communication.events.team.RespondTeamSaveEvent;
import de.hems.communication.events.team.SaveTeamEvent;
import de.hems.communication.events.team.TeamUpdatedEvent;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.paper.PaperContext;
import de.hems.paper.PayingPlayers;
import de.hems.types.team.TeamData;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The teams of the network, as seen from a game server.
 * <p>
 * The teams themselves live on the launcher. This keeps a local copy so that looking up which team a player
 * belongs to - something that happens on every block break and every chat message - never touches the
 * network. The copy is kept current by the launcher, which announces every change, so a team created on one
 * server is known on the others a moment later without anybody polling.
 * <p>
 * Writes go the other way: they are sent to the launcher, which decides whether they are allowed and sends
 * the result back. Nothing is ever written locally, so there is exactly one place a team can be changed.
 */
public final class TeamService {

    /** How long to wait for the launcher to answer. */
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    /** How often the whole list is refreshed as a safety net, in ticks. */
    private static final long REFRESH_INTERVAL_TICKS = 20L * 300L;
    /** How often to retry while the list has never arrived, in ticks. */
    private static final long STARTUP_RETRY_TICKS = 40L;

    private static final Map<String, TeamData> teams = new ConcurrentHashMap<>();
    private static volatile boolean loaded = false;
    private static boolean initialized = false;

    private TeamService() {
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
        ListenerAdapter.register(TeamUpdatedEvent.class, event -> apply((TeamUpdatedEvent) event));
        refreshAsync();
        // the network may not be connected yet when this plugin loads, so try again quickly until it is
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task -> {
            if (loaded) {
                task.cancel();
                return;
            }
            refreshBlocking();
        }, STARTUP_RETRY_TICKS, STARTUP_RETRY_TICKS);
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, TeamService::refreshBlocking,
                REFRESH_INTERVAL_TICKS, REFRESH_INTERVAL_TICKS);
    }

    /**
     * Takes over what the launcher announced.
     *
     * @param event the announcement
     */
    private static void apply(TeamUpdatedEvent event) {
        String key = key(event.getTeamName());
        if (key == null) return;
        if (event.isDeleted()) {
            teams.remove(key);
            return;
        }
        teams.put(key, event.getTeam());
    }

    /**
     * @return whether the list has arrived at least once
     */
    public static boolean isLoaded() {
        return loaded;
    }

    /**
     * @return every team of the network
     */
    public static List<TeamData> getTeams() {
        return new ArrayList<>(teams.values());
    }

    /**
     * @param name the team to look up, in any capitalisation
     * @return that team, or {@code null}
     */
    public static TeamData getTeam(String name) {
        String key = key(name);
        return key == null ? null : teams.get(key);
    }

    /**
     * @param uuid the player to look up
     * @return the team that player belongs to, or {@code null}
     */
    public static TeamData getTeamOf(UUID uuid) {
        if (uuid == null) return null;
        for (TeamData team : teams.values()) {
            if (team.hasMember(uuid)) return team;
        }
        return null;
    }

    /**
     * Whether most members of a team pay for the server. That is what decides whether their shared backpack
     * is a single or a double chest.
     *
     * @param team the team to weigh up
     * @return whether the paying members are in the majority
     */
    public static boolean isMajorityPaying(TeamData team) {
        if (team == null) return false;
        return PayingPlayers.isMajorityPaying(team.getMembers());
    }

    /**
     * Fetches the full list in the background.
     */
    public static void refreshAsync() {
        if (!PaperContext.hasPlugin()) return;
        PaperContext.async(TeamService::refreshBlocking);
    }

    /**
     * Fetches the full list. Blocks, so it must not run on the main thread.
     */
    public static void refreshBlocking() {
        try {
            if (!ListenerAdapter.isInitialized()) return;
            RequestTeamsEvent request = new RequestTeamsEvent();
            ListenerAdapter.sendListeners(request);
            RespondDataEvent response = ListenerAdapter.waitForEvent(request.getEventId(), TIMEOUT);
            if (response == null || !(response.getData() instanceof List<?> list)) return;
            Map<String, TeamData> fresh = new ConcurrentHashMap<>();
            for (Object entry : list) {
                if (!(entry instanceof TeamData team) || team.getName() == null) continue;
                fresh.put(key(team.getName()), team);
            }
            teams.clear();
            teams.putAll(fresh);
            loaded = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            Bukkit.getLogger().warning("Could not load the teams: " + e.getMessage());
        }
    }

    /**
     * Stores a team on the launcher.
     * <p>
     * Runs in the background and calls back on the main thread, so the caller can put the result straight
     * into a message or an inventory.
     *
     * @param team            the team to store, carrying the revision it was read at
     * @param createIfMissing whether it may be created
     * @param callback        what to do with the result, on the main thread
     */
    public static void saveAsync(TeamData team, boolean createIfMissing, Consumer<Result> callback) {
        if (!PaperContext.hasPlugin()) return;
        PaperContext.async(() -> {
            Result result = saveBlocking(team, createIfMissing);
            if (callback == null) return;
            PaperContext.sync(() -> callback.accept(result));
        });
    }

    /**
     * Stores a team and waits for the answer. Blocks, so it must not run on the main thread.
     *
     * @param team            the team to store
     * @param createIfMissing whether it may be created
     * @return what the launcher made of it
     */
    public static Result saveBlocking(TeamData team, boolean createIfMissing) {
        try {
            SaveTeamEvent request = new SaveTeamEvent(team, createIfMissing);
            ListenerAdapter.sendListeners(request);
            RespondDataEvent response = ListenerAdapter.waitForEvent(request.getEventId(), TIMEOUT);
            if (!(response instanceof RespondTeamSaveEvent saved)) {
                return new Result(false, "Der Hauptserver hat nicht geantwortet.", null);
            }
            if (saved.isSuccessful() && saved.getData() instanceof TeamData stored) {
                teams.put(key(stored.getName()), stored);
                return new Result(true, saved.getMessage(), stored);
            }
            return new Result(false, saved.getMessage(), null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result(false, "Unterbrochen.", null);
        } catch (Exception e) {
            return new Result(false, "Konnte nicht gespeichert werden: " + e.getMessage(), null);
        }
    }

    /**
     * Removes a team on the launcher, together with its claims and its backpack.
     *
     * @param name the team to remove
     */
    public static void deleteAsync(String name) {
        if (!PaperContext.hasPlugin()) return;
        PaperContext.async(() -> {
            try {
                ListenerAdapter.sendListeners(new DeleteTeamEvent(name));
                teams.remove(key(name));
            } catch (Exception e) {
                Bukkit.getLogger().warning("Could not delete team " + name + ": " + e.getMessage());
            }
        });
    }

    /**
     * Removes a team and waits until the request is out. Blocks, so it must not run on the main thread -
     * {@link #deleteAsync(String)} is what game code uses.
     *
     * @param name the team to remove
     */
    public static void deleteBlocking(String name) {
        try {
            ListenerAdapter.sendListeners(new DeleteTeamEvent(name));
            teams.remove(key(name));
        } catch (Exception e) {
            Bukkit.getLogger().warning("Could not delete team " + name + ": " + e.getMessage());
        }
    }

    private static String key(String name) {
        return name == null ? null : name.toLowerCase(Locale.ROOT);
    }

    /**
     * What came back from a write.
     *
     * @param successful whether it was stored
     * @param message    what to tell the player
     * @param team       the team as it is stored now, or {@code null} when it was refused
     */
    public record Result(boolean successful, String message, TeamData team) {
    }
}

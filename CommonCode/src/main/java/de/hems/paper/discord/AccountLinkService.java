package de.hems.paper.discord;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.discord.AccountLinkUpdatedEvent;
import de.hems.communication.events.discord.ConfirmAccountLinkEvent;
import de.hems.communication.events.discord.RequestAccountLinksEvent;
import de.hems.communication.events.discord.RespondAccountLinkEvent;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.paper.PaperContext;
import de.hems.types.discord.AccountLink;
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
 * Who is who, as seen from a game server.
 * <p>
 * Same shape as the other services: the launcher owns the links, this keeps a copy that is kept current by
 * announcements, and the one write there is - handing in a code - goes to the launcher and waits for its
 * answer, because a link that was not accepted must not be shown as one.
 */
public final class AccountLinkService {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final long REFRESH_INTERVAL_TICKS = 20L * 300L;
    private static final long STARTUP_RETRY_TICKS = 40L;

    private static final Map<UUID, AccountLink> links = new ConcurrentHashMap<>();
    private static volatile boolean loaded = false;
    private static boolean initialized = false;

    private AccountLinkService() {
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
        ListenerAdapter.register(AccountLinkUpdatedEvent.class, event -> {
            AccountLinkUpdatedEvent updated = (AccountLinkUpdatedEvent) event;
            if (updated.getMinecraftId() == null) return;
            if (updated.isRemoved()) {
                links.remove(updated.getMinecraftId());
                return;
            }
            links.put(updated.getMinecraftId(), updated.getLink());
        });
        refreshAsync();
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task -> {
            if (loaded) {
                task.cancel();
                return;
            }
            refreshBlocking();
        }, STARTUP_RETRY_TICKS, STARTUP_RETRY_TICKS);
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, AccountLinkService::refreshBlocking,
                REFRESH_INTERVAL_TICKS, REFRESH_INTERVAL_TICKS);
    }

    public static boolean isLoaded() {
        return loaded;
    }

    /**
     * @param player a minecraft account
     * @return the discord account behind it, or {@code null} when it has never been linked
     */
    public static @Nullable AccountLink of(UUID player) {
        return player == null ? null : links.get(player);
    }

    /**
     * Looks a link up by minecraft name, for somebody who is not online to be asked for their uuid.
     *
     * @param name a minecraft name, in any capitalisation
     * @return the link, or {@code null}
     */
    public static @Nullable AccountLink byName(String name) {
        if (name == null) return null;
        String wanted = name.toLowerCase(Locale.ROOT);
        for (AccountLink link : links.values()) {
            if (link.getMinecraftName() != null
                    && link.getMinecraftName().toLowerCase(Locale.ROOT).equals(wanted)) {
                return link;
            }
        }
        return null;
    }

    /**
     * @return every link the launcher knows
     */
    public static List<AccountLink> all() {
        return new ArrayList<>(links.values());
    }

    /**
     * Hands a code in and waits for the launcher to say whether it was right.
     *
     * @param player  who is typing it
     * @param name    their name, so the link can carry it
     * @param code    the code they were given on discord
     * @param callback what to do with the answer, on the main thread
     */
    public static void confirmAsync(UUID player, String name, String code, Consumer<Result> callback) {
        if (!PaperContext.hasPlugin()) return;
        PaperContext.async(() -> {
            Result result = confirmBlocking(player, name, code);
            if (callback == null) return;
            PaperContext.sync(() -> callback.accept(result));
        });
    }

    /**
     * Hands a code in. Blocks, so it must not run on the main thread.
     *
     * @param player who is typing it
     * @param name   their name
     * @param code   the code
     * @return what the launcher made of it
     */
    public static Result confirmBlocking(UUID player, String name, String code) {
        try {
            if (!ListenerAdapter.isInitialized()) {
                return new Result(false, "Keine Verbindung zum Netzwerk.", null);
            }
            ConfirmAccountLinkEvent request = new ConfirmAccountLinkEvent(player, name, code);
            ListenerAdapter.sendListeners(request);
            RespondDataEvent response = ListenerAdapter.waitForEvent(request.getEventId(), TIMEOUT);
            if (!(response instanceof RespondAccountLinkEvent answer)) {
                return new Result(false, "Der Host antwortet nicht.", null);
            }
            AccountLink link = response.getData() instanceof AccountLink stored ? stored : null;
            if (answer.isSuccessful() && link != null) links.put(link.getMinecraftId(), link);
            return new Result(answer.isSuccessful(), answer.getMessage(), link);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result(false, "Unterbrochen.", null);
        } catch (Exception e) {
            return new Result(false, e.getMessage(), null);
        }
    }

    /**
     * What became of a code.
     *
     * @param successful whether the accounts are now linked
     * @param message    what to tell the player
     * @param link       the link, when there is one
     */
    public record Result(boolean successful, String message, @Nullable AccountLink link) {
    }

    public static void refreshAsync() {
        if (!PaperContext.hasPlugin()) return;
        PaperContext.async(AccountLinkService::refreshBlocking);
    }

    /**
     * Fetches every link. Blocks, so it must not run on the main thread.
     */
    public static void refreshBlocking() {
        try {
            if (!ListenerAdapter.isInitialized()) return;
            RequestAccountLinksEvent request = new RequestAccountLinksEvent();
            ListenerAdapter.sendListeners(request);
            RespondDataEvent response = ListenerAdapter.waitForEvent(request.getEventId(), TIMEOUT);
            if (response == null || !(response.getData() instanceof List<?> list)) return;
            Map<UUID, AccountLink> fresh = new ConcurrentHashMap<>();
            for (Object entry : list) {
                if (entry instanceof AccountLink link && link.getMinecraftId() != null) {
                    fresh.put(link.getMinecraftId(), link);
                }
            }
            links.keySet().retainAll(fresh.keySet());
            links.putAll(fresh);
            loaded = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            Bukkit.getLogger().warning("Could not load the account links: " + e.getMessage());
        }
    }
}

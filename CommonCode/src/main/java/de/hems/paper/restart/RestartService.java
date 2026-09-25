package de.hems.paper.restart;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.restart.RequestRestartStatusEvent;
import de.hems.communication.events.restart.RespondRestartEvent;
import de.hems.communication.events.restart.RestartUpdatedEvent;
import de.hems.communication.events.restart.ScheduleRestartEvent;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.paper.NetworkSync;
import de.hems.paper.PaperContext;
import de.hems.types.restart.RestartMode;
import de.hems.types.restart.RestartStatus;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Set;

/**
 * The restart of the network, as every server sees it: a countdown in chat, a bar in the last minutes, a
 * title in the last seconds, and everybody sent off when it happens.
 * <p>
 * The launcher only says when; each server counts down by itself from that. So a countdown costs no
 * network traffic, and a server that starts in the middle of one picks it up with its first load.
 */
public final class RestartService {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    /** The seconds before the restart at which the chat is told. */
    private static final Set<Long> ANNOUNCE_AT = Set.of(600L, 300L, 180L, 120L, 60L, 30L);
    /** From how many seconds before the restart the bar is shown. */
    private static final long BAR_FROM = 300L;

    private static volatile RestartStatus status = new RestartStatus();
    private static volatile boolean loaded;
    private static boolean initialized;
    private static final BossBar bar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.RED,
            BossBar.Overlay.PROGRESS);
    private static long lastAnnounced = -1L;

    private RestartService() {
    }

    /**
     * Starts following the restart. Called by {@code NetworkPlugin.connect} on every server.
     *
     * @param plugin the plugin of this server
     */
    public static synchronized void init(Plugin plugin) {
        if (initialized) return;
        initialized = true;
        PaperContext.setPlugin(plugin);
        ListenerAdapter.register(RestartUpdatedEvent.class, event -> {
            RestartUpdatedEvent update = (RestartUpdatedEvent) event;
            if (update.getStatus() != null) status = update.getStatus();
            lastAnnounced = -1L;
            if (update.isNow()) PaperContext.sync(RestartService::sendEverybodyOff);
            else PaperContext.sync(RestartService::announceChange);
        });
        NetworkSync.keepFresh(plugin, RestartService::refreshBlocking, () -> loaded, NetworkSync.DEFAULT_REFRESH_TICKS);
        Bukkit.getScheduler().runTaskTimer(plugin, RestartService::tick, 20L, 20L);
    }

    private static void refreshBlocking() {
        RespondDataEvent response = ListenerAdapter.ask(new RequestRestartStatusEvent(), TIMEOUT);
        if (response == null || !(response.getData() instanceof RestartStatus fresh)) return;
        status = fresh;
        loaded = true;
    }

    public static RestartStatus getStatus() {
        return status;
    }

    /**
     * Schedules or calls off a restart and waits for the launcher. Blocks.
     *
     * @param minutes     how long until it happens
     * @param mode        what happens, or {@code null} to call it off
     * @param requestedBy who asked
     * @return why it was refused, or {@code null} when it went through
     */
    public static String requestBlocking(int minutes, RestartMode mode, String requestedBy) {
        RespondDataEvent response = ListenerAdapter.ask(new ScheduleRestartEvent(minutes, mode, requestedBy), TIMEOUT);
        if (!(response instanceof RespondRestartEvent answer)) return "Der Launcher antwortet nicht.";
        if (answer.getData() instanceof RestartStatus fresh) status = fresh;
        return answer.getError();
    }

    /**
     * Once a second: the chat at the set marks, the bar in the last minutes, a title in the last seconds.
     */
    private static void tick() {
        RestartStatus current = status;
        if (!current.isScheduled()) {
            hideBar();
            return;
        }
        long left = current.getSecondsLeft();
        String what = current.getMode().getTitle();
        if (ANNOUNCE_AT.contains(left) && left != lastAnnounced) {
            lastAnnounced = left;
            Bukkit.getServer().sendMessage(Component.text("⚠ " + what + " des Netzwerks in "
                    + RestartStatus.format(left) + ".", NamedTextColor.GOLD));
        }
        if (left <= BAR_FROM) {
            bar.name(Component.text(what + " in " + RestartStatus.format(left), NamedTextColor.RED));
            bar.progress(Math.max(0f, Math.min(1f, left / (float) BAR_FROM)));
            for (Player player : Bukkit.getOnlinePlayers()) player.showBossBar(bar);
        }
        if (left <= 10 && left > 0) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.showTitle(Title.title(Component.text(String.valueOf(left), NamedTextColor.RED),
                        Component.text(what, NamedTextColor.GRAY),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(900), Duration.ZERO)));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
            }
        }
    }

    private static void hideBar() {
        for (Player player : Bukkit.getOnlinePlayers()) player.hideBossBar(bar);
    }

    private static void announceChange() {
        RestartStatus current = status;
        if (!current.isScheduled()) {
            hideBar();
            Bukkit.getServer().sendMessage(Component.text("✓ Der geplante Neustart wurde abgesagt.",
                    NamedTextColor.GREEN));
            return;
        }
        Bukkit.getServer().sendMessage(Component.text("⚠ " + current.getMode().getTitle() + " des Netzwerks in "
                + RestartStatus.format(current.getSecondsLeft()) + ".", NamedTextColor.GOLD));
    }

    /**
     * It is happening: everybody gets a message and is sent off, so nobody is standing in a world while it
     * is being saved and stopped.
     */
    private static void sendEverybodyOff() {
        RestartMode mode = status.getMode();
        Component message = Component.text(mode == RestartMode.SHUTDOWN
                ? "Das Netzwerk wird heruntergefahren."
                : "Das Netzwerk startet neu" + (mode == RestartMode.UPDATE ? " mit einem Update" : "")
                + " - in ein paar Minuten ist es wieder da.", NamedTextColor.GOLD);
        hideBar();
        for (Player player : Bukkit.getOnlinePlayers()) player.kick(message);
        // everything that is only in memory goes to disk now; the stop that follows saves again
        Bukkit.savePlayers();
        Bukkit.getWorlds().forEach(org.bukkit.World::save);
    }
}

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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Set;

/**
 * The restart of the network, as every server sees it: a title and a chat line when it is scheduled, at every
 * mark of the countdown and for everybody who joins while it is coming, a bar in the last minutes, a seconds
 * countdown at the end, and everybody sent off when it happens.
 * <p>
 * The launcher only says when; each server counts down by itself from that. So a countdown costs no
 * network traffic, and a server that starts in the middle of one picks it up with its first load.
 */
public final class RestartService implements Listener {

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

    /** How long after a join the title comes, so whatever the server shows on join does not cover it. */
    private static final long JOIN_DELAY_TICKS = 40L;

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
        Bukkit.getPluginManager().registerEvents(new RestartService(), plugin);
    }

    /**
     * Whoever joins while a restart is coming is told at once - a chat line from five minutes ago is
     * nothing they have seen.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(PaperContext.getPlugin(), () -> {
            RestartStatus current = status;
            if (!player.isOnline() || !current.isScheduled()) return;
            long left = current.getSecondsLeft();
            // the last seconds have their own countdown title
            if (left <= 10) return;
            player.sendMessage(chatLine(current.getMode(), left));
            player.showTitle(warning(current.getMode(), left));
        }, JOIN_DELAY_TICKS);
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
            announce(current.getMode(), left);
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
        long left = current.getSecondsLeft();
        // this line is the announcement for the mark it falls on: a restart set for five minutes would
        // otherwise be announced twice in the same second, once here and once by the countdown
        lastAnnounced = left;
        announce(current.getMode(), left);
    }

    /** Chat and title for everybody on this server. */
    private static void announce(RestartMode mode, long left) {
        Bukkit.getServer().sendMessage(chatLine(mode, left));
        if (left <= 10) return;
        Title title = warning(mode, left);
        for (Player player : Bukkit.getOnlinePlayers()) player.showTitle(title);
    }

    private static Component chatLine(RestartMode mode, long left) {
        return Component.text("⚠ " + mode.getTitle() + " des Netzwerks in " + RestartStatus.format(left) + ".",
                NamedTextColor.GOLD);
    }

    /**
     * @return "In 10 Minuten wird der Server neu starten", or what fits the mode
     */
    static Title warning(RestartMode mode, long left) {
        String what = switch (mode) {
            case SHUTDOWN -> "heruntergefahren";
            case UPDATE -> "für ein Update neu starten";
            default -> "neu starten";
        };
        return Title.title(Component.text("⚠ " + mode.getTitle(), NamedTextColor.GOLD),
                Component.text("In " + spoken(left) + " wird der Server " + what, NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(5), Duration.ofMillis(700)));
    }

    /**
     * @return "10 Minuten", "1 Minute", "30 Sekunden" - rounded down: someone told four minutes when four and
     *         a half are left is fine, someone told five is not
     */
    static String spoken(long seconds) {
        if (seconds < 60) return seconds + (seconds == 1 ? " Sekunde" : " Sekunden");
        long minutes = seconds / 60;
        return minutes + (minutes == 1 ? " Minute" : " Minuten");
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

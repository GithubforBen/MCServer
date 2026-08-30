package de.hems.paper.warp;

import de.hems.api.ServerApi;
import de.hems.paper.PaperContext;
import de.hems.types.FileType;
import de.hems.types.Server;
import de.hems.types.ServerPhase;
import de.hems.types.ServerTemplate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Starts a server and takes players there once it is actually ready.
 * <p>
 * Warping straight after asking for a server does not work: the request is only a message to the launcher,
 * and even once the process runs, paper spends the better part of a minute loading worlds and generating
 * the spawn area. A player sent there in the meantime is bounced by the proxy and left standing where they
 * were with no idea why. So the warp waits for {@link ServerPhase#READY} - and because that wait is long
 * enough to feel broken, every step of it is reported: queued, starting, terrain at some percent, ready.
 */
public final class ServerStartup {

    /** How often the host is asked how far the server is. */
    private static final long POLL_INTERVAL_MS = 1000L;
    /** How long a server may take before waiting for it is given up on. */
    private static final long TIMEOUT_MS = 5L * 60L * 1000L;
    /** How wide the bar in the action bar is drawn. */
    private static final int BAR_WIDTH = 20;

    private ServerStartup() {
    }

    /**
     * Creates a server and warps everyone there as soon as it accepts players.
     *
     * @param players      who is going, may be empty to only start the server
     * @param name         the name of the server
     * @param template     the blueprint to use
     * @param memoryMB     the memory in MB, or {@code null} for the default of the template
     * @param extraPlugins plugins installed on top of the template, may be {@code null}
     */
    public static void createAndWarp(Collection<? extends Player> players, String name, ServerTemplate template,
                                     Integer memoryMB, Collection<FileType.PLUGIN> extraPlugins) {
        Set<UUID> waiting = idsOf(players);
        PaperContext.async(() -> {
            try {
                ServerApi.createServer(name, template, memoryMB, extraPlugins);
            } catch (Exception e) {
                tell(waiting, "Der Server konnte nicht gestartet werden: " + e.getMessage(), NamedTextColor.RED);
                return;
            }
            awaitAndWarp(waiting, name);
        });
    }

    /**
     * Creates a server and warps one player there as soon as it accepts players.
     *
     * @param player   who is going
     * @param name     the name of the server
     * @param template the blueprint to use
     */
    public static void createAndWarp(Player player, String name, ServerTemplate template) {
        createAndWarp(List.of(player), name, template, null, null);
    }

    /**
     * Makes sure a server runs and warps everyone there once it is ready. A server that is already up is
     * simply warped to, one that is still booting is waited for, and one that is gone is started again.
     * <p>
     * This is what picking a paused run back up needs: the server keeps its name and therefore its world,
     * so starting it again continues where it left off.
     *
     * @param players  who is going
     * @param name     the name of the server
     * @param template the blueprint to start it from if it is not running
     */
    public static void ensureAndWarp(Collection<? extends Player> players, String name, ServerTemplate template) {
        Set<UUID> waiting = idsOf(players);
        PaperContext.async(() -> {
            try {
                if (!ServerApi.isRunning(name)) ServerApi.createServer(name, template, null, null);
            } catch (Exception e) {
                tell(waiting, "Der Server konnte nicht gestartet werden: " + e.getMessage(), NamedTextColor.RED);
                return;
            }
            awaitAndWarp(waiting, name);
        });
    }

    /**
     * Warps a player to a server that is already on its way up, waiting for it rather than failing.
     *
     * @param player the player to warp
     * @param name   the name of the server
     */
    public static void warpWhenReady(Player player, String name) {
        warpWhenReady(List.of(player), name);
    }

    /**
     * Warps a group to a server that is already on its way up, waiting for it rather than failing.
     * <p>
     * Unlike {@link #ensureAndWarp} this never starts anything: it is for a server somebody else has
     * already ordered, and a server that never comes up is given up on rather than started again.
     *
     * @param players who is going
     * @param name    the name of the server
     */
    public static void warpWhenReady(Collection<? extends Player> players, String name) {
        Set<UUID> waiting = idsOf(players);
        if (waiting.isEmpty()) return;
        PaperContext.async(() -> awaitAndWarp(waiting, name));
    }

    /**
     * Watches a server come up and warps everyone the moment it is ready. Blocks, so it only ever runs off
     * the main thread.
     *
     * @param waiting who is going
     * @param name    the name of the server
     */
    private static void awaitAndWarp(Set<UUID> waiting, String name) {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        ServerPhase lastReported = null;
        while (System.currentTimeMillis() < deadline) {
            if (onlineOf(waiting).isEmpty() && !waiting.isEmpty()) {
                // everybody logged off while waiting - the server stays up, but nobody needs telling
                return;
            }
            Server server = find(name);
            ServerPhase phase = server == null ? ServerPhase.QUEUED : server.getPhase();
            String description = server == null ? ServerPhase.QUEUED.getDescription() : server.getPhaseDescription();

            if (phase == ServerPhase.READY && (server == null || server.isJoinable())) {
                if (lastReported != null) tell(waiting, "Warp wird vorbereitet ...", NamedTextColor.GRAY);
                tell(waiting, ServerPhase.READY.getDescription() + ".", NamedTextColor.GREEN);
                actionBar(waiting, description, 100);
                warp(waiting, name);
                return;
            }
            if (phase == ServerPhase.OFFLINE || phase == ServerPhase.STOPPING) {
                tell(waiting, name + " ist wieder aus gegangen.", NamedTextColor.RED);
                return;
            }
            if (phase != lastReported) {
                // one line per step, never one per percent - the moving number lives in the action bar
                tell(waiting, phase.getDescription() + " ...", NamedTextColor.GRAY);
                lastReported = phase;
            }
            actionBar(waiting, description, percentOf(phase, server));
            if (!sleep()) return;
        }
        tell(waiting, name + " ist nicht rechtzeitig hochgekommen. Mit /warp " + name
                + " kannst du es später nochmal versuchen.", NamedTextColor.RED);
    }

    /**
     * The share of the whole startup that is done, so the bar moves through all of it rather than jumping
     * back to zero when the terrain starts.
     *
     * @param phase  where the server is
     * @param server the server, or {@code null} while the host does not know it yet
     * @return the progress in percent
     */
    private static int percentOf(ServerPhase phase, Server server) {
        return switch (phase) {
            case QUEUED -> 5;
            case STARTING -> 20;
            case GENERATING -> 30 + (server == null ? 0 : server.getPhasePercent() * 65 / 100);
            case READY -> 100;
            default -> 0;
        };
    }

    private static Server find(String name) {
        try {
            for (Server server : ServerApi.listServers()) {
                if (server.getName().equalsIgnoreCase(name)) return server;
            }
        } catch (Exception e) {
            // a single unanswered request does not mean the server is not coming - look again next round
        }
        return null;
    }

    private static void warp(Set<UUID> waiting, String name) {
        PaperContext.sync(() -> {
            for (Player player : onlineOf(waiting)) ServerConnector.connect(player, name);
        });
    }

    /**
     * @return whether the wait may carry on
     */
    private static boolean sleep() {
        try {
            Thread.sleep(POLL_INTERVAL_MS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static Set<UUID> idsOf(Collection<? extends Player> players) {
        Set<UUID> ids = new LinkedHashSet<>();
        if (players != null) {
            for (Player player : players) if (player != null) ids.add(player.getUniqueId());
        }
        return ids;
    }

    private static List<Player> onlineOf(Set<UUID> ids) {
        List<Player> online = new ArrayList<>();
        for (UUID id : ids) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) online.add(player);
        }
        return online;
    }

    private static void tell(Set<UUID> ids, String message, NamedTextColor color) {
        if (ids.isEmpty()) return;
        PaperContext.sync(() -> {
            for (Player player : onlineOf(ids)) player.sendMessage(Component.text(message, color));
        });
    }

    /**
     * The bar above the hotbar, which is where a wait belongs - it updates every second without pushing the
     * chat away.
     *
     * @param ids     who is waiting
     * @param label   what the server is doing
     * @param percent how far it is
     */
    private static void actionBar(Set<UUID> ids, String label, int percent) {
        if (ids.isEmpty()) return;
        int filled = Math.max(0, Math.min(BAR_WIDTH, percent * BAR_WIDTH / 100));
        Component bar = Component.text("▏".repeat(filled), NamedTextColor.GREEN)
                .append(Component.text("▏".repeat(BAR_WIDTH - filled), NamedTextColor.DARK_GRAY));
        Component line = Component.text(label + "  ", NamedTextColor.AQUA).append(bar);
        PaperContext.sync(() -> {
            for (Player player : onlineOf(ids)) player.sendActionBar(line);
        });
    }
}

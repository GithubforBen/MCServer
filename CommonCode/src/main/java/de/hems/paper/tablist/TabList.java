package de.hems.paper.tablist;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.server.RequestProxyPlayersEvent;
import de.hems.communication.events.server.RespondProxyPlayersEvent;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.paper.PaperContext;
import de.hems.paper.event.EventAnnouncer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The tab list of every server of the network: the same frame everywhere, the content of the game mode
 * inside it.
 * <p>
 * The header says where you are - the network, the mode, how many are online in the whole network and how
 * many of them here. The footer carries what the mode has to say ({@link TabContent}), what the events are
 * doing, the commands worth knowing here and, for ops only, how the server is doing. Survival used to have
 * a list of its own with the tps for everybody and the bits as a bare number; the other servers had none.
 */
public final class TabList {

    /** What the network is called at the top of the list. */
    public static final String NETWORK_NAME = "MCSERVER";
    /** How often the network is asked how many are online, in ticks - it is a round trip through the proxy. */
    private static final long NETWORK_REFRESH_TICKS = 20L * 10L;
    private static final Duration PROXY_TIMEOUT = Duration.ofSeconds(2);
    private static final Component RULE = Component.text(" ".repeat(48), NamedTextColor.DARK_GRAY)
            .decorate(TextDecoration.STRIKETHROUGH);

    private static TabContent content;
    /** How many are online in the whole network, {@code -1} while the proxy has not said. */
    private static volatile int networkOnline = -1;
    private static final AtomicBoolean asking = new AtomicBoolean();

    private TabList() {
    }

    /**
     * Starts drawing the tab list for everybody on this server.
     *
     * @param plugin      the plugin of this server
     * @param modeContent what this game mode puts into it
     */
    public static synchronized void init(Plugin plugin, TabContent modeContent) {
        boolean first = content == null;
        content = modeContent;
        if (!first) return;
        Bukkit.getScheduler().runTaskTimer(plugin, TabList::update, 20L, 20L);
        Bukkit.getScheduler().runTaskTimer(plugin, TabList::refreshNetwork, 40L, NETWORK_REFRESH_TICKS);
    }

    private static void update() {
        TabContent mode = content;
        if (mode == null) return;
        Component events = EventAnnouncer.tabLine();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendPlayerListHeaderAndFooter(header(mode, player), footer(mode, player, events));
            Component name = mode.listName(player);
            if (name != null) player.playerListName(name);
        }
    }

    private static Component header(TabContent mode, Player viewer) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(Component.text("✦ ", NamedTextColor.GOLD)
                .append(Component.text(NETWORK_NAME, NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(Component.text(" ✦", NamedTextColor.GOLD)));
        lines.add(Component.text(mode.mode(), mode.accent(), TextDecoration.BOLD)
                .append(Component.text("  ·  ", NamedTextColor.DARK_GRAY))
                .append(online()));
        lines.addAll(mode.header(viewer));
        lines.add(RULE);
        return join(lines);
    }

    private static Component online() {
        int here = Bukkit.getOnlinePlayers().size();
        int network = networkOnline;
        if (network < here) {
            return Component.text(here + " online", NamedTextColor.GRAY);
        }
        return Component.text(network + " online", NamedTextColor.WHITE)
                .append(Component.text(" (" + here + " hier)", NamedTextColor.GRAY));
    }

    private static Component footer(TabContent mode, Player viewer, Component events) {
        List<Component> lines = new ArrayList<>();
        lines.add(RULE);
        lines.addAll(mode.footer(viewer));
        if (events != null) lines.add(events);
        String hint = mode.hint();
        if (hint != null) lines.add(Component.text(hint, NamedTextColor.DARK_GRAY));
        // for everybody: a player who feels the server lag should be able to see that it is the server
        double tps = Math.min(20.0, Bukkit.getServer().getTPS()[0]);
        NamedTextColor color = tps >= 19.0 ? NamedTextColor.GREEN : tps >= 15.0 ? NamedTextColor.YELLOW
                : NamedTextColor.RED;
        lines.add(Component.text(ListenerAdapter.isInitialized() ? String.valueOf(ListenerAdapter.getName()) : "?",
                        NamedTextColor.DARK_GRAY)
                .append(Component.text("  TPS ", NamedTextColor.DARK_GRAY))
                .append(Component.text(String.format(Locale.ROOT, "%.1f", tps), color))
                .append(Component.text("  " + Math.round(Bukkit.getServer().getAverageTickTime()) + " ms",
                        NamedTextColor.DARK_GRAY)));
        lines.add(Component.empty());
        return join(lines);
    }

    private static Component join(List<Component> lines) {
        Component text = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) text = text.append(Component.newline());
            text = text.append(lines.get(i));
        }
        return text;
    }

    /**
     * Asks the proxy how many are online, off the main thread.
     */
    private static void refreshNetwork() {
        if (!ListenerAdapter.isInitialized() || !asking.compareAndSet(false, true)) return;
        PaperContext.async(() -> {
            try {
                RequestProxyPlayersEvent request = new RequestProxyPlayersEvent();
                ListenerAdapter.sendListeners(request);
                RespondDataEvent response = ListenerAdapter.waitForEvent(request.getEventId(), PROXY_TIMEOUT);
                if (response instanceof RespondProxyPlayersEvent players && players.getPlayersPerServer() != null) {
                    int total = 0;
                    for (Map.Entry<String, List<String>> server : players.getPlayersPerServer().entrySet()) {
                        if (server.getValue() != null) total += server.getValue().size();
                    }
                    networkOnline = total;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                // no proxy answer: the header shows who is here, which is still true
            } finally {
                asking.set(false);
            }
        });
    }
}

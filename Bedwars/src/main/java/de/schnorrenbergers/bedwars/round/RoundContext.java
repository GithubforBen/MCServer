package de.schnorrenbergers.bedwars.round;

import de.hems.communication.ListenerAdapter;
import de.hems.paper.round.RoundService;
import de.hems.paper.warp.ServerConnector;
import de.hems.types.round.RoundData;
import de.hems.types.round.RoundState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The round this server was started for, when a player started it.
 * <p>
 * A server is created with a name and nothing else, so it finds out what it is by looking itself up - the
 * same trick the events use. What comes back is the map, the mode, the addons and, unlike an event, a
 * person: whoever pressed the button owns this round and may run it.
 * <p>
 * Everything here tolerates there being no round at all. A server started by an event, by {@code /bwdebug}
 * or by hand has none, and then this is simply empty and nothing behaves differently.
 */
public final class RoundContext {

    /**
     * The round as this server found it at start. What is current lives in {@link RoundService}, which
     * follows every change - an invitation sent from the lobby lands there, not here.
     */
    private static volatile RoundData round;
    /** What this server is called, to look the round up again if the start could not. */
    private static volatile String serverName;
    /**
     * Whether the lookup at start got an answer. Without one, "no round" and "the launcher did not answer"
     * look the same - and the first one opens the server to everybody.
     */
    private static volatile boolean known;
    /** How long somebody sent back gets to leave before the server closes the connection itself. */
    private static final long KICK_FALLBACK_TICKS = 60L;
    /** Who was thrown out of this round, so they do not simply walk back in. */
    private static final Set<UUID> kicked = ConcurrentHashMap.newKeySet();

    private RoundContext() {
    }

    /**
     * Looks this server up in the round list.
     * <p>
     * Blocks, and is meant to: it runs while the server starts, and what it finds decides which map is
     * loaded. Working that out a second later would mean loading a world underneath whoever had already
     * joined.
     *
     * @param serverName what this server is called on the network
     */
    public static void load(String serverName) {
        RoundContext.serverName = serverName;
        RoundService.refreshBlocking();
        known = RoundService.isLoaded();
        round = RoundService.byServer(serverName);
    }

    /**
     * @return the round this server is playing, or {@code null} when nobody ordered it
     */
    public static @Nullable RoundData get() {
        resolve();
        RoundData start = round;
        if (start == null) return null;
        RoundData fresh = RoundService.get(start.getId());
        return fresh != null ? fresh : start;
    }

    public static boolean exists() {
        return get() != null;
    }

    /**
     * Catches up on a lookup that got no answer at start, once the background refresh has one.
     */
    private static void resolve() {
        if (known || serverName == null || !RoundService.isLoaded()) return;
        round = RoundService.byServer(serverName);
        known = true;
    }

    /** Stores a change, here and at the launcher. */
    private static void store(RoundData updated) {
        round = updated;
        RoundService.remember(updated);
        RoundService.saveAsync(updated, null);
    }

    /**
     * @param player somebody on this server
     * @return whether they own this round
     */
    public static boolean isOwner(Player player) {
        RoundData current = get();
        return current != null && player != null && current.isOwner(player.getUniqueId());
    }

    /**
     * @param player somebody on this server
     * @return whether they may run this round - its owner, or a real admin
     */
    public static boolean mayAdminister(Player player) {
        if (player == null) return false;
        return player.isOp() || player.hasPermission("bedwars.admin") || isOwner(player);
    }

    /**
     * @param player somebody trying to join
     * @return whether the round is closed to them
     */
    public static boolean isKicked(Player player) {
        return player != null && kicked.contains(player.getUniqueId());
    }

    /**
     * Throws somebody out of this round and back to the hub.
     *
     * @param player who has to go
     */
    public static void kick(Player player) {
        if (player == null) return;
        kicked.add(player.getUniqueId());
        sendBack(player);
    }

    /**
     * Sends somebody who may not be here back to the hub, without marking them as thrown out - an uninvited
     * visitor who is invited later must be able to come back.
     *
     * @param player who has to go
     */
    public static void sendBack(Player player) {
        if (player == null) return;
        ServerConnector.connect(player, ListenerAdapter.ServerName.LOBBY);
        // the warp is a request to the proxy and can get lost - right after a join, or with the lobby down.
        // Somebody who has to go must not stay because of that, so after a moment the connection is closed
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Bedwars");
        if (plugin == null) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && (isKicked(player) || !mayJoin(player))) {
                player.kick(Component.text("Du darfst in diese Runde nicht hinein.", NamedTextColor.RED));
            }
        }, KICK_FALLBACK_TICKS);
    }

    /**
     * Lets somebody back in.
     *
     * @param player who is forgiven
     */
    public static void unkick(UUID player) {
        kicked.remove(player);
    }

    /**
     * @return everybody who was thrown out
     */
    public static Set<UUID> getKicked() {
        return Set.copyOf(kicked);
    }

    /**
     * Whether somebody may be on this round at all.
     * <p>
     * A closed round that only hides itself from a list is not closed: a server name is easy to guess and
     * easy to type into {@code /warp}. This is where "private" is actually enforced.
     *
     * @param player who turned up
     * @return whether they may stay
     */
    public static boolean mayJoin(Player player) {
        if (player == null) return true;
        if (player.isOp() || player.hasPermission("bedwars.admin")) return true;
        RoundData current = get();
        // nobody knows yet whether this is a private round: closed until it is known, not open
        if (current == null) return known || serverName == null;
        return current.isAllowed(player.getUniqueId());
    }

    /**
     * Lets somebody into a closed round.
     *
     * @param player who may come
     * @return whether they were not already invited
     */
    public static boolean invite(UUID player) {
        RoundData current = get();
        if (current == null) return false;
        RoundData updated = current.copy();
        // an invitation forgives an earlier kick, otherwise it lets nobody in
        kicked.remove(player);
        if (!updated.invite(player)) return false;
        store(updated);
        return true;
    }

    /**
     * Opens or closes the round to strangers.
     *
     * @param open whether it shows up in the lobby list
     */
    public static void setOpen(boolean open) {
        RoundData current = get();
        if (current == null) return;
        RoundData updated = current.copy();
        updated.setOpen(open);
        store(updated);
    }

    /**
     * Tells the network where this round stands, so the lobby list stops offering a round that has begun
     * and the launcher stops counting one that is over.
     *
     * @param state   where the round is
     * @param players how many are on it
     */
    public static void report(RoundState state, int players) {
        RoundData current = get();
        if (current == null) return;
        if (current.getState() == state && current.getPlayers() == players) return;
        RoundData updated = current.copy();
        updated.setState(state);
        updated.setPlayers(players);
        store(updated);
    }
}

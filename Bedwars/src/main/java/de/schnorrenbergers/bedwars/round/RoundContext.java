package de.schnorrenbergers.bedwars.round;

import de.hems.communication.ListenerAdapter;
import de.hems.paper.round.RoundService;
import de.hems.paper.warp.ServerConnector;
import de.hems.types.round.RoundData;
import de.hems.types.round.RoundState;
import org.bukkit.entity.Player;
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

    private static volatile RoundData round;
    /** This server's name, to find the round in the network's live list again. */
    private static volatile String server;
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
        RoundService.refreshBlocking();
        server = serverName;
        round = RoundService.byServer(serverName);
    }

    /**
     * @return the round this server is playing, or {@code null} when nobody ordered it
     */
    public static @Nullable RoundData get() {
        // the network's copy, which every change reaches - an invitation sent from the lobby, a switch to
        // private in the menu. The one read at start only stands in while the list is not there
        RoundData live = server == null ? null : RoundService.byServer(server);
        if (live != null) round = live;
        return round;
    }

    public static boolean exists() {
        return get() != null;
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
        sendToLobby(player);
    }

    /**
     * Sends somebody to the lobby without holding it against them - for whoever came to a private round
     * uninvited, who may still be invited a minute later.
     *
     * @param player who has to go
     */
    public static void sendToLobby(Player player) {
        if (player == null) return;
        ServerConnector.connect(player, ListenerAdapter.ServerName.LOBBY);
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
        RoundData current = get();
        if (current == null || player == null) return true;
        if (player.isOp() || player.hasPermission("bedwars.admin")) return true;
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
        // an invitation forgives: whoever the owner asks in may come, even after being thrown out
        kicked.remove(player);
        RoundData updated = current.copy();
        if (!updated.invite(player)) return false;
        round = updated;
        RoundService.saveAsync(updated, null);
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
        round = updated;
        RoundService.saveAsync(updated, null);
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
        round = updated;
        RoundService.saveAsync(updated, null);
    }
}

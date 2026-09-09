package de.schnorrenbergers.poker;

import de.hems.types.event.PokerEventSettings;
import de.schnorrenbergers.poker.bank.Bank;
import de.schnorrenbergers.poker.game.PokerPlayer;
import de.schnorrenbergers.poker.world.CasinoLayout;
import de.schnorrenbergers.poker.world.CasinoWorld;
import de.schnorrenbergers.poker.world.TableSpot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Every table in the house, and everything that is true of all of them.
 * <p>
 * It owns the tick that drives the tables, the map of who is sitting where, and the one number the launcher
 * really cares about: how many chips are on the tables and whose they are. That last one is reported from
 * here rather than from a table, because somebody can be sitting at two of them and what has to go back to
 * them if this server dies is the sum.
 */
public final class Casino {

    /** How long somebody has to make up their mind. */
    public static final int ACTION_SECONDS = 30;
    /** How often the tables are driven, in ticks. Four times a second is enough for a card game. */
    private static final long TICK_INTERVAL = 5L;

    private static Plugin plugin;
    private static CasinoLayout layout;
    private static PokerEventSettings settings;
    private static final List<CasinoTable> tables = new ArrayList<>();
    /** Where everybody is sitting, so a click or a quit finds the right table without searching all of them. */
    private static final Map<UUID, CasinoTable> seatedAt = new HashMap<>();
    /** What is on each table, per account, so the total can be reported without asking every table. */
    private static final Map<Integer, Map<UUID, Integer>> stacksByTable = new HashMap<>();
    private static final Map<UUID, String> accountNames = new HashMap<>();
    private static boolean closed;

    private Casino() {
    }

    /**
     * Opens the house.
     *
     * @param owner     the plugin
     * @param theLayout where the tables are
     * @param theRules  what is played for
     */
    public static void open(Plugin owner, CasinoLayout theLayout, PokerEventSettings theRules) {
        plugin = owner;
        layout = theLayout;
        settings = theRules;
        World world = CasinoWorld.get();
        if (world == null) {
            owner.getLogger().severe("There is no casino world - no table can be opened.");
            return;
        }
        for (TableSpot spot : layout.getTables()) {
            tables.add(new CasinoTable(owner, world, spot, settings));
        }
        owner.getLogger().info("Opened " + tables.size() + " tables.");
        Bukkit.getScheduler().runTaskTimer(owner, () -> {
            if (closed) return;
            long now = System.currentTimeMillis();
            for (CasinoTable table : tables) table.tick(now);
        }, TICK_INTERVAL, TICK_INTERVAL);
    }

    public static PokerEventSettings getSettings() {
        return settings;
    }

    public static CasinoLayout getLayout() {
        return layout;
    }

    public static List<CasinoTable> getTables() {
        return List.copyOf(tables);
    }

    /**
     * @param index which table
     * @return it, or {@code null}
     */
    public static @Nullable CasinoTable getTable(int index) {
        for (CasinoTable table : tables) {
            if (table.getIndex() == index) return table;
        }
        return null;
    }

    /**
     * @param player somebody here
     * @return the table they are sitting at, or {@code null}
     */
    public static @Nullable CasinoTable tableOf(Player player) {
        return seatedAt.get(player.getUniqueId());
    }

    /**
     * @param location somewhere in the casino
     * @return the table whose chair is nearest, or {@code null} if there is none within reach
     */
    public static @Nullable CasinoTable tableNear(Location location) {
        for (CasinoTable table : tables) {
            if (table.seatNear(location) >= 0) return table;
        }
        return null;
    }

    /**
     * Buys somebody in and sits them down.
     * <p>
     * The order matters and is the reason this is one method rather than two: the bits are taken first and
     * the seat is only given once they are really gone. The other way round is a seat with chips that were
     * never paid for, and one crash at the wrong moment turns that into money out of nothing.
     *
     * @param player who wants to play
     * @param table  which table
     * @param seat   which chair, or {@code -1} for any
     */
    public static void sitDown(Player player, CasinoTable table, int seat) {
        if (seatedAt.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("Du sitzt schon an einem Tisch.", NamedTextColor.RED));
            return;
        }
        if (seat >= 0 && table.isTaken(seat)) {
            player.sendMessage(Component.text("Der Platz ist besetzt.", NamedTextColor.RED));
            return;
        }
        if (!table.hasRoom()) {
            player.sendMessage(Component.text("Der Tisch ist voll.", NamedTextColor.RED));
            return;
        }
        int buyIn = settings.getBuyIn();
        player.sendMessage(Component.text("Du kaufst dich für " + buyIn + " Bits ein ...",
                NamedTextColor.GRAY));
        Bank.buyIn(player, buyIn, "Poker: Buy-in " + CasinoContext.getTitle(), chips -> {
            if (chips <= 0) return;
            if (!table.seat(player, seat, chips)) {
                // the seat went while the launcher was answering. The money goes straight back rather
                // than sitting in a nowhere
                Bank.cashOut(player.getUniqueId(), player.getName(), chips,
                        "Poker: Platz war weg, Buy-in zurück");
                player.sendMessage(Component.text("Der Platz war schneller weg als das Geld da. "
                        + "Deine Bits sind zurück.", NamedTextColor.YELLOW));
                return;
            }
            seatedAt.put(player.getUniqueId(), table);
        });
    }

    /**
     * Buys more chips for somebody who is already sitting.
     *
     * @param player who is topping up
     */
    public static void rebuy(Player player) {
        CasinoTable table = tableOf(player);
        if (table == null) {
            player.sendMessage(Component.text("Du sitzt an keinem Tisch.", NamedTextColor.RED));
            return;
        }
        if (!settings.getFormat().allowsCashOut()) {
            player.sendMessage(Component.text("Im Turnier wird nicht nachgekauft.", NamedTextColor.RED));
            return;
        }
        PokerPlayer seat = table.find(player.getUniqueId());
        if (seat == null) return;
        if (seat.isInHand() && !seat.isFolded()) {
            player.sendMessage(Component.text("Mitten in der Hand geht das nicht - warte sie ab.",
                    NamedTextColor.YELLOW));
            return;
        }
        int buyIn = settings.getBuyIn();
        Bank.buyIn(player, buyIn, "Poker: Nachkauf " + CasinoContext.getTitle(), chips -> {
            if (chips <= 0) return;
            seat.addChips(chips);
            seat.setSittingOut(false);
            player.sendMessage(Component.text("+" + chips + " Chips. Du hast jetzt "
                    + seat.getChips() + ".", NamedTextColor.GREEN));
        });
    }

    /**
     * Takes somebody off whatever table they are at.
     *
     * @param player who is leaving
     */
    public static void leave(Player player) {
        CasinoTable table = seatedAt.get(player.getUniqueId());
        if (table == null) {
            player.sendMessage(Component.text("Du sitzt an keinem Tisch.", NamedTextColor.RED));
            return;
        }
        String message = table.leave(player.getUniqueId());
        // the seat is only forgotten once the table has really let them go. An all-in player waits for the
        // hand, and until then they are still sitting there
        if (table.find(player.getUniqueId()) == null) seatedAt.remove(player.getUniqueId());
        player.sendMessage(Component.text(message, NamedTextColor.YELLOW));
    }

    /**
     * Called when somebody logs off. Same as standing up, without anybody to tell.
     *
     * @param player who left
     */
    public static void handleQuit(Player player) {
        CasinoTable table = seatedAt.get(player.getUniqueId());
        if (table == null) return;
        table.leave(player.getUniqueId());
        if (table.find(player.getUniqueId()) == null) seatedAt.remove(player.getUniqueId());
    }

    /**
     * Takes a table's word for what is on it and reports the total per account to the launcher.
     *
     * @param tableIndex which table
     * @param stacks     what is on it, per account
     * @param names      what those accounts are called
     */
    static void reportStacks(int tableIndex, Map<UUID, Integer> stacks, Map<UUID, String> names) {
        stacksByTable.put(tableIndex, new HashMap<>(stacks));
        accountNames.putAll(names);
        Map<UUID, Integer> total = totals();
        for (Map.Entry<UUID, Integer> entry : total.entrySet()) {
            Bank.setOpenStack(entry.getKey(), accountNames.getOrDefault(entry.getKey(), "?"),
                    entry.getValue());
        }
        // an account that has left every table has to be reported as nothing left, or the launcher would
        // hand out a stack that was cashed in ten minutes ago
        for (UUID account : new ArrayList<>(accountNames.keySet())) {
            if (total.containsKey(account)) continue;
            Bank.setOpenStack(account, accountNames.get(account), 0);
        }
    }

    private static Map<UUID, Integer> totals() {
        Map<UUID, Integer> total = new HashMap<>();
        for (Map<UUID, Integer> ofTable : stacksByTable.values()) {
            for (Map.Entry<UUID, Integer> entry : ofTable.entrySet()) {
                if (entry.getValue() <= 0) continue;
                total.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }
        return total;
    }

    /**
     * @param account whose chips
     * @return what they have on the tables right now
     */
    public static int stackOf(UUID account) {
        return totals().getOrDefault(account, 0);
    }

    /**
     * Closes the house: every table is settled, every chip is paid out, everything drawn is taken away.
     * <p>
     * The launcher would hand the stacks back anyway when the night is settled. Doing it here means that in
     * the ordinary case - the night ends and the server stops - nobody has to wait for that.
     */
    public static void close() {
        if (closed) return;
        closed = true;
        for (CasinoTable table : tables) {
            table.close();
        }
        seatedAt.clear();
        Map<UUID, Bank.StackEntry> left = new HashMap<>();
        for (Map.Entry<UUID, Integer> entry : totals().entrySet()) {
            left.put(entry.getKey(), new Bank.StackEntry(
                    accountNames.getOrDefault(entry.getKey(), "?"), entry.getValue()));
        }
        if (!left.isEmpty()) Bank.payOutEverything(left);
        stacksByTable.clear();
        tables.clear();
    }

    public static boolean isClosed() {
        return closed;
    }
}

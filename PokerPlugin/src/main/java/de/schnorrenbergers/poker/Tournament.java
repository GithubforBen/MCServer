package de.schnorrenbergers.poker;

import de.hems.types.event.PokerEventSettings;
import de.schnorrenbergers.poker.bank.Bank;
import de.schnorrenbergers.poker.game.PokerPlayer;
import de.schnorrenbergers.poker.game.PokerTable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The other kind of poker night: one buy-in each, blinds that climb, and only the last few get paid.
 * <p>
 * <b>Chips are not money here, and that changes everything about the bookkeeping.</b> In a cash game a chip
 * is a bit and standing up hands it back; in a tournament a chip is a position in a race, and the money is a
 * pot on the side that is only divided at the end. So a tournament must never cash chips out - a player with
 * a huge stack who logs off has won nothing yet - and the launcher must never be told those chips are an
 * open stack, or a crash would pay everybody out their chip count as bits and the night would have printed
 * money.
 * <p>
 * What is reported instead is the entry: while the tournament is unfinished, everybody who paid to be in it
 * is owed their buy-in back. That is what makes a crash survivable here - the evening is lost and nobody is
 * out of pocket - and it is the only honest answer, because a tournament that stopped in the middle has no
 * result to pay.
 * <p>
 * <b>One table.</b> Moving players between tables as seats empty is what a multi-table tournament is, and
 * it is a whole system of its own - breaking tables, balancing seats, a bubble across four rooms. So a
 * tournament here is one table of {@code seats} players, and the rest of the casino stays shut for the
 * evening. A cash game uses every table.
 */
public final class Tournament {

    /** How the prize pool is divided, by how many people were in it. */
    private static final int[][] PAYOUTS = {
            {100},              // two or three players: winner takes it
            {70, 30},           // from four
            {50, 30, 20}};      // from seven

    private final Plugin plugin;
    private final PokerEventSettings settings;
    /** Everybody who paid to be in, in the order they registered. */
    private final Map<UUID, String> entrants = new LinkedHashMap<>();
    /** Who went out when, worst first, so the order can be reversed into places at the end. */
    private final List<UUID> knockedOut = new ArrayList<>();

    private boolean running;
    private boolean finished;
    private int level = 1;
    private long nextLevelAt;

    public Tournament(Plugin plugin, PokerEventSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    /**
     * @return whether people can still buy in
     */
    public boolean isOpen() {
        return !running && !finished;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isFinished() {
        return finished;
    }

    public int getLevel() {
        return level;
    }

    public int getEntrantCount() {
        return entrants.size();
    }

    /**
     * @return what there is to play for
     */
    public int getPrizePool() {
        int entries = entrants.size() * settings.getBuyIn();
        // no rake off the pots in a tournament - the house takes its share of the entries instead, once,
        // which is how a tournament is charged for and also the only way that works when the chips in the
        // pots are not money
        return entries - houseCut();
    }

    public int houseCut() {
        return entrants.size() * settings.getBuyIn() * settings.getRakePermille() / 1000;
    }

    /**
     * Writes somebody into the tournament. Their buy-in is already paid at this point.
     *
     * @param player who is in
     */
    public void register(Player player) {
        entrants.put(player.getUniqueId(), player.getName());
        report();
        announce(Component.text(player.getName() + " ist dabei (" + entrants.size()
                + " Spieler, " + getPrizePool() + " Bits im Topf).", NamedTextColor.GRAY));
    }

    /**
     * Starts it, which shuts registration.
     *
     * @param now the current time
     */
    public void start(long now) {
        if (running || finished) return;
        running = true;
        level = 1;
        nextLevelAt = now + settings.getBlindUpMinutes() * 60_000L;
        announce(Component.text("Das Turnier läuft: " + entrants.size() + " Spieler, "
                + getPrizePool() + " Bits im Topf.", NamedTextColor.GOLD));
        announce(Component.text("Die Blinds steigen alle " + settings.getBlindUpMinutes()
                + " Minuten.", NamedTextColor.GRAY));
    }

    /**
     * Raises the blinds when their time is up.
     *
     * @param table the one table
     * @param now   the current time
     */
    public void tick(PokerTable table, long now) {
        if (!running || finished || now < nextLevelAt) return;
        // between hands only: raising the blinds under a hand that has already been dealt would change the
        // price of a bet somebody has already made
        if (table.isHandRunning()) return;
        level++;
        nextLevelAt = now + settings.getBlindUpMinutes() * 60_000L;
        int small = blindOf(level);
        table.getRules().setSmallBlind(small);
        announce(Component.text("Level " + level + ": Blinds " + small + "/" + (small * 2),
                NamedTextColor.YELLOW));
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 0.8f);
        }
    }

    /**
     * The blind at a given level.
     * <p>
     * Half again per level, rounded to something a person can add up. A doubling schedule ends a tournament
     * in five levels; half again gives it an evening.
     *
     * @param level which level
     * @return the small blind
     */
    public int blindOf(int level) {
        double small = settings.getSmallBlind();
        for (int step = 1; step < level; step++) small *= 1.5d;
        // rounded to something round, so nobody has to work out what a call of 337 costs
        int rounded = (int) Math.round(small);
        if (rounded > 100) return (rounded + 24) / 25 * 25;
        if (rounded > 20) return (rounded + 4) / 5 * 5;
        return Math.max(1, rounded);
    }

    /**
     * @return when the blinds go up next, in millis from now
     */
    public long millisToNextLevel() {
        return Math.max(0, nextLevelAt - System.currentTimeMillis());
    }

    /**
     * Somebody ran out of chips.
     *
     * @param player who is out
     */
    public void knockOut(PokerPlayer player) {
        if (player.isBot()) return;
        if (knockedOut.contains(player.getAccount())) return;
        knockedOut.add(player.getAccount());
        int place = entrants.size() - knockedOut.size() + 1;
        Player online = Bukkit.getPlayer(player.getId());
        if (online != null) {
            online.sendMessage(Component.text("Du bist raus - Platz " + place + " von "
                    + entrants.size() + ".", NamedTextColor.YELLOW));
        }
        announce(Component.text(player.getName() + " ist auf Platz " + place + " raus.",
                NamedTextColor.GRAY));
    }

    /**
     * Ends it and divides the pot.
     *
     * @param winner who was left, or {@code null} if nobody was
     */
    public void finish(PokerPlayer winner) {
        if (finished) return;
        finished = true;
        running = false;

        List<UUID> places = new ArrayList<>();
        if (winner != null && !winner.isBot()) places.add(winner.getAccount());
        // the knockouts are in the order they happened, so the last one out is the runner up
        for (int i = knockedOut.size() - 1; i >= 0; i--) {
            UUID out = knockedOut.get(i);
            if (!places.contains(out)) places.add(out);
        }

        int pool = getPrizePool();
        int[] split = splitFor(entrants.size());
        int handedOut = 0;
        for (int i = 0; i < split.length && i < places.size(); i++) {
            int share = pool * split[i] / 100;
            if (i == split.length - 1 || i == places.size() - 1) {
                // the last paid place takes the rounding, so the pool is handed out to the last bit
                share = pool - handedOut;
            }
            UUID account = places.get(i);
            String name = entrants.getOrDefault(account, "?");
            Bank.cashOut(account, name, share, "Poker: Turnier Platz " + (i + 1));
            handedOut += share;
            announce(Component.text("Platz " + (i + 1) + ": " + name + " - " + share + " Bits",
                    NamedTextColor.GOLD));
            Player online = Bukkit.getPlayer(account);
            if (online != null) {
                online.playSound(online.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }
        }
        // and the entries stop being owed back, because the tournament has a result now
        for (Map.Entry<UUID, String> entrant : entrants.entrySet()) {
            Bank.setOpenStack(entrant.getKey(), entrant.getValue(), 0);
        }
        plugin.getLogger().info("Tournament finished: " + handedOut + " bits paid out of a pool of "
                + pool + ", house kept " + houseCut() + ".");
    }

    /**
     * @param players how many were in it
     * @return how the pool is divided, as percentages
     */
    private static int[] splitFor(int players) {
        if (players >= 7) return PAYOUTS[2];
        if (players >= 4) return PAYOUTS[1];
        return PAYOUTS[0];
    }

    /**
     * Tells the launcher that everybody who is in is owed their entry back.
     * <p>
     * This is the whole crash story of a tournament. There is no result until it ends, so the only thing
     * that can honestly be owed is what people paid to get in - and that is what the launcher hands back if
     * this server never comes up again.
     */
    private void report() {
        if (finished) return;
        for (Map.Entry<UUID, String> entrant : entrants.entrySet()) {
            Bank.setOpenStack(entrant.getKey(), entrant.getValue(), settings.getBuyIn());
        }
    }

    private void announce(Component message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }
}

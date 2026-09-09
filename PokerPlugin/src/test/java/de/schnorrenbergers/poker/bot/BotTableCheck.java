package de.schnorrenbergers.poker.bot;

import de.schnorrenbergers.poker.game.Action;
import de.schnorrenbergers.poker.game.ActionType;
import de.schnorrenbergers.poker.game.Card;
import de.schnorrenbergers.poker.game.HandValue;
import de.schnorrenbergers.poker.game.PokerPlayer;
import de.schnorrenbergers.poker.game.PokerTable;
import de.schnorrenbergers.poker.game.RakePolicy;
import de.schnorrenbergers.poker.game.Street;
import de.schnorrenbergers.poker.game.TableEvents;
import de.schnorrenbergers.poker.game.TableRules;

import java.util.*;

/** Sits six bots down and watches how they play. */
/**
 * Sits six bots down together and reports how they played: how often they put money in before the flop,
 * how often they went all-in, how big the pots got and what the house took.
 * <p>
 * There is no pass or fail here - it is a measurement, and it is in the repository because the numbers are
 * the argument. What it is looking for is a table that is neither everybody folding nor everybody shoving:
 * roughly a third of hands played, all-ins in the low tens over fifteen hundred hands, and pots measured in
 * tens of big blinds rather than hundreds.
 * <pre>
 * mvn -q -pl PokerPlugin -am install -DskipTests
 * java -cp PokerPlugin/target/classes:PokerPlugin/target/test-classes \\
 *      de.schnorrenbergers.poker.bot.BotTableCheck 1500 30
 * </pre>
 */
public final class BotTableCheck {
    public static void main(String[] args) {
        int hands = Integer.parseInt(args.length > 0 ? args[0] : "1500");
        int rakePermille = Integer.parseInt(args.length > 1 ? args[1] : "30");
        int sb = 10, bb = 20, buyIn = 2000;

        final int[] raked = {0};
        final int[] handsPlayed = {0};
        final long[] potTotal = {0};
        final int[] showdowns = {0};
        final Map<String, int[]> stats = new HashMap<>(); // name -> [vpip, allin, folds, wins]
        RakePolicy rake = (pot, contested) -> contested ? Math.min(pot * rakePermille / 1000, 50 * bb) : 0;

        PokerTable table = new PokerTable(1, 6, new TableRules(sb, 30, false, rake), new TableEvents() {
            public void onPotAwarded(PokerTable t, PokerPlayer w, int a, int r, HandValue v) {
                raked[0] += r;
                stats.computeIfAbsent(w.getName(), k -> new int[4])[3]++;
            }
            public void onHandStarted(PokerTable t, int n) { handsPlayed[0]++; }
            public void onHandEnded(PokerTable t) { potTotal[0] += t.getLastPot(); }
            public void onShowdown(PokerTable t, java.util.List<PokerTable.ShowdownEntry> e) { showdowns[0]++; }
        });

        Random random = new Random(7);
        Map<PokerPlayer, BotBrain> brains = new LinkedHashMap<>();
        List<PokerPlayer> players = new ArrayList<>();
        int totalBoughtIn = 0;
        for (int i = 0; i < 6; i++) {
            PokerPlayer bot = new PokerPlayer(UUID.randomUUID(), "Bot" + i, UUID.randomUUID(), true, buyIn);
            totalBoughtIn += buyIn;
            players.add(bot);
            brains.put(bot, new BotBrain(new Random(random.nextLong())));
            table.sitDown(bot, i);
            stats.put(bot.getName(), new int[4]);
        }

        long now = 0;
        Set<PokerPlayer> putMoneyIn = new HashSet<>();
        int rebuys = 0;
        for (int hand = 0; hand < hands; hand++) {
            // a bot that has run dry buys back in, the way a cash game works
            for (PokerPlayer bot : players) {
                if (bot.getChips() < bb) { bot.addChips(buyIn); rebuys++; }
            }
            now += 20_000;
            table.tick(now);
            if (!table.isHandRunning()) { now += 20_000; table.tick(now); }
            if (!table.isHandRunning()) continue;

            putMoneyIn.clear();
            int guard = 0;
            Street lastStreet = table.getStreet();
            while (table.isHandRunning() && guard++ < 400) {
                PokerPlayer acting = table.getActing();
                if (acting == null) break;
                int before = acting.getCommittedTotal();
                boolean preflop = table.getStreet() == Street.PREFLOP;
                Action action = brains.get(acting).decide(table, acting);
                int[] s = stats.get(acting.getName());
                if (action.type() == ActionType.FOLD) s[2]++;
                if (action.type() == ActionType.ALL_IN) s[1]++;
                if (preflop && (action.type() == ActionType.CALL || action.type() == ActionType.RAISE
                        || action.type() == ActionType.BET || action.type() == ActionType.ALL_IN)) {
                    if (putMoneyIn.add(acting)) s[0]++;
                }
                if (!table.act(acting, action, now)) {
                    // an illegal decision would be a bug in the brain, and the table must never accept one
                    System.out.println("ILLEGAL: " + acting.getName() + " " + action.type()
                            + " " + action.amount() + " toCall=" + table.toCall(acting)
                            + " min=" + table.minRaiseTo(acting) + " max=" + table.maxRaiseTo(acting));
                    table.act(acting, table.toCall(acting) > 0 ? Action.fold() : Action.check(), now);
                }
            }
            if (guard >= 400) { System.out.println("HAND HING BEI " + hand); break; }
        }

        int end = players.stream().mapToInt(PokerPlayer::getChips).sum();
        int broughtIn = totalBoughtIn + rebuys * buyIn;
        System.out.println("Hände gespielt: " + handsPlayed[0] + ", Rebuys: " + rebuys);
        System.out.println("Eingekauft " + broughtIn + ", am Tisch " + end + ", Haus " + raked[0]
                + " -> Bilanz " + (broughtIn - end - raked[0]));
        System.out.println();
        System.out.printf("%-6s %8s %8s %8s %8s %8s%n", "Bot", "Stack", "VPIP", "All-In", "Folds", "Pots");
        for (PokerPlayer bot : players) {
            int[] s = stats.get(bot.getName());
            System.out.printf("%-6s %8d %7.1f%% %8d %8d %8d%n", bot.getName(), bot.getChips(),
                    100.0 * s[0] / Math.max(1, handsPlayed[0]), s[1], s[2], s[3]);
        }
        System.out.println();
        double avgPot = (double) potTotal[0] / Math.max(1, handsPlayed[0]);
        System.out.printf("%nDurchschnittspot %.0f Chips = %.1f Big Blinds, Showdowns %d (%.0f%% der Haende)%n",
                avgPot, avgPot / bb, showdowns[0], 100.0 * showdowns[0] / Math.max(1, handsPlayed[0]));
        System.out.printf("Haus nahm %.1f%% des Eingekauften, %.2f BB pro Hand%n",
                100.0 * raked[0] / Math.max(1, broughtIn), (double) raked[0] / Math.max(1, handsPlayed[0]) / bb);
    }
}

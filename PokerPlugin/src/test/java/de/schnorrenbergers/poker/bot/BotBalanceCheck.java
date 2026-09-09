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

/**
 * Sits the bot down against the three opponents that expose the two ways a poker bot fails.
 * A bot that folds too much is free money for the maniac; a bot that calls too much is free money
 * for the nit. A usable bot beats the maniac and the station and roughly holds its own against the nit.
 */
/**
 * The measurement the bots exist to pass.
 * <p>
 * A poker bot fails in one of two directions, and both of them ruin a table. One folds too much: free money
 * for anybody willing to bet, and no fun to play against. The other calls too much: free money for anybody
 * willing to wait for aces, and it feels like being robbed. Neither is caught by looking at the code, so it
 * is measured instead - against a maniac that moves in every hand, a nit that only plays premium hands, and
 * a station that never folds.
 * <p>
 * What a usable bot does: beats the maniac clearly, beats the station clearly, and comes out of the nit
 * roughly level. Losing badly to the nit is the failure that matters most - it means the bot is calling off
 * stacks against hands that are always ahead of it.
 * <pre>
 * java -cp PokerPlugin/target/classes:PokerPlugin/target/test-classes \\
 *      de.schnorrenbergers.poker.bot.BotBalanceCheck 4000
 * </pre>
 */
public final class BotBalanceCheck {

    interface Strategy { Action decide(PokerTable t, PokerPlayer me); }

    /** Puts everything in, every time. */
    static Strategy maniac() {
        return (t, me) -> {
            int min = t.minRaiseTo(me);
            if (min > 0) return Action.allIn(0);
            int toCall = t.toCall(me);
            return toCall > 0 ? Action.call(toCall) : Action.check();
        };
    }

    /** Only ever plays a big pair or two big cards, and shoves them. */
    static Strategy nit() {
        return (t, me) -> {
            List<Card> hole = me.getHole();
            int toCall = t.toCall(me);
            boolean pair = hole.get(0).rank() == hole.get(1).rank();
            int high = Math.max(hole.get(0).value(), hole.get(1).value());
            int low = Math.min(hole.get(0).value(), hole.get(1).value());
            boolean premium = (pair && high >= 10) || (high >= 13 && low >= 12);
            if (!premium) return toCall > 0 ? Action.fold() : Action.check();
            int min = t.minRaiseTo(me);
            if (min > 0) return Action.allIn(0);
            return toCall > 0 ? Action.call(toCall) : Action.check();
        };
    }

    /** Never folds, never raises. */
    static Strategy station() {
        return (t, me) -> {
            int toCall = t.toCall(me);
            return toCall > 0 ? Action.call(toCall) : Action.check();
        };
    }

    public static void main(String[] args) {
        int hands = Integer.parseInt(args.length > 0 ? args[0] : "3000");
        run("Bot gegen Maniac (immer All-In)", maniac(), hands);
        run("Bot gegen Nit (nur Premiumhaende)", nit(), hands);
        run("Bot gegen Calling Station (geht immer mit)", station(), hands);
    }

    static void run(String title, Strategy other, int hands) {
        int sb = 10, bb = 20, buyIn = 2000;
        RakePolicy rake = RakePolicy.NONE;   // measured without the house, to see the play alone
        PokerTable table = new PokerTable(1, 4, new TableRules(sb, 30, false, rake), new TableEvents() {});

        Random random = new Random(11);
        List<PokerPlayer> bots = new ArrayList<>();
        List<PokerPlayer> others = new ArrayList<>();
        Map<PokerPlayer, BotBrain> brains = new HashMap<>();
        Map<PokerPlayer, Strategy> strategies = new HashMap<>();

        for (int i = 0; i < 4; i++) {
            boolean isBot = i % 2 == 0;
            PokerPlayer p = new PokerPlayer(UUID.randomUUID(), (isBot ? "Bot" : "Geg") + i,
                    UUID.randomUUID(), true, buyIn);
            table.sitDown(p, i);
            if (isBot) { bots.add(p); brains.put(p, new BotBrain(new Random(random.nextLong()))); }
            else { others.add(p); strategies.put(p, other); }
        }

        int[] boughtBot = {bots.size() * buyIn};
        int[] boughtOther = {others.size() * buyIn};
        long now = 0;
        for (int hand = 0; hand < hands; hand++) {
            for (PokerPlayer p : bots) if (p.getChips() < bb) { p.addChips(buyIn); boughtBot[0] += buyIn; }
            for (PokerPlayer p : others) if (p.getChips() < bb) { p.addChips(buyIn); boughtOther[0] += buyIn; }
            now += 20_000;
            table.tick(now);
            if (!table.isHandRunning()) { now += 20_000; table.tick(now); }
            if (!table.isHandRunning()) continue;
            int guard = 0;
            while (table.isHandRunning() && guard++ < 400) {
                PokerPlayer acting = table.getActing();
                if (acting == null) break;
                Action action = brains.containsKey(acting)
                        ? brains.get(acting).decide(table, acting)
                        : strategies.get(acting).decide(table, acting);
                if (!table.act(acting, action, now)) {
                    table.act(acting, table.toCall(acting) > 0 ? Action.fold() : Action.check(), now);
                }
            }
        }
        int botEnd = bots.stream().mapToInt(PokerPlayer::getChips).sum();
        int otherEnd = others.stream().mapToInt(PokerPlayer::getChips).sum();
        double perHandBB = (double) (botEnd - boughtBot[0]) / hands / bb;
        System.out.printf("%-42s Bot %+7d (%+.3f BB/Hand), Gegner %+7d%n",
                title, botEnd - boughtBot[0], perHandBB, otherEnd - boughtOther[0]);
    }
}

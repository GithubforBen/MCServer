package de.schnorrenbergers.poker.bot;

import de.schnorrenbergers.poker.game.Action;
import de.schnorrenbergers.poker.game.ActionType;
import de.schnorrenbergers.poker.game.Card;
import de.schnorrenbergers.poker.game.PokerPlayer;
import de.schnorrenbergers.poker.game.PokerTable;
import de.schnorrenbergers.poker.game.RakePolicy;
import de.schnorrenbergers.poker.game.Rank;
import de.schnorrenbergers.poker.game.Suit;
import de.schnorrenbergers.poker.game.TableEvents;
import de.schnorrenbergers.poker.game.TableRules;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Measures the thing {@link BotVibe} exists for: that a bot cannot be learned.
 * <p>
 * Balance is measured elsewhere ({@link BotBalanceCheck}). This asks a different question, and it is the
 * one a player actually experiences: can I work out what this opponent does and then beat it forever? Three
 * measurements, because being unreadable has three separate parts:
 * <ol>
 *   <li><b>Spread.</b> Are two bots different players, or the same player twice? Reported as the range of
 *       the derived traits across a hundred draws. If they cluster into two or three groups, the whole idea
 *       has failed and a player only has to learn three opponents.</li>
 *   <li><b>Wobble.</b> Put one bot in the same spot two hundred times. If it always answers the same way,
 *       two hands of watching are enough to know it.</li>
 *   <li><b>Drift.</b> Is a read still worth anything later? Its temperament is measured, then a bad evening
 *       is put through it, then measured again.</li>
 * </ol>
 * There is no pass or fail - it is a report, and the numbers are the argument.
 * <pre>
 * mvn -q -pl PokerPlugin -am install -DskipTests
 * java -cp PokerPlugin/target/classes:PokerPlugin/target/test-classes \
 *      de.schnorrenbergers.poker.bot.BotCharacterCheck
 * </pre>
 */
public final class BotCharacterCheck {

    public static void main(String[] args) {
        spread();
        System.out.println();
        wobble();
        System.out.println();
        drift();
    }

    /* ------------------------------------------------------------------ 1. spread */

    /**
     * Draws a hundred temperaments and reports how far apart they are.
     */
    private static void spread() {
        Random random = new Random(5);
        int draws = 100;
        double[] edges = new double[draws];
        double[] raises = new double[draws];
        double[] bluffs = new double[draws];
        for (int i = 0; i < draws; i++) {
            BotVibe vibe = new BotVibe(new Random(random.nextLong()));
            edges[i] = vibe.demandedEdge();
            raises[i] = vibe.raiseRate();
            bluffs[i] = vibe.bluffRate();
        }
        System.out.println("1. Spread über " + draws + " gezogene Charaktere");
        report("  Verlangter Vorsprung", edges);
        report("  Erhöhungsrate      ", raises);
        report("  Bluffrate          ", bluffs);
        System.out.println("  Verschiedene Werte: " + distinct(edges) + " von " + draws
                + " (feste Typen wären eine Handvoll)");
    }

    private static void report(String label, double[] values) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        double sum = 0;
        for (double value : values) {
            min = Math.min(min, value);
            max = Math.max(max, value);
            sum += value;
        }
        double mean = sum / values.length;
        double variance = 0;
        for (double value : values) variance += (value - mean) * (value - mean);
        System.out.printf("%s  %.3f bis %.3f, Mittel %.3f, Streuung %.3f%n",
                label, min, max, mean, Math.sqrt(variance / values.length));
    }

    private static int distinct(double[] values) {
        java.util.Set<Long> seen = new java.util.HashSet<>();
        // rounded to three decimals, because two values that differ in the fifth are the same opponent
        for (double value : values) seen.add(Math.round(value * 1000));
        return seen.size();
    }

    /* ------------------------------------------------------------------ 2. wobble */

    /**
     * Asks one bot the same question two hundred times.
     */
    private static void wobble() {
        System.out.println("2. Dieselbe Situation, 200 mal gefragt");
        System.out.println("   Drei Hände. Bei Müll ist Passen immer richtig - dass der Bot das immer tut,");
        System.out.println("   ist kein Muster, sondern richtiges Spiel. Interessant sind die Grenzhand");
        System.out.println("   und die Monsterhand: dort muss gemischt werden, sonst verrät jede");
        System.out.println("   Erhöhung, was er hält.");
        for (int who = 0; who < 3; who++) {
            Random random = new Random(100 + who);
            BotVibe vibe = new BotVibe(random);
            BotBrain brain = new BotBrain(random, vibe);
            System.out.printf("  Bot %d (%s)%n", who + 1, vibe);
            probe(brain, "Müll     (7-2 offsuit)",
                    new Card(Rank.SEVEN, Suit.CLUBS), new Card(Rank.TWO, Suit.DIAMONDS));
            probe(brain, "Grenze   (10-9 suited)",
                    new Card(Rank.TEN, Suit.SPADES), new Card(Rank.NINE, Suit.SPADES));
            probe(brain, "Monster  (Asse)",
                    new Card(Rank.ACE, Suit.CLUBS), new Card(Rank.ACE, Suit.DIAMONDS));
        }
    }

    /**
     * Puts one bot in one spot two hundred times and reports what came back.
     */
    private static void probe(BotBrain brain, String label, Card first, Card second) {
        PokerTable table = new PokerTable(1, 3, new TableRules(10, 30, false, RakePolicy.NONE),
                new TableEvents() {
                });
        PokerPlayer me = seat(table, "Ich", 2000, 0);
        seat(table, "A", 2000, 1);
        seat(table, "B", 2000, 2);
        table.startHand(0L);
        drive(table, 0L);
        me.setHole(first, second);

        Map<ActionType, Integer> counts = new EnumMap<>(ActionType.class);
        for (int i = 0; i < 200; i++) {
            counts.merge(brain.decide(table, me).type(), 1, Integer::sum);
        }
        StringBuilder line = new StringBuilder();
        for (Map.Entry<ActionType, Integer> entry : counts.entrySet()) {
            line.append(entry.getKey()).append(' ').append(entry.getValue() / 2).append("%  ");
        }
        System.out.printf("    %-34s %s%s%n", label, line,
                counts.size() == 1 ? "<- immer dasselbe" : "<- gemischt");
    }

    /** Puts the table into a spot where somebody has bet and it is our turn. */
    private static void drive(PokerTable table, long now) {
        int guard = 0;
        while (table.isHandRunning() && guard++ < 20) {
            PokerPlayer acting = table.getActing();
            if (acting == null) break;
            if (acting.getName().equals("Ich")) {
                if (table.toCall(acting) > 0) return;
                table.act(acting, Action.check(), now);
                continue;
            }
            int min = table.minRaiseTo(acting);
            if (min > 0 && table.getCurrentBet() < 60) {
                table.act(acting, Action.raise(Math.min(min + 20, table.maxRaiseTo(acting))), now);
            } else {
                table.act(acting, table.toCall(acting) > 0
                        ? Action.call(table.toCall(acting)) : Action.check(), now);
            }
        }
    }

    /* ------------------------------------------------------------------ 3. drift */

    /**
     * Measures a temperament, puts a bad evening through it, and measures again.
     */
    private static void drift() {
        System.out.println("3. Drift: derselbe Bot vor und nach einem schlechten Abend");
        Random random = new Random(9);
        for (int who = 0; who < 3; who++) {
            BotVibe vibe = new BotVibe(new Random(random.nextLong()));
            double edgeBefore = average(vibe, 60);
            String before = vibe.toString();
            // two hundred hands, two thirds of them losing a quarter of the stack
            for (int hand = 0; hand < 200; hand++) {
                boolean lost = hand % 3 != 0;
                vibe.afterHand(!lost, 2000, lost ? 1400 : 2700);
            }
            double edgeAfter = average(vibe, 60);
            System.out.printf("  Bot %d%n    vorher: %s -> Vorsprung %.3f%n    nachher: %s -> Vorsprung %.3f"
                            + "  (%+.1f%%)%n",
                    who + 1, before, edgeBefore, vibe, edgeAfter,
                    100 * (edgeAfter - edgeBefore) / edgeBefore);
        }
        System.out.println("  Ein Read von vor 200 Händen beschreibt einen anderen Gegner.");
    }

    private static double average(BotVibe vibe, int samples) {
        double sum = 0;
        for (int i = 0; i < samples; i++) sum += vibe.demandedEdge();
        return sum / samples;
    }

    private static PokerPlayer seat(PokerTable table, String name, int chips, int at) {
        PokerPlayer player = new PokerPlayer(UUID.randomUUID(), name, UUID.randomUUID(), true, chips);
        table.sitDown(player, at);
        return player;
    }
}

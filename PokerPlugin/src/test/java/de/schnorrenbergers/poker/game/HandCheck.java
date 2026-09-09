package de.schnorrenbergers.poker.game;

import java.util.*;

/**
 * Checks the hand evaluator against the cases that are normally got wrong.
 * <p>
 * Not a unit test framework, on purpose: there is none in this repository and one class with a main is
 * something anybody can run. From the repository root:
 * <pre>
 * mvn -q -pl PokerPlugin -am install -DskipTests
 * java -cp PokerPlugin/target/classes:PokerPlugin/target/test-classes \\
 *      de.schnorrenbergers.poker.game.HandCheck
 * </pre>
 * It exits non-zero when something is wrong, so it can be hung into a build later.
 */
public final class HandCheck {
    static int failed = 0, passed = 0;

    static List<Card> parse(String spec) {
        List<Card> cards = new ArrayList<>();
        for (String token : spec.split(" ")) {
            String rankPart = token.substring(0, token.length() - 1);
            char suitChar = token.charAt(token.length() - 1);
            Rank rank = switch (rankPart) {
                case "A" -> Rank.ACE; case "K" -> Rank.KING; case "Q" -> Rank.QUEEN;
                case "J" -> Rank.JACK; case "T" -> Rank.TEN;
                default -> Rank.ofValue(Integer.parseInt(rankPart));
            };
            Suit suit = switch (suitChar) {
                case 's' -> Suit.SPADES; case 'h' -> Suit.HEARTS;
                case 'd' -> Suit.DIAMONDS; default -> Suit.CLUBS;
            };
            cards.add(new Card(rank, suit));
        }
        return cards;
    }

    static void cat(String spec, HandCategory expected) {
        HandValue v = HandEvaluator.evaluate(parse(spec));
        if (v.getCategory() != expected) {
            System.out.println("FAIL cat " + spec + " -> " + v.getCategory() + " expected " + expected);
            failed++;
        } else { passed++; }
        if (v.getBest().size() != 5) {
            System.out.println("FAIL size " + spec + " -> " + v.getBest().size() + " cards"); failed++;
        } else passed++;
    }

    static void beats(String a, String b) {
        HandValue va = HandEvaluator.evaluate(parse(a));
        HandValue vb = HandEvaluator.evaluate(parse(b));
        if (va.compareTo(vb) <= 0) {
            System.out.println("FAIL beats: " + a + " (" + va.describe() + ") should beat "
                + b + " (" + vb.describe() + ")");
            failed++;
        } else passed++;
    }

    static void ties(String a, String b) {
        HandValue va = HandEvaluator.evaluate(parse(a));
        HandValue vb = HandEvaluator.evaluate(parse(b));
        if (va.compareTo(vb) != 0) {
            System.out.println("FAIL tie: " + a + " (" + va.describe() + ") vs "
                + b + " (" + vb.describe() + ")");
            failed++;
        } else passed++;
    }

    public static void main(String[] args) {
        // categories, seven cards each, the way a real hold'em hand arrives
        cat("As Ks Qs Js Ts 2h 3d", HandCategory.STRAIGHT_FLUSH);
        cat("5s 4s 3s 2s As Kh Qd", HandCategory.STRAIGHT_FLUSH);   // the wheel, suited
        cat("9c 9d 9h 9s 2c 3d 4h", HandCategory.FOUR_OF_A_KIND);
        cat("9c 9d 9h Kc Kd 4h 2s", HandCategory.FULL_HOUSE);
        cat("9c 9d 9h 8c 8d 8h 2s", HandCategory.FULL_HOUSE);       // two trips is a full house
        cat("As Js 9s 6s 3s Kh Qd", HandCategory.FLUSH);
        cat("5c 4d 3h 2s Ac Kh Qd", HandCategory.STRAIGHT);         // the wheel, unsuited
        cat("Tc 9d 8h 7s 6c 2h 3d", HandCategory.STRAIGHT);
        cat("7c 7d 7h Kc 9d 4h 2s", HandCategory.THREE_OF_A_KIND);
        cat("7c 7d Kc Kd 9h 4s 2c", HandCategory.TWO_PAIR);
        cat("7c 7d Kc 9d 5h 4s 2c", HandCategory.PAIR);
        cat("Ac Kd 9h 7s 5c 3d 2h", HandCategory.HIGH_CARD);
        // three pairs with seven cards is still two pair, and the top two count
        HandValue threePair = HandEvaluator.evaluate(parse("Ac Ad Kc Kd 9h 9s 2c"));
        if (threePair.getCategory() != HandCategory.TWO_PAIR
                || !threePair.describe().contains("Asse") || !threePair.describe().contains("Könige")) {
            System.out.println("FAIL three pairs -> " + threePair.describe()); failed++;
        } else passed++;

        // the order of the hands
        beats("As Ks Qs Js Ts 2h 3d", "9c 9d 9h 9s 2c 3d 4h");
        beats("9c 9d 9h 9s 2c 3d 4h", "9c 9d 9h Kc Kd 4h 2s");
        beats("9c 9d 9h Kc Kd 4h 2s", "As Js 9s 6s 3s Kh Qd");
        beats("As Js 9s 6s 3s Kh Qd", "Tc 9d 8h 7s 6c 2h 3d");
        beats("Tc 9d 8h 7s 6c 2h 3d", "7c 7d 7h Kc 9d 4h 2s");
        beats("7c 7d 7h Kc 9d 4h 2s", "7c 7d Kc Kd 9h 4s 2c");
        beats("7c 7d Kc Kd 9h 4s 2c", "7c 7d Kc 9d 5h 4s 2c");
        beats("7c 7d Kc 9d 5h 4s 2c", "Ac Kd 9h 7s 5c 3d 2h");

        // kickers
        beats("Ac Ad Kc 9d 5h 4s 2c", "Ac Ad Qc 9d 5h 4s 2c");     // pair, better kicker
        beats("Ac Ad Kc Kd 9h 4s 2c", "Ac Ad Qc Qd 9h 4s 2c");     // two pair, better second pair
        beats("Ac Ad Kc Kd Jh 4s 2c", "Ac Ad Kc Kd 9h 4s 2c");     // two pair, better kicker
        beats("Ts 9s 8s 7s 6s 2h 3d", "9s 8s 7s 6s 5s 2h 3d");     // higher straight flush
        beats("6c 5d 4h 3s 2c Kh Qd", "5c 4d 3h 2s Ac Kh Qd");     // six-high beats the wheel
        beats("As Ks Js 9s 3s 2h 4d", "Ks Qs Js 9s 3s 2h 4d");     // higher flush

        // ties: the board plays, and two identical hands split
        ties("Ac Kd 2h 3s 4c 5d 9h", "Ah Ks 2h 3s 4c 5d 9h");
        ties("2c 3d Ah Kh Qh Jh Th", "2s 3h Ah Kh Qh Jh Th");      // both play the royal on the board

        // an ace-high straight is not a wheel, and A-2-3-4 is not a straight at all
        HandValue notStraight = HandEvaluator.evaluate(parse("Ac 2d 3h 4s 9c Kh Qd"));
        if (notStraight.getCategory() == HandCategory.STRAIGHT) {
            System.out.println("FAIL A-2-3-4 counted as a straight"); failed++;
        } else passed++;

        // five card input, which is what a showdown of the board alone looks like
        cat("Ac Kd 9h 7s 5c", HandCategory.HIGH_CARD);
        cat("Ac Ad 9h 7s 5c", HandCategory.PAIR);

        // every hand out of a full deck evaluates without throwing, and always to five cards
        Deck deck = new Deck();
        for (int i = 0; i < 5000; i++) {
            Deck fresh = new Deck();
            List<Card> seven = new ArrayList<>();
            for (int c = 0; c < 7; c++) seven.add(fresh.draw());
            HandValue v = HandEvaluator.evaluate(seven);
            if (v.getBest().size() != 5) {
                System.out.println("FAIL random size " + seven + " -> " + v.getBest()); failed++; break;
            }
            if (new HashSet<>(v.getBest()).size() != 5) {
                System.out.println("FAIL duplicate card in best five: " + seven + " -> " + v.getBest());
                failed++; break;
            }
            if (!new HashSet<>(seven).containsAll(v.getBest())) {
                System.out.println("FAIL best five not from the hand: " + seven + " -> " + v.getBest());
                failed++; break;
            }
        }
        passed++;

        System.out.println(failed == 0 ? ("ALLE " + passed + " PRUEFUNGEN GRUEN") : (failed + " FEHLER"));
        if (failed > 0) System.exit(1);
    }
}

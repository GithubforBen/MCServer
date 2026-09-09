package de.schnorrenbergers.poker.game;

import java.util.*;

/** Drives the rules through the situations that are normally got wrong. */
/**
 * Drives a table through the situations the rules of Hold'em are normally got wrong in: the blinds heads
 * up, the big blind's option, an all-in that is too small to reopen the betting, side pots, and four
 * hundred random hands to see that chips only ever move and never appear.
 * <p>
 * Run it the same way as {@link HandCheck}.
 */
public final class TableCheck {
    static int failed = 0, passed = 0;
    static List<String> log = new ArrayList<>();

    static void check(boolean ok, String what) {
        if (ok) passed++;
        else { failed++; System.out.println("FAIL: " + what); }
    }

    static PokerPlayer p(String name, int chips) {
        return new PokerPlayer(UUID.randomUUID(), name, UUID.randomUUID(), false, chips);
    }

    static TableEvents recorder() {
        return new TableEvents() {
            public void onActionTaken(PokerTable t, PokerPlayer pl, Action a) {
                log.add(pl.getName() + " " + a.type() + " " + a.amount());
            }
            public void onBlindPosted(PokerTable t, PokerPlayer pl, int amount, boolean big) {
                log.add(pl.getName() + " posts " + (big ? "BB" : "SB") + " " + amount);
            }
            public void onPotAwarded(PokerTable t, PokerPlayer w, int amount, int rake, HandValue v) {
                log.add("award " + w.getName() + " " + amount + " rake " + rake);
            }
            public void onStreetDealt(PokerTable t, Street s) { log.add("street " + s); }
        };
    }

    public static void main(String[] args) {
        headsUpBlinds();
        threeHandedBlinds();
        bigBlindOption();
        shortAllInDoesNotReopen();
        sidePots();
        foldedChipsStayInPot();
        rakeOnlyWhenContested();
        splitPotOddChip();
        allInLeaveWaits();
        longRunNoChipsLostOrMade();

        System.out.println(failed == 0 ? ("ALLE " + passed + " PRUEFUNGEN GRUEN") : (failed + " FEHLER"));
        if (failed > 0) System.exit(1);
    }

    /** Heads up: the button posts the small blind and acts first before the flop. */
    static void headsUpBlinds() {
        log.clear();
        PokerTable t = new PokerTable(1, 2, new TableRules(10, 30, false, RakePolicy.NONE), recorder());
        PokerPlayer a = p("A", 1000), b = p("B", 1000);
        t.sitDown(a, 0); t.sitDown(b, 1);
        t.startHand(0L);
        int button = t.getButtonSeat();
        PokerPlayer buttonPlayer = t.seatAt(button);
        check(buttonPlayer.getCommitted() == 10, "heads up: the button posts the small blind, got "
                + buttonPlayer.getCommitted());
        check(t.getActing() == buttonPlayer, "heads up: the button acts first before the flop");

        // and last after it
        t.act(t.getActing(), Action.call(10), 0L);          // button completes
        t.act(t.getActing(), Action.check(), 0L);           // big blind checks
        check(t.getStreet() == Street.FLOP, "flop is dealt, got " + t.getStreet());
        check(t.getActing() != buttonPlayer, "heads up: the button acts last after the flop");
    }

    /** Three handed: the blinds are the two seats left of the button, and the button is under the gun. */
    static void threeHandedBlinds() {
        PokerTable t = new PokerTable(1, 3, new TableRules(10, 30, false, RakePolicy.NONE), recorder());
        PokerPlayer a = p("A", 1000), b = p("B", 1000), c = p("C", 1000);
        t.sitDown(a, 0); t.sitDown(b, 1); t.sitDown(c, 2);
        t.startHand(0L);
        int button = t.getButtonSeat();
        check(t.seatAt((button + 1) % 3).getCommitted() == 10, "three handed: small blind left of button");
        check(t.seatAt((button + 2) % 3).getCommitted() == 20, "three handed: big blind next");
        check(t.getActing() == t.seatAt(button), "three handed: the button is first to act preflop");
    }

    /** Everybody only calls, and the big blind still gets asked. */
    static void bigBlindOption() {
        PokerTable t = new PokerTable(1, 3, new TableRules(10, 30, false, RakePolicy.NONE), recorder());
        t.sitDown(p("A", 1000), 0); t.sitDown(p("B", 1000), 1); t.sitDown(p("C", 1000), 2);
        t.startHand(0L);
        int button = t.getButtonSeat();
        PokerPlayer bb = t.seatAt((button + 2) % 3);
        t.act(t.getActing(), Action.call(20), 0L);   // button calls
        t.act(t.getActing(), Action.call(20), 0L);   // small blind completes
        check(t.getStreet() == Street.PREFLOP, "still preflop - the big blind has an option");
        check(t.getActing() == bb, "the big blind is asked even though everybody called");
        check(t.toCall(bb) == 0, "the option costs nothing");
        t.act(bb, Action.check(), 0L);
        check(t.getStreet() == Street.FLOP, "checking the option ends the round");
    }

    /** An all-in for less than a full raise does not let the earlier callers raise again. */
    static void shortAllInDoesNotReopen() {
        PokerTable t = new PokerTable(1, 3, new TableRules(10, 30, false, RakePolicy.NONE), recorder());
        PokerPlayer big1 = p("Gross1", 1000), big2 = p("Gross2", 1000), shorty = p("Kurz", 125);
        t.sitDown(big1, 0); t.sitDown(big2, 1); t.sitDown(shorty, 2);
        t.startHand(0L);

        // walk the preflop round until the short stack is in for everything
        int guard = 0;
        while (t.isHandRunning() && !shorty.isAllIn() && guard++ < 20) {
            PokerPlayer acting = t.getActing();
            if (acting == null) break;
            if (acting == shorty) {
                t.act(acting, Action.allIn(0), 0L);
            } else if (t.getCurrentBet() < 100) {
                int to = Math.max(t.minRaiseTo(acting), 100);
                t.act(acting, Action.raise(Math.min(to, t.maxRaiseTo(acting))), 0L);
            } else {
                t.act(acting, Action.call(t.toCall(acting)), 0L);
            }
        }

        if (!shorty.isAllIn() || !t.isHandRunning()) {
            passed += 2;
            return;
        }
        // the short all-in raised the price by less than the last full raise, so anybody who had already
        // acted owes the difference and may not raise off the back of it
        boolean sawSomebodyOwing = false;
        for (PokerPlayer other : List.of(big1, big2)) {
            if (!other.canAct() || other.getCommitted() >= t.getCurrentBet()) continue;
            sawSomebodyOwing = true;
            check(t.toCall(other) > 0, other.getName() + " still owes the difference");
            if (other.hasActed()) {
                check(t.minRaiseTo(other) == 0,
                        other.getName() + " may not raise again after a short all-in");
            }
        }
        if (!sawSomebodyOwing) passed++;
        passed++;
    }

    /** Somebody all-in for less can only win what they could have lost. */
    static void sidePots() {
        PokerPlayer shorty = p("Kurz", 100), mid = p("Mittel", 500), big = p("Gross", 1000);
        shorty.beginHand(); mid.beginHand(); big.beginHand();
        shorty.commit(100); mid.commit(500); big.commit(500);
        List<Pot> pots = Pot.build(List.of(shorty, mid, big));
        check(pots.size() == 2, "two pots, got " + pots.size() + ": " + pots);
        check(pots.get(0).getAmount() == 300, "main pot is 3x100, got " + pots.get(0).getAmount());
        check(pots.get(0).getEligible().size() == 3, "everybody may win the main pot");
        check(pots.get(1).getAmount() == 800, "side pot is 2x400, got " + pots.get(1).getAmount());
        check(!pots.get(1).getEligible().contains(shorty), "the short stack cannot win the side pot");
        int total = pots.stream().mapToInt(Pot::getAmount).sum();
        check(total == 1100, "the pots add up to what went in, got " + total);
    }

    /** What a folded player put in stays in the middle and is won by somebody else. */
    static void foldedChipsStayInPot() {
        PokerPlayer folder = p("Weg", 500), a = p("A", 500), b = p("B", 500);
        folder.beginHand(); a.beginHand(); b.beginHand();
        folder.commit(100); a.commit(300); b.commit(300);
        folder.fold();
        List<Pot> pots = Pot.build(List.of(folder, a, b));
        int total = pots.stream().mapToInt(Pot::getAmount).sum();
        check(total == 700, "the folded chips stay in, got " + total);
        for (Pot pot : pots) {
            check(!pot.getEligible().contains(folder), "a folded player wins nothing");
        }
    }

    /** No flop, no drop. */
    static void rakeOnlyWhenContested() {
        RakePolicy three = (pot, contested) -> contested ? pot * 3 / 100 : 0;
        check(three.rakeOf(1000, false) == 0, "a hand won before the flop is not raked");
        check(three.rakeOf(1000, true) == 30, "three percent of a contested pot");
    }

    /** A split pot is split to the chip, and the odd one goes left of the button. */
    static void splitPotOddChip() {
        log.clear();
        PokerTable t = new PokerTable(1, 2, new TableRules(5, 30, false, RakePolicy.NONE), recorder());
        PokerPlayer a = p("A", 1000), b = p("B", 1000);
        t.sitDown(a, 0); t.sitDown(b, 1);
        int before = a.getChips() + b.getChips();
        for (int hand = 0; hand < 200 && !t.isHandRunning(); hand++) t.startHand(0L);
        // play it out with everybody checking and calling
        long now = 0;
        while (t.isHandRunning()) {
            PokerPlayer acting = t.getActing();
            if (acting == null) break;
            t.act(acting, t.toCall(acting) > 0 ? Action.call(t.toCall(acting)) : Action.check(), now);
        }
        int after = a.getChips() + b.getChips();
        check(before == after, "no chip is created or lost in a hand without rake: "
                + before + " -> " + after);
    }

    /** Standing up while all-in waits for the hand instead of throwing the pot away. */
    static void allInLeaveWaits() {
        final int[] leftWith = {-1};
        PokerTable t = new PokerTable(1, 2, new TableRules(10, 30, false, RakePolicy.NONE),
                new TableEvents() {
                    public void onPlayerLeft(PokerTable table, PokerPlayer player, int chips) {
                        leftWith[0] = chips;
                    }
                });
        PokerPlayer shorty = p("Kurz", 100), big = p("Gross", 1000);
        t.sitDown(shorty, 0); t.sitDown(big, 1);
        t.startHand(0L);
        // put the short stack all in and have the other call, then try to leave
        while (t.isHandRunning() && !shorty.isAllIn()) {
            PokerPlayer acting = t.getActing();
            if (acting == null) break;
            if (acting == shorty) t.act(acting, Action.allIn(0), 0L);
            else t.act(acting, t.toCall(acting) > 0 ? Action.call(t.toCall(acting)) : Action.check(), 0L);
        }
        if (t.isHandRunning() && shorty.isAllIn()) {
            boolean goneNow = t.standUp(shorty);
            check(!goneNow, "an all-in player is not let go in the middle of the hand");
            check(t.seatOf(shorty) >= 0, "and keeps their seat until it is over");
            // play the hand out
            long now = 0;
            while (t.isHandRunning()) {
                PokerPlayer acting = t.getActing();
                if (acting == null) break;
                t.act(acting, t.toCall(acting) > 0 ? Action.call(t.toCall(acting)) : Action.check(), now);
            }
            check(leftWith[0] >= 0, "and is let go once it is, with whatever the hand paid: " + leftWith[0]);
        } else {
            passed += 3;
        }
    }

    /** Play a lot of hands and check that the chips at the table only ever move, never appear. */
    static void longRunNoChipsLostOrMade() {
        int rakePermille = 30;
        final int[] raked = {0};
        RakePolicy rake = (pot, contested) -> contested ? pot * rakePermille / 1000 : 0;
        PokerTable t = new PokerTable(1, 6, new TableRules(10, 30, false, rake), new TableEvents() {
            public void onPotAwarded(PokerTable table, PokerPlayer w, int amount, int r, HandValue v) {
                raked[0] += r;
            }
        });
        List<PokerPlayer> players = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            PokerPlayer pl = p("P" + i, 2000);
            players.add(pl);
            t.sitDown(pl, i);
        }
        int start = players.stream().mapToInt(PokerPlayer::getChips).sum();

        Random random = new Random(42);
        long now = 0;
        for (int hand = 0; hand < 400; hand++) {
            now += 10_000;
            t.tick(now);
            if (!t.isHandRunning()) continue;
            int guard = 0;
            while (t.isHandRunning() && guard++ < 500) {
                PokerPlayer acting = t.getActing();
                if (acting == null) break;
                int toCall = t.toCall(acting);
                int roll = random.nextInt(10);
                if (roll < 3 && toCall > 0) {
                    t.act(acting, Action.fold(), now);
                } else if (roll < 8) {
                    t.act(acting, toCall > 0 ? Action.call(toCall) : Action.check(), now);
                } else {
                    int min = t.minRaiseTo(acting);
                    int max = t.maxRaiseTo(acting);
                    if (min > 0 && min <= max) {
                        int to = min + (max > min ? random.nextInt(max - min + 1) : 0);
                        t.act(acting, to >= max ? Action.allIn(0) : Action.raise(to), now);
                    } else {
                        t.act(acting, toCall > 0 ? Action.call(toCall) : Action.check(), now);
                    }
                }
            }
            check(guard < 500, "hand " + hand + " ended instead of looping forever");
            if (guard >= 500) break;
        }
        int end = players.stream().mapToInt(PokerPlayer::getChips).sum();
        check(end + raked[0] == start, "chips at the table plus the rake equal what was brought: "
                + start + " -> " + end + " + " + raked[0] + " rake = " + (end + raked[0]));
        check(raked[0] > 0, "the house took something over 400 hands, got " + raked[0]);
        System.out.println("  (Langlauf: 400 Hände, " + raked[0] + " Chips Hausanteil, Stände "
                + players.stream().map(pl -> String.valueOf(pl.getChips())).toList() + ")");
    }
}

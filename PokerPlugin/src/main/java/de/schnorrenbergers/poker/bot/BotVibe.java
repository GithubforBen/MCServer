package de.schnorrenbergers.poker.bot;

import java.util.Random;

/**
 * A bot's temperament: what it is like to play against, as a handful of numbers that never repeat.
 * <p>
 * The point of this class is that a player should not be able to learn a bot. Six bots drawn from three
 * fixed settings are three opponents, and three opponents get solved in an evening: this one always folds
 * to a raise, that one always fires the turn, and from then on the table is an ATM. So there are no
 * settings here and no types. Two continuous axes are drawn per bot, everything else is a function of them
 * plus noise, and the result is a different opponent every time - not one of three, one of an infinite
 * number of points on a plane.
 * <p>
 * <b>The two axes.</b> Everything a poker temperament is can be put on them:
 * <ul>
 *   <li><b>heat</b> - passive to aggressive. How willing it is to be the one putting money in rather than
 *       the one paying to see.</li>
 *   <li><b>care</b> - reckless to careful. How much better than the price it wants the hand to be before
 *       it goes on. This is the risk-averse axis.</li>
 * </ul>
 * Low heat and high care is the quiet one who folds all evening and shows up with the nuts. High heat and
 * low care is the cheerful one who plays every hand and bets every board. High heat and high care is the
 * one who is not in many pots and runs you over when they are. Nobody had to decide those exist; they fall
 * out of two numbers.
 * <p>
 * <b>Why it also drifts.</b> A fixed temperament is still learnable - it takes longer, but a hundred hands
 * of watching gets there. So it moves: a bad beat pushes heat up and care down for a while, the way tilt
 * works on people, and underneath that both axes wander a little every hand. A read that was right an hour
 * ago is not right now, and that is the whole idea.
 * <p>
 * <b>What it is not allowed to do.</b> Drift and noise move the shades of grey. They never touch the two
 * rules in {@link BotBrain} that keep a bot worth playing against - a leading hand is never folded and a
 * whole stack never goes in behind. A bot on tilt is looser, not broken.
 */
public final class BotVibe {

    /** How far tilt can shift the axes, so a bot on tilt is a worse player and never a different one. */
    private static final double MAX_TILT = 0.18d;
    /** How much of the tilt fades per hand. */
    private static final double TILT_DECAY = 0.86d;
    /** How far the axes themselves wander per hand, so a long read goes stale. */
    private static final double WANDER = 0.006d;

    private final Random random;

    /** Passive to aggressive, nought to one. */
    private double heat;
    /** Reckless to careful, nought to one. */
    private double care;
    /**
     * How consistent it is from one decision to the next.
     * <p>
     * The single most useful trait against somebody looking for a pattern, and the one that is not on
     * either axis: a bot that is a little different in the same spot twice cannot be read off two hands,
     * however well it plays.
     */
    private final double steadiness;
    /** How quickly it takes things personally, which is how fast tilt builds. */
    private final double temper;
    /** How long it likes to think about it, before the spot is taken into account. */
    private final double patience;
    /** Where it is right now, on top of the axes. Positive is tilted. */
    private double tilt;

    /**
     * Draws a temperament.
     * <p>
     * Both axes come from the average of two uniforms rather than one, which bunches them towards the
     * middle and leaves the extremes rare. That matters: a table of six bots should mostly be ordinary
     * players with one or two odd ones, not six caricatures.
     *
     * @param random where the numbers come from
     */
    public BotVibe(Random random) {
        this.random = random;
        this.heat = triangular();
        this.care = triangular();
        this.steadiness = 0.35d + random.nextDouble() * 0.6d;
        this.temper = random.nextDouble();
        this.patience = random.nextDouble();
    }

    /**
     * Draws a temperament that is deliberately unlike another one.
     * <p>
     * Used when a table is filled with several bots at once. Six independent draws land close together more
     * often than people expect, and a table of six bots that all play the same way is exactly the table
     * this class exists to prevent.
     *
     * @param random where the numbers come from
     * @param others the temperaments already at the table
     */
    public static BotVibe unlike(Random random, Iterable<BotVibe> others) {
        BotVibe best = null;
        double bestDistance = -1d;
        // a handful of candidates and the one furthest from everybody already sitting there. Cheap, and it
        // spreads a table out far better than drawing once
        for (int attempt = 0; attempt < 6; attempt++) {
            BotVibe candidate = new BotVibe(random);
            double nearest = Double.MAX_VALUE;
            for (BotVibe other : others) {
                nearest = Math.min(nearest, candidate.distanceTo(other));
            }
            if (nearest == Double.MAX_VALUE) return candidate;
            if (nearest > bestDistance) {
                bestDistance = nearest;
                best = candidate;
            }
        }
        return best == null ? new BotVibe(random) : best;
    }

    private double distanceTo(BotVibe other) {
        return Math.hypot(heat - other.heat, care - other.care);
    }

    /**
     * @return a number between nought and one that is much more likely to be near the middle
     */
    private double triangular() {
        return (random.nextDouble() + random.nextDouble()) / 2d;
    }

    /* ------------------------------------------------------------------ what the brain asks for */

    /**
     * @return how aggressive it is right now, nought to one
     */
    public double heat() {
        return clamp(heat + tilt);
    }

    /**
     * @return how careful it is right now, nought to one. Tilt eats into it
     */
    public double care() {
        return clamp(care - tilt);
    }

    /**
     * How much better than the price the hand has to be before it goes on.
     * <p>
     * Around one, above it for a careful bot and below it for a reckless one. This is the number that
     * decides whether a marginal call is made, so it is also the number a person would most like to be able
     * to predict - which is why the jitter is on it.
     *
     * @return the multiplier on what the pot is offering
     */
    public double demandedEdge() {
        return 0.86d + care() * 0.34d + jitter();
    }

    /**
     * @return how often it raises a hand worth raising, nought to about a half
     */
    public double raiseRate() {
        return 0.10d + heat() * 0.34d;
    }

    /**
     * @return how often it bets a hand worth nothing
     */
    public double bluffRate() {
        // bluffing is what a warm careless bot does. A careful one has to be warm indeed before it starts
        return Math.max(0.01d, heat() * 0.14d * (1.25d - care()));
    }

    /**
     * How wide the band is in which it makes up its mind rather than following the maths.
     * <p>
     * This is what makes a threshold unreadable. A bot with a hard line at "call above 34%" answers the
     * same spot the same way every time, and two hands of watching is enough to find the line. Inside this
     * band the answer is a weighted coin instead - which is also what people do in marginal spots, for the
     * same reason.
     *
     * @return how far either side of the price it mixes
     */
    public double mixBand() {
        return 0.05d + (1d - steadiness) * 0.09d;
    }

    /**
     * How often it raises with a hand that is behind but has somewhere to go.
     * <p>
     * A bot that only ever raises when it is already ahead is readable in one sentence: if it raises, fold.
     * It is also weak - a draw that can make somebody fold the best hand is worth betting - so this is both
     * halves of the same fix.
     *
     * @return how often, nought to about a fifth
     */
    public double semiBluffRate() {
        return heat() * 0.22d * (1.15d - care() * 0.6d);
    }

    /**
     * @return how much of the pot it bets, as a share
     */
    public double betShare() {
        return 0.38d + heat() * 0.42d + (random.nextDouble() - 0.5d) * 0.12d;
    }

    /**
     * @return how many times the standing bet it raises to
     */
    public double raiseFactor() {
        return 2.1d + heat() * 1.1d + random.nextDouble() * 0.5d;
    }

    /**
     * The wobble on a single decision.
     * <p>
     * Small, and the whole reason a bot cannot be read off two hands: the same spot twice does not
     * reliably give the same answer. A steady bot wobbles by a percent or two, an erratic one by nearly a
     * tenth - and which of the two it is, is itself something you would have to work out.
     *
     * @return a signed nudge on whatever it is added to
     */
    public double jitter() {
        double width = (1d - steadiness) * 0.16d;
        return (random.nextDouble() + random.nextDouble() - 1d) * width;
    }

    /**
     * @param closeCall whether the decision is a near thing, which is what somebody would really think about
     * @return how long to take over it, in milliseconds
     */
    public long thinkingTime(boolean closeCall) {
        double base = 500d + patience * 1300d;
        if (closeCall) base += 400d + patience * 1500d;
        // never quite the same twice, because timing is a tell as much as anything else is
        return (long) (base * (0.7d + random.nextDouble() * 0.6d));
    }

    /* ------------------------------------------------------------------ how it changes */

    /**
     * Told after every hand, so the temperament moves with the evening.
     *
     * @param won         whether it took the pot
     * @param chipsBefore what it had when the hand started
     * @param chipsAfter  what it has now
     */
    public void afterHand(boolean won, int chipsBefore, int chipsAfter) {
        tilt *= TILT_DECAY;
        if (chipsBefore > 0) {
            double swing = (double) (chipsAfter - chipsBefore) / chipsBefore;
            if (swing < -0.25d) {
                // a hand that cost a quarter of the stack. Some people shrug that off and some do not
                tilt += temper * Math.min(0.12d, -swing * 0.16d);
            } else if (won && swing > 0.3d) {
                // and a big win loosens people up too, just less
                tilt += temper * 0.03d;
            }
        }
        tilt = Math.max(-MAX_TILT, Math.min(MAX_TILT, tilt));

        // and underneath the tilt, a slow wander, so a read taken an hour ago has quietly gone stale
        heat = clamp(heat + (random.nextDouble() - 0.5d) * WANDER * 2d);
        care = clamp(care + (random.nextDouble() - 0.5d) * WANDER * 2d);
    }

    private static double clamp(double value) {
        return Math.max(0d, Math.min(1d, value));
    }

    /**
     * @return the temperament written out, for a log. Never shown to a player - a bot whose character is
     *         on a label is a bot with no character
     */
    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT,
                "heat=%.2f care=%.2f steady=%.2f temper=%.2f tilt=%+.2f",
                heat, care, steadiness, temper, tilt);
    }
}

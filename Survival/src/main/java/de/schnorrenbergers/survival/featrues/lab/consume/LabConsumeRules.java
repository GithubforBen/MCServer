package de.schnorrenbergers.survival.featrues.lab.consume;

import java.util.HashMap;
import java.util.Map;

/**
 * The tables of consuming Kokain, the crash, the combat cap and the cooldown, with nothing from Bukkit so a
 * check can run them. Levels are 1-based here (level II is 2); 0 means none.
 */
public final class LabConsumeRules {

    /** At most this level of Speed and Haste from Kokain while in combat. */
    public static final int COMBAT_MAX_LEVEL = 2;

    /** No cooldown has started. */
    public static final long NEVER = -1;

    private static final int[] SPEED = {1, 1, 2, 2, 3};
    private static final int[] HASTE = {0, 1, 1, 2, 3};
    private static final int[] SECONDS = {30, 45, 60, 90, 120};
    private static final int[] SLOWNESS = {0, 0, 1, 1, 2};
    private static final int[] CRASH_SECONDS = {5, 8, 10, 15, 20};

    private LabConsumeRules() {
    }

    /**
     * The effects of one Kokain.
     *
     * @param speed   Speed level, 0 for none
     * @param haste   Haste level, 0 for none
     * @param seconds how long, and how long the cut-in effects last
     */
    public record Grant(int speed, int haste, int seconds) {
    }

    /**
     * The crash after the effects. Nausea comes at every purity.
     *
     * @param nausea   whether nausea comes
     * @param slowness Slowness level, 0 for none
     * @param seconds  how long
     */
    public record Crash(boolean nausea, int slowness, int seconds) {
    }

    /** What Kokain of this purity gives; in combat Speed and Haste are capped. */
    public static Grant grant(int purity, boolean inCombat) {
        int i = index(purity);
        return new Grant(cap(SPEED[i], inCombat), cap(HASTE[i], inCombat), SECONDS[i]);
    }

    /** The crash that follows Kokain of this purity. */
    public static Crash crash(int purity) {
        int i = index(purity);
        return new Crash(true, SLOWNESS[i], CRASH_SECONDS[i]);
    }

    /** The level under the combat rule: above {@link #COMBAT_MAX_LEVEL} only out of combat. */
    public static int cap(int level, boolean inCombat) {
        return inCombat ? Math.min(level, COMBAT_MAX_LEVEL) : level;
    }

    /** Seconds still to wait, rounded up; 0 when consuming is allowed. */
    public static int cooldownLeft(long lastMillis, long nowMillis, int cooldownSeconds) {
        if (lastMillis == NEVER || cooldownSeconds <= 0) return 0;
        long elapsed = Math.max(0, nowMillis - lastMillis);
        long left = cooldownSeconds * 1000L - elapsed;
        return left <= 0 ? 0 : (int) ((left + 999) / 1000);
    }

    /** Whether a hit counts for combat: only between two different players. */
    public static boolean countsAsCombat(boolean attackerIsPlayer, boolean victimIsPlayer, boolean samePlayer) {
        return attackerIsPlayer && victimIsPlayer && !samePlayer;
    }

    /**
     * Whether a running effect is the one Kokain gave and not the same effect from a potion or a beacon: the
     * level matches what was recorded and it ends at the recorded time, within the tolerance.
     */
    public static boolean isOurs(int activeAmplifier, long activeEndMillis, int recordedAmplifier,
                                 long recordedEndMillis, long toleranceMillis) {
        return activeAmplifier == recordedAmplifier
                && Math.abs(activeEndMillis - recordedEndMillis) <= toleranceMillis;
    }

    /** Whether a crash that fell due at the given time starts now. */
    public static boolean crashDue(long dueMillis, long nowMillis) {
        return nowMillis >= dueMillis;
    }

    /** When players last dealt or took a hit from another player. */
    public static final class CombatClock<K> {
        private final Map<K, Long> last = new HashMap<>();
        private final int seconds;

        public CombatClock(int seconds) {
            this.seconds = seconds;
        }

        public void hit(K player, long nowMillis) {
            last.put(player, nowMillis);
        }

        public boolean inCombat(K player, long nowMillis) {
            Long at = last.get(player);
            return at != null && seconds > 0 && nowMillis - at < seconds * 1000L;
        }

        public void forget(K player) {
            last.remove(player);
        }
    }

    private static int index(int purity) {
        if (purity < 1 || purity > 5) throw new IllegalArgumentException("purity " + purity);
        return purity - 1;
    }
}

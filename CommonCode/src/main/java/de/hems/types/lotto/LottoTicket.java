package de.hems.types.lotto;

import java.io.Serializable;
import java.util.Arrays;
import java.util.UUID;

/**
 * One tip: four different numbers from 1 to 15, bought for one round.
 * <p>
 * The rules live here because the game server checks a tip before it is sent and the launcher checks it
 * again before it takes the money - both with the same code.
 */
public class LottoTicket implements Serializable {

    private static final long serialVersionUID = 4600L;

    /** How many numbers a tip has, and the draw draws. */
    public static final int NUMBERS = 4;
    /** The highest number; the lowest is 1. */
    public static final int HIGHEST = 15;

    private long id;
    private int round;
    private UUID player;
    private String playerName;
    private int[] numbers;
    private long boughtAt;

    public LottoTicket() {
    }

    public LottoTicket(long id, int round, UUID player, String playerName, int[] numbers, long boughtAt) {
        this.id = id;
        this.round = round;
        this.player = player;
        this.playerName = playerName;
        this.numbers = numbers.clone();
        this.boughtAt = boughtAt;
    }

    /**
     * @param numbers what somebody picked
     * @return the tip sorted, or {@code null} if it is not four different numbers from 1 to {@link #HIGHEST}
     */
    public static int[] normalize(int[] numbers) {
        if (numbers == null || numbers.length != NUMBERS) return null;
        int[] sorted = numbers.clone();
        Arrays.sort(sorted);
        for (int i = 0; i < sorted.length; i++) {
            if (sorted[i] < 1 || sorted[i] > HIGHEST) return null;
            if (i > 0 && sorted[i] == sorted[i - 1]) return null;
        }
        return sorted;
    }

    /**
     * @param random where the chance comes from
     * @return four different numbers from 1 to {@link #HIGHEST}, sorted - a quick tip, or a draw
     */
    public static int[] random(java.util.Random random) {
        java.util.List<Integer> pool = new java.util.ArrayList<>();
        for (int i = 1; i <= HIGHEST; i++) pool.add(i);
        int[] picked = new int[NUMBERS];
        for (int i = 0; i < picked.length; i++) picked[i] = pool.remove(random.nextInt(pool.size()));
        return normalize(picked);
    }

    /**
     * @param numbers a tip or a draw
     * @return it written the way it is shown everywhere, like "3 · 7 · 11 · 14"
     */
    public static String format(int[] numbers) {
        if (numbers == null) return "-";
        StringBuilder text = new StringBuilder();
        for (int number : numbers) {
            if (!text.isEmpty()) text.append(" · ");
            text.append(number);
        }
        return text.toString();
    }

    /**
     * @param drawn the numbers of a draw
     * @return whether this tip has all of them
     */
    public boolean wins(int[] drawn) {
        int[] sorted = normalize(drawn);
        return sorted != null && Arrays.equals(numbers, sorted);
    }

    public long getId() {
        return id;
    }

    public int getRound() {
        return round;
    }

    public UUID getPlayer() {
        return player;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int[] getNumbers() {
        return numbers.clone();
    }

    public long getBoughtAt() {
        return boughtAt;
    }
}

package de.hems.types.poker;

import java.io.Serializable;
import java.util.UUID;

/**
 * What one player did over one poker night, as it travels between the casino server, the launcher and the
 * lobby.
 * <p>
 * Two numbers carry the whole thing: what somebody put on the table and what came back off it. Everything
 * else - the hands, the biggest pot - is there so the ranking can tell somebody who played from somebody
 * who sat down once and got lucky.
 * <p>
 * <b>Why a bot's chips land in its owner's numbers.</b> A bot has no account. Its stack was paid for by
 * whoever put it down, and whatever is left of that stack goes back to them, so both halves belong in their
 * row - which also means a bot that gets taken apart at the table costs its owner ranking, exactly like
 * losing the money themselves. The fee for putting it down goes in as well and never comes back out, and
 * that is the point: filling a table with bots is supposed to be a decision, not a free lever.
 */
public class PokerStatsData implements Serializable {

    private static final long serialVersionUID = 4330L;

    /** The poker night this belongs to. */
    private UUID eventId;
    private UUID playerId;
    private String playerName;

    /** Bits that went onto a table: buy-ins, re-buys, bot stacks and bot fees. */
    private int boughtIn;
    /** Bits that came back off it: cash-outs, tournament payouts, what was left of a bot. */
    private int cashedOut;
    /**
     * Chips still sitting in front of somebody, in bits.
     * <p>
     * Counted towards the ranking as if it had been cashed out, because a player who is up and still
     * playing is up. It is kept apart from {@link #cashedOut} so the launcher can tell the difference
     * between money that is safe and money that is still at risk - after a crash it is the second kind
     * that has to be given back.
     */
    private int openStack;

    private int hands;
    private int handsWon;
    private int biggestPot;

    private long firstSeenAt;
    private long updatedAt;

    public PokerStatsData() {
    }

    public PokerStatsData(UUID eventId, UUID playerId, String playerName) {
        this.eventId = eventId;
        this.playerId = playerId;
        this.playerName = playerName;
        this.firstSeenAt = System.currentTimeMillis();
        this.updatedAt = this.firstSeenAt;
    }

    /**
     * What somebody walked away with against what they brought.
     * <p>
     * This is the ranking: two means the money doubled, half means half of it is gone. Higher is better.
     * Somebody who never bought in has no ratio at all rather than a division by zero, and is sorted last.
     *
     * @return the ratio, or {@code 0} for somebody who never put anything in
     */
    public double getRatio() {
        if (boughtIn <= 0) return 0d;
        return (double) (cashedOut + openStack) / (double) boughtIn;
    }

    /**
     * @return what is left over in bits, negative when the evening cost money
     */
    public int getProfit() {
        return cashedOut + openStack - boughtIn;
    }

    /**
     * Whether this row is allowed into the ranking.
     * <p>
     * Both bars exist for the same reason: a ratio rewards the smallest possible denominator. Without them
     * the winner of every poker night is whoever bought in for the minimum, won one hand and left, and
     * everybody who actually played the evening is behind them.
     *
     * @param minHands  how many hands have to have been played
     * @param minVolume how many bits have to have gone in
     * @return whether it counts
     */
    public boolean qualifies(int minHands, int minVolume) {
        return boughtIn > 0 && hands >= minHands && boughtIn >= minVolume;
    }

    /**
     * @return the ratio written out, like "1,42x"
     */
    public String getRatioText() {
        if (boughtIn <= 0) return "-";
        return String.format(java.util.Locale.GERMANY, "%.2fx", getRatio());
    }

    public void addBoughtIn(int bits) {
        if (bits <= 0) return;
        boughtIn += bits;
        touch();
    }

    public void addCashedOut(int bits) {
        if (bits <= 0) return;
        cashedOut += bits;
        touch();
    }

    public void addHand(boolean won, int pot) {
        hands++;
        if (won) handsWon++;
        if (pot > biggestPot) biggestPot = pot;
        touch();
    }

    private void touch() {
        updatedAt = System.currentTimeMillis();
        if (firstSeenAt == 0) firstSeenAt = updatedAt;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getBoughtIn() {
        return boughtIn;
    }

    public void setBoughtIn(int boughtIn) {
        this.boughtIn = Math.max(0, boughtIn);
    }

    public int getCashedOut() {
        return cashedOut;
    }

    public void setCashedOut(int cashedOut) {
        this.cashedOut = Math.max(0, cashedOut);
    }

    public int getOpenStack() {
        return openStack;
    }

    public void setOpenStack(int openStack) {
        this.openStack = Math.max(0, openStack);
        touch();
    }

    public int getHands() {
        return hands;
    }

    public void setHands(int hands) {
        this.hands = Math.max(0, hands);
    }

    public int getHandsWon() {
        return handsWon;
    }

    public void setHandsWon(int handsWon) {
        this.handsWon = Math.max(0, handsWon);
    }

    public int getBiggestPot() {
        return biggestPot;
    }

    public void setBiggestPot(int biggestPot) {
        this.biggestPot = Math.max(0, biggestPot);
    }

    public long getFirstSeenAt() {
        return firstSeenAt;
    }

    public void setFirstSeenAt(long firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * The order a poker ranking is read in: best ratio first, and where two are equal the one who risked
     * more is in front - the same ratio off a bigger stake is the harder thing to do.
     *
     * @return the comparator
     */
    public static java.util.Comparator<PokerStatsData> rankingOrder() {
        return java.util.Comparator.comparingDouble(PokerStatsData::getRatio).reversed()
                .thenComparing(java.util.Comparator.comparingInt(PokerStatsData::getBoughtIn).reversed())
                .thenComparing(java.util.Comparator.comparingInt(PokerStatsData::getHands).reversed());
    }

    /**
     * @return a copy that can be edited without touching the cached original
     */
    public PokerStatsData copy() {
        PokerStatsData copy = new PokerStatsData();
        copy.eventId = eventId;
        copy.playerId = playerId;
        copy.playerName = playerName;
        copy.boughtIn = boughtIn;
        copy.cashedOut = cashedOut;
        copy.openStack = openStack;
        copy.hands = hands;
        copy.handsWon = handsWon;
        copy.biggestPot = biggestPot;
        copy.firstSeenAt = firstSeenAt;
        copy.updatedAt = updatedAt;
        return copy;
    }
}

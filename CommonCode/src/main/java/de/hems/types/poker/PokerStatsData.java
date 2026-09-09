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
     * Shown next to the profit because it says something the profit does not - somebody who turned 500 into
     * 900 played better than somebody who turned 50000 into 51000 - but it is not what the ranking is
     * ordered by. See {@link #rankingOrder()} for why.
     *
     * @return the ratio, or {@code 0} for somebody who never put anything in
     */
    public double getRatio() {
        if (boughtIn <= 0) return 0d;
        return (double) (cashedOut + openStack) / (double) boughtIn;
    }

    /**
     * What the evening was worth, in bits.
     * <p>
     * This is the ranking: everything that came off the tables minus everything that went onto them. Chips
     * still sitting in front of somebody count, because a player who is up and still playing is up.
     *
     * @return the winnings, negative when the evening cost money
     */
    public int getProfit() {
        return cashedOut + openStack - boughtIn;
    }

    /**
     * @return the winnings written for a board, with a sign so a loss reads as one
     */
    public String getProfitText() {
        int profit = getProfit();
        return (profit > 0 ? "+" : "") + profit;
    }

    /**
     * Whether this row is allowed into the ranking.
     * <p>
     * One bar really matters here, and it is the hands. Somebody who sits down, is dealt aces once, wins a
     * big pot and leaves has a real profit and has not played a poker night; the hand count is what tells
     * the two apart.
     * <p>
     * The stake bar is the leftover of an earlier version that ranked by ratio, where it was load-bearing:
     * a ratio rewards the smallest denominator, so without a minimum stake the winner was always whoever
     * risked least. Ranking by winnings has no such hole - a big win off a big stake is simply a big win -
     * so it defaults to zero and is left in only for an admin who wants a minimum stake to be counted.
     *
     * @param minHands  how many hands have to have been played
     * @param minVolume how many bits have to have gone in, usually none
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
     * The order a poker ranking is read in: most won first.
     * <p>
     * Deliberately the plain number of bits rather than the ratio it used to be. A ratio has a hole that
     * no amount of qualifying bars really closes - it rewards the smallest stake, so the way to win the
     * evening is to risk as little as possible, which is the opposite of a poker night. Winnings have no
     * such hole: the way to be top of the board is to take money off other people, which is the game.
     * <p>
     * The trade it makes is real and worth saying out loud: it rewards playing bigger. Somebody who wins
     * 5000 off a 50000 buy-in is ahead of somebody who wins 4000 off 1000, even though the second played
     * the better evening. That is how a casino counts, and the ratio is on the board next to it for
     * anybody who wants to see the other story.
     * <p>
     * Ties go to whoever played more hands, because more hands is less luck.
     *
     * @return the comparator
     */
    public static java.util.Comparator<PokerStatsData> rankingOrder() {
        return java.util.Comparator.comparingInt(PokerStatsData::getProfit).reversed()
                .thenComparing(java.util.Comparator.comparingInt(PokerStatsData::getHands).reversed())
                .thenComparing(java.util.Comparator.comparingDouble(PokerStatsData::getRatio).reversed());
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

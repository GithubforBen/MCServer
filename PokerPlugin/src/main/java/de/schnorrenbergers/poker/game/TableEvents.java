package de.schnorrenbergers.poker.game;

import java.util.List;

/**
 * Everything a table has to say while it deals, so that what is drawn in the world and what the rules
 * believe can never drift apart.
 * <p>
 * The rules push, nothing polls. A view that asks "what is the pot now" every tick shows the pot a tick
 * late and, worse, shows a hand that has already been won for as long as it takes to notice.
 * <p>
 * Every method has a do-nothing default, so a table can be run in a test with no world around it.
 */
public interface TableEvents {

    /** A new hand was dealt. */
    default void onHandStarted(PokerTable table, int handNumber) {
    }

    /** Somebody got their two cards, and only they may see them. */
    default void onHoleCards(PokerTable table, PokerPlayer player) {
    }

    /** A blind went in. */
    default void onBlindPosted(PokerTable table, PokerPlayer player, int amount, boolean big) {
    }

    /** Somebody decided. */
    default void onActionTaken(PokerTable table, PokerPlayer player, Action action) {
    }

    /**
     * It is somebody's turn.
     *
     * @param toCall     what it costs to stay in
     * @param minRaiseTo the smallest legal raise, as a total
     * @param deadline   when they run out of time, in epoch millis
     */
    default void onTurn(PokerTable table, PokerPlayer player, int toCall, int minRaiseTo, long deadline) {
    }

    /** Community cards were turned over. */
    default void onStreetDealt(PokerTable table, Street street) {
    }

    /** Cards on their backs. */
    default void onShowdown(PokerTable table, List<PokerTable.ShowdownEntry> entries) {
    }

    /**
     * A pot was handed over.
     *
     * @param with what they won with, or {@code null} when everybody else folded and nothing was shown
     */
    default void onPotAwarded(PokerTable table, PokerPlayer winner, int amount, int rake, HandValue with) {
    }

    /** The hand is over and the table is about to be idle for a moment. */
    default void onHandEnded(PokerTable table) {
    }

    /** Somebody has no chips left and is out of the game until they buy more. */
    default void onSeatBroke(PokerTable table, PokerPlayer player) {
    }

    /**
     * Somebody left the table, and this is the only way chips ever come off one.
     *
     * @param chips what they had left, which is what has to turn back into bits
     */
    default void onPlayerLeft(PokerTable table, PokerPlayer player, int chips) {
    }

    /** Something changed that is worth redrawing. */
    default void onStateChanged(PokerTable table) {
    }
}

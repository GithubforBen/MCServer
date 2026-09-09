package de.schnorrenbergers.poker.game;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Somebody at a table, as far as the rules are concerned: a name, a stack and two cards.
 * <p>
 * A bot is one of these too. The rules do not care which of them is a person - what makes a bot different
 * is only who decides for it and whose account paid for its chips, and both of those are answered here so
 * that nothing else has to ask.
 */
public class PokerPlayer {

    private final UUID id;
    private final String name;
    /** Whose money this seat plays with - the player themselves, or the owner of a bot. */
    private final UUID account;
    private final boolean bot;

    private int chips;
    private final List<Card> hole = new ArrayList<>(2);

    /** What is in front of them on this street. */
    private int committed;
    /** What has gone in over the whole hand, which is what side pots are built from. */
    private int committedTotal;

    private boolean inHand;
    private boolean folded;
    private boolean allIn;
    /**
     * Whether they are sitting the next hand out.
     * <p>
     * Set when somebody walks away from the table, logs off, or has no chips left. They keep their seat -
     * a seat that disappears the moment somebody steps out for a minute is worse than one that waits.
     */
    private boolean sittingOut;

    /** Whether they have had their turn since the last full raise. */
    private boolean acted;
    /**
     * Whether they are still allowed to raise.
     * <p>
     * There is one case where somebody has to act again but may not raise: an all-in that is smaller than a
     * full raise does not reopen the betting. Everybody who had already acted owes the difference and
     * nothing more, and without this they could re-raise off the back of somebody else's short stack.
     */
    private boolean mayRaise = true;

    public PokerPlayer(UUID id, String name, UUID account, boolean bot, int chips) {
        this.id = id;
        this.name = name;
        this.account = account;
        this.bot = bot;
        this.chips = Math.max(0, chips);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * @return the account the chips of this seat belong to: the player, or whoever put the bot down
     */
    public UUID getAccount() {
        return account;
    }

    public boolean isBot() {
        return bot;
    }

    public int getChips() {
        return chips;
    }

    public void setChips(int chips) {
        this.chips = Math.max(0, chips);
    }

    public void addChips(int amount) {
        this.chips = Math.max(0, this.chips + amount);
    }

    public List<Card> getHole() {
        return hole;
    }

    public void setHole(Card first, Card second) {
        hole.clear();
        hole.add(first);
        hole.add(second);
    }

    public void clearHole() {
        hole.clear();
    }

    public int getCommitted() {
        return committed;
    }

    public int getCommittedTotal() {
        return committedTotal;
    }

    /**
     * Moves chips from the stack into the middle.
     *
     * @param amount how much, never more than there is
     * @return what actually went in
     */
    public int commit(int amount) {
        int moved = Math.min(Math.max(0, amount), chips);
        chips -= moved;
        committed += moved;
        committedTotal += moved;
        if (chips == 0 && inHand) allIn = true;
        return moved;
    }

    /**
     * Ends a betting round for this player. What is in front of them goes into the pot; what they have
     * committed over the hand stays, because the side pots are built from it at the end.
     */
    public void endStreet() {
        committed = 0;
        acted = false;
        mayRaise = true;
    }

    /**
     * Puts them into a hand, with a clean sheet.
     */
    public void beginHand() {
        inHand = true;
        folded = false;
        allIn = false;
        committed = 0;
        committedTotal = 0;
        acted = false;
        mayRaise = true;
        hole.clear();
    }

    public void endHand() {
        inHand = false;
        folded = false;
        allIn = false;
        committed = 0;
        committedTotal = 0;
        hole.clear();
    }

    public boolean isInHand() {
        return inHand;
    }

    public boolean isFolded() {
        return folded;
    }

    public void fold() {
        folded = true;
    }

    public boolean isAllIn() {
        return allIn;
    }

    /**
     * @return whether they are still in the hand and can still be asked for a decision
     */
    public boolean canAct() {
        return inHand && !folded && !allIn && chips > 0;
    }

    /**
     * @return whether they are still in the hand at all, all-in included
     */
    public boolean isContesting() {
        return inHand && !folded;
    }

    public boolean isSittingOut() {
        return sittingOut;
    }

    public void setSittingOut(boolean sittingOut) {
        this.sittingOut = sittingOut;
    }

    public boolean hasActed() {
        return acted;
    }

    public void setActed(boolean acted) {
        this.acted = acted;
    }

    public boolean mayRaise() {
        return mayRaise;
    }

    public void setMayRaise(boolean mayRaise) {
        this.mayRaise = mayRaise;
    }

    /**
     * @return whether they are ready to be dealt into the next hand
     */
    public boolean isReady(int bigBlind) {
        // somebody who cannot cover a big blind is not folded out of the game, they are simply not dealt
        // in until they buy more chips. Dealing them in would post a blind they cannot pay
        return !sittingOut && chips >= bigBlind;
    }

    @Override
    public String toString() {
        return name + "(" + chips + ")";
    }
}

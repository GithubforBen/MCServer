package de.schnorrenbergers.poker.game;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A pot and who is allowed to win it.
 * <p>
 * There is more than one whenever somebody is all-in for less than the others: they can only win what they
 * could have lost, and everything past that is a second pot between the players who kept betting. Getting
 * this wrong is not a rounding error - it hands somebody money they were never in for.
 */
public final class Pot {

    private final int amount;
    private final Set<PokerPlayer> eligible;

    public Pot(int amount, Set<PokerPlayer> eligible) {
        this.amount = amount;
        this.eligible = new LinkedHashSet<>(eligible);
    }

    public int getAmount() {
        return amount;
    }

    /**
     * @return who may win it - the players who were not folded and had put in at least this much
     */
    public Set<PokerPlayer> getEligible() {
        return eligible;
    }

    /**
     * Splits the committed chips of a hand into the pots they really are.
     * <p>
     * Worked out from what everybody put in over the whole hand, folded players included: their chips are
     * in the middle and stay there, they simply cannot win any of it. The pots are cut at every level
     * somebody went all-in at, from the smallest upwards.
     *
     * @param players everybody who was dealt into the hand
     * @return the main pot first, then the side pots
     */
    public static List<Pot> build(List<PokerPlayer> players) {
        List<Integer> levels = new java.util.ArrayList<>();
        for (PokerPlayer player : players) {
            int committed = player.getCommittedTotal();
            if (committed > 0 && !levels.contains(committed)) levels.add(committed);
        }
        levels.sort(Integer::compareTo);

        List<Pot> pots = new java.util.ArrayList<>();
        int previous = 0;
        for (int level : levels) {
            int slice = level - previous;
            int amount = 0;
            Set<PokerPlayer> eligible = new LinkedHashSet<>();
            for (PokerPlayer player : players) {
                int committed = player.getCommittedTotal();
                if (committed <= previous) continue;
                amount += Math.min(committed - previous, slice);
                // a folded player pays into the pot and cannot win it, which is the whole point of
                // separating "what is in there" from "who may have it"
                if (player.isContesting() && committed >= level) eligible.add(player);
            }
            if (amount > 0 && !eligible.isEmpty()) {
                pots.add(new Pot(amount, eligible));
            } else if (amount > 0) {
                // everybody who could have won this slice folded. It cannot simply vanish, so it is added
                // to the pot below it - the players who are left were in for that much as well
                if (!pots.isEmpty()) {
                    Pot last = pots.removeLast();
                    pots.add(new Pot(last.getAmount() + amount, last.getEligible()));
                }
            }
            previous = level;
        }
        return pots;
    }

    @Override
    public String toString() {
        return amount + " für " + eligible;
    }
}

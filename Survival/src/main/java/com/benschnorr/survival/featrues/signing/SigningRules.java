package com.benschnorr.survival.featrues.signing;

import java.util.UUID;

/**
 * Decides what {@code /signItem} does with the stack in the hand. No Bukkit types, so it can be checked
 * with a plain main. see knowledge: /capabilities/item-signing.md
 */
public final class SigningRules {

    /** What happens to the stack. */
    public enum Outcome { EMPTY_HAND, NOT_SIGNABLE, ALREADY_YOURS, SIGNED_BY_OTHER, SIGN }

    private SigningRules() {
    }

    /**
     * @param empty      the main hand holds nothing
     * @param signable   the item carries no function of the network
     * @param signedBy   who signed it, or null if unsigned
     * @param player     who runs the command
     * @return the outcome; the caller names the other signer for {@link Outcome#SIGNED_BY_OTHER}
     */
    public static Outcome decide(boolean empty, boolean signable, UUID signedBy, UUID player) {
        if (empty) return Outcome.EMPTY_HAND;
        if (!signable) return Outcome.NOT_SIGNABLE;
        if (signedBy == null) return Outcome.SIGN;
        return signedBy.equals(player) ? Outcome.ALREADY_YOURS : Outcome.SIGNED_BY_OTHER;
    }

    /** The lore line for a signer name; the same text for the same name every time. */
    public static String loreLine(String name) {
        return "Signiert von " + name;
    }
}

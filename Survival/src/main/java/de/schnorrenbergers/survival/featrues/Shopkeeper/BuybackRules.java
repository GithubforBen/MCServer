package de.schnorrenbergers.survival.featrues.Shopkeeper;

/**
 * Whether a buyback sale may start, without any Bukkit type so a plain {@code main} can test it.
 * <p>
 * Only the checks before any bits move live here; the debit itself is decided by the launcher, which is
 * the only place two sellers against one account are told apart.
 */
public final class BuybackRules {

    /** Why a sale does not start. */
    public enum Refusal {
        BROKEN_OFFER("Dieses Angebot ist kaputt und kann nicht verkauft werden."),
        BUYBACK_OFF("Dieser Shop kauft das nicht zurück."),
        NO_CHEST("Die Kiste dieses Shops gibt es nicht mehr."),
        NO_TEAM("Diesen Shop gibt es nicht mehr oder sein Team wird noch geladen."),
        NOT_ENOUGH_GOODS("Du hast davon nicht genug dabei, oder es ist nicht genau derselbe Gegenstand."),
        CHEST_FULL("Die Kiste des Shops ist voll."),
        TEAM_CANNOT_PAY("Das Team dieses Shops hat nicht genug Bits.");

        private final String message;

        Refusal(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }
    }

    private BuybackRules() {
    }

    /**
     * @param offerValid   whether the offer has an item and a positive lot size
     * @param buybackOn    whether the offer buys back
     * @param chestExists  whether the stock chest is there
     * @param teamExists   whether the owning team is known
     * @param lotSize      items per lot
     * @param heldMatching how many items identical to the offer's the player holds
     * @param chestRoom    how many such items still fit into the chest
     * @param teamBalance  what the team account holds
     * @param price        the buyback price per lot
     * @return the first reason to refuse, or {@code null} when the sale may start
     */
    public static Refusal check(boolean offerValid, boolean buybackOn, boolean chestExists, boolean teamExists,
                                int lotSize, int heldMatching, int chestRoom, int teamBalance, int price) {
        if (!offerValid || lotSize <= 0) return Refusal.BROKEN_OFFER;
        if (!buybackOn) return Refusal.BUYBACK_OFF;
        if (!chestExists) return Refusal.NO_CHEST;
        if (!teamExists) return Refusal.NO_TEAM;
        if (heldMatching < lotSize) return Refusal.NOT_ENOUGH_GOODS;
        if (chestRoom < lotSize) return Refusal.CHEST_FULL;
        if (price < 0 || teamBalance < price) return Refusal.TEAM_CANNOT_PAY;
        return null;
    }
}

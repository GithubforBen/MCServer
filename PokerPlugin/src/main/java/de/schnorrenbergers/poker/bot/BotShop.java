package de.schnorrenbergers.poker.bot;

import de.schnorrenbergers.poker.Casino;
import de.schnorrenbergers.poker.CasinoContext;
import de.schnorrenbergers.poker.CasinoTable;
import de.schnorrenbergers.poker.bank.Bank;
import de.hems.types.event.PokerEventSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * What it costs to put a bot down, and why it costs anything.
 * <p>
 * A bot has no account. Its chips have to come from somewhere, and the only honest somewhere is the person
 * who asked for it: they pay for the stack, they get back whatever is left of it, and if the table takes it
 * apart the money is really gone - out of their pocket, into the pockets of whoever won it.
 * <p>
 * Without that, a bot would be a machine for making bits. Everything it lost would be money the network
 * created out of nothing and handed to the other players, and a table with three bots at it would print
 * faster than anybody could spend.
 * <p>
 * On top of the stack there is a fee, and the fee is the part that does not come back. That is what makes
 * filling a table with bots a decision rather than a free lever: a bot that plays roughly break-even costs
 * its owner the fee, every time.
 */
public final class BotShop {

    /** How many bots one person may have at one table. */
    private static final int MAX_PER_PLAYER = 3;

    private BotShop() {
    }

    /**
     * Puts a bot down for somebody.
     *
     * @param owner who is paying
     * @param table where it sits
     */
    public static void spawn(Player owner, CasinoTable table) {
        PokerEventSettings settings = Casino.getSettings();
        if (!settings.isBotsAllowed()) {
            owner.sendMessage(Component.text("Bots sind an diesem Abend aus.", NamedTextColor.RED));
            return;
        }
        if (!table.hasRoom()) {
            owner.sendMessage(Component.text("Am Tisch ist kein Platz frei.", NamedTextColor.RED));
            return;
        }
        if (table.botsOf(owner.getUniqueId()) >= MAX_PER_PLAYER) {
            owner.sendMessage(Component.text("Mehr als " + MAX_PER_PLAYER
                    + " eigene Bots pro Tisch gehen nicht.", NamedTextColor.RED));
            return;
        }

        int stack = settings.getBuyIn();
        int fee = settings.getBotFee();
        int cost = stack + fee;
        owner.sendMessage(Component.text("Bot kostet " + cost + " Bits: " + stack
                + " als Stack, " + fee + " Gebühr.", NamedTextColor.GRAY));

        Bank.buyFor(owner, owner.getUniqueId(), owner.getName(), cost,
                "Poker: Bot " + CasinoContext.getTitle(), paid -> {
                    if (paid <= 0) return;
                    if (!table.seatBot(owner.getUniqueId(), owner.getName(), stack)) {
                        // the seat went while the launcher was answering - everything back, fee included,
                        // because nothing happened
                        Bank.cashOut(owner.getUniqueId(), owner.getName(), cost,
                                "Poker: Bot konnte nicht sitzen, Geld zurück");
                        owner.sendMessage(Component.text("Der Platz war weg - dein Geld ist zurück.",
                                NamedTextColor.YELLOW));
                        return;
                    }
                    owner.sendMessage(Component.text("Der Bot sitzt. Was er übrig lässt, bekommst du "
                            + "zurück - was er verliert, ist weg.", NamedTextColor.GREEN));
                });
    }

    /**
     * Takes every bot somebody put down off a table.
     *
     * @param owner whose bots
     * @param table which table
     */
    public static void removeAll(Player owner, CasinoTable table) {
        int removed = table.removeBotsOf(owner.getUniqueId());
        owner.sendMessage(removed == 0
                ? Component.text("Du hast hier keine Bots.", NamedTextColor.GRAY)
                : Component.text(removed + " Bot(s) abgeräumt - was übrig war, ist wieder auf deinem Konto.",
                NamedTextColor.GREEN));
    }

    /**
     * @return the fee, which is what somebody wants to know before they say yes
     */
    public static int costOf() {
        PokerEventSettings settings = Casino.getSettings();
        return settings.getBuyIn() + settings.getBotFee();
    }
}

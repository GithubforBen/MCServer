package de.schnorrenbergers.poker.ui;

import de.schnorrenbergers.poker.game.Card;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;

/**
 * How a card looks on the table.
 * <p>
 * Drawn out of text rather than out of a texture, because a texture means a resource pack and a resource
 * pack means every player has to accept a download before they can tell a king from a five. Two lines - the
 * rank over the pip, red or black, on white - read as a playing card from across the room and cost nothing.
 */
public final class CardArt {

    /** The white of a card face. */
    private static final Color FACE = Color.fromARGB(255, 245, 245, 240);
    /** The deep red of the back of the deck. */
    private static final Color BACK = Color.fromARGB(255, 120, 20, 20);
    private static final TextColor RED = TextColor.color(0xC0202F);
    private static final TextColor BLACK = TextColor.color(0x1A1A1A);

    /** How big a card is on the felt. */
    public static final float SCALE = 0.42f;

    private CardArt() {
    }

    /**
     * Puts a face-up card on the table.
     *
     * @param at   where it lies, yaw included
     * @param card which card
     * @return the display
     */
    public static TextDisplay face(Location at, Card card) {
        TextDisplay display = Displays.flatText(at, SCALE, FACE);
        if (display != null) display.text(text(card));
        return display;
    }

    /**
     * Puts a card face down on the table - the thing everybody can see is there without knowing what it is.
     *
     * @param at where it lies
     * @return the display
     */
    public static TextDisplay back(Location at) {
        TextDisplay display = Displays.flatText(at, SCALE, BACK);
        if (display != null) {
            display.text(Component.text("✦", TextColor.color(0xE0C060)));
        }
        return display;
    }

    /**
     * Turns a card over in place, so a showdown does not have to take every card away and put a new one
     * down - which would look like the cards flickering rather than being turned.
     *
     * @param display the card on the table
     * @param card    what it turns out to be, or {@code null} to turn it back over
     */
    public static void turn(TextDisplay display, Card card) {
        if (display == null || !display.isValid()) return;
        if (card == null) {
            display.setBackgroundColor(BACK);
            display.text(Component.text("✦", TextColor.color(0xE0C060)));
            return;
        }
        display.setBackgroundColor(FACE);
        display.text(text(card));
    }

    /**
     * @param card a card
     * @return its face: the rank over the pip, in the colour of the suit
     */
    private static Component text(Card card) {
        TextColor colour = card.suit().isRed() ? RED : BLACK;
        return Component.text(card.rank().getSymbol(), colour)
                .append(Component.newline())
                .append(Component.text(String.valueOf(card.suit().getSymbol()), colour));
    }

    /**
     * @param card a card
     * @return it written for chat, where a background colour is not available
     */
    public static Component inChat(Card card) {
        return Component.text(card.rank().getSymbol() + card.suit().getSymbol(),
                card.suit().isRed() ? NamedTextColor.RED : NamedTextColor.WHITE);
    }
}

package de.schnorrenbergers.bedwars.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Locale;

/**
 * The two directions text is turned in.
 * <p>
 * Everything the plugin writes is MiniMessage, but two places still want something else: an inventory
 * title is a legacy string, and an item name that is not told otherwise is drawn in italics. Both are
 * handled here rather than in every menu.
 */
public final class Text {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private Text() {
    }

    /**
     * @param text a MiniMessage line
     * @return it as a component
     */
    public static Component mini(String text) {
        return MINI.deserialize(text);
    }

    /**
     * @param text a MiniMessage line
     * @return it the way an item name or a lore line should look: no italics unless it asks for them
     */
    public static Component item(String text) {
        return mini(text).decoration(TextDecoration.ITALIC, false);
    }

    /**
     * @param text a MiniMessage line
     * @return the words of it, without any of the formatting - what a placeholder of another message needs,
     *         because a placeholder is inserted as text and would otherwise show its own tags
     */
    public static String plain(String text) {
        return PlainTextComponentSerializer.plainText().serialize(mini(text));
    }

    /**
     * @param number a level, a tier or a stage
     * @return it as a roman numeral, the way every minecraft level is written
     */
    public static String roman(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(number);
        };
    }

    /**
     * @param key a registry name or an enum constant, e.g. {@code fire_resistance} or {@code IRON_INGOT}
     * @return it the way it is written out for a player, e.g. {@code Fire Resistance}
     */
    public static String niceName(String key) {
        StringBuilder text = new StringBuilder();
        for (String word : key.toLowerCase(Locale.ROOT).split("_")) {
            if (word.isEmpty()) continue;
            if (!text.isEmpty()) text.append(' ');
            text.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return text.toString();
    }

    /**
     * @param seconds a countdown
     * @return it as {@code mm:ss}, which is how every countdown in the game is written
     */
    public static String clock(int seconds) {
        int safe = Math.max(0, seconds);
        return String.format("%02d:%02d", safe / 60, safe % 60);
    }

    /**
     * @param component a message
     * @return it as the legacy string an inventory title still has to be
     */
    public static String legacy(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    /**
     * @param text a MiniMessage line
     * @return it as a legacy string
     */
    public static String legacy(String text) {
        return legacy(mini(text));
    }
}

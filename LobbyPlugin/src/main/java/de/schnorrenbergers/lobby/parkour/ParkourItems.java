package de.schnorrenbergers.lobby.parkour;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * The three things a runner holds while they are on a course.
 * <p>
 * A parkour is played with the keyboard, so everything it needs has to be one click away rather than one
 * command: falling into a gap, wanting the run back from the top, and giving up are the three things that
 * happen on a course, and all three used to need typing.
 * <p>
 * The items are tagged rather than recognised by their material, so a player who brings a clock of their
 * own into the lobby does not restart their run by right clicking it.
 */
public final class ParkourItems {

    /** Which of the three an item is, or nothing when it is not one of them. */
    public enum Kind {
        /** Back to the last checkpoint. */
        CHECKPOINT,
        /** The whole course again, from the start, with the clock back at zero. */
        RESTART,
        /** Give up and go back to the start without a time. */
        QUIT
    }

    private static final NamespacedKey KEY = new NamespacedKey("lobby", "parkour-item");

    /** Where each of them sits, so they are always under the same key. */
    private static final int CHECKPOINT_SLOT = 0;
    private static final int RESTART_SLOT = 4;
    private static final int QUIT_SLOT = 8;

    private ParkourItems() {
    }

    /**
     * Lays the three items out in a runner's hotbar.
     *
     * @param player who is running
     */
    public static void give(Player player) {
        player.getInventory().setItem(CHECKPOINT_SLOT, build(Kind.CHECKPOINT,
                Material.RECOVERY_COMPASS,
                Component.text("Letzter Checkpoint", NamedTextColor.AQUA),
                "Zurück zum letzten Checkpoint.",
                "Die Zeit läuft weiter."));
        player.getInventory().setItem(RESTART_SLOT, build(Kind.RESTART,
                Material.CLOCK,
                Component.text("Neu starten", NamedTextColor.YELLOW),
                "Von vorne, mit der Uhr auf null."));
        player.getInventory().setItem(QUIT_SLOT, build(Kind.QUIT,
                Material.BARRIER,
                Component.text("Abbrechen", NamedTextColor.RED),
                "Lauf beenden und zurück zum Start."));
        player.getInventory().setHeldItemSlot(CHECKPOINT_SLOT);
    }

    /**
     * Takes them out again, leaving anything else in the hotbar alone.
     *
     * @param player who has stopped running
     */
    public static void take(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (kindOf(contents[slot]) != null) player.getInventory().setItem(slot, null);
        }
    }

    /**
     * @param stack something a player is holding
     * @return which of the three it is, or {@code null} when it is anything else
     */
    public static @Nullable Kind kindOf(@Nullable ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        String tag = stack.getItemMeta().getPersistentDataContainer()
                .get(KEY, PersistentDataType.STRING);
        if (tag == null) return null;
        try {
            return Kind.valueOf(tag);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static ItemStack build(Kind kind, Material material, Component name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        meta.lore(Arrays.stream(lore)
                .map(line -> (Component) Component.text(line, NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false))
                .toList());
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, kind.name());
        stack.setItemMeta(meta);
        return stack;
    }
}

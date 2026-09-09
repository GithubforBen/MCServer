package de.schnorrenbergers.poker.ui;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * The handful of things this plugin does to display entities, in one place.
 * <p>
 * Everything on a poker table is drawn rather than built: cards, chips, the heads over the chairs. Blocks
 * would be cheaper to write and wrong to live with - a table made of blocks cannot be dealt to twice a
 * minute without the chunk being rewritten, and a world that is rewritten twice a minute is a world nobody
 * can build in.
 * <p>
 * Every display spawned here is non-persistent. A casino that crashes mid-hand must not come back with
 * yesterday's cards lying on the felt.
 */
public final class Displays {

    /** Turning a flat thing onto its back, which is what makes a card lie on a table. */
    private static final Quaternionf FLAT = new Quaternionf(new AxisAngle4f((float) Math.toRadians(90), 1, 0, 0));

    private Displays() {
    }

    /**
     * A piece of text lying flat on a surface, like a playing card face up on the felt.
     *
     * @param at         where it lies, yaw included
     * @param scale      how big
     * @param background the colour behind the text
     * @return the display
     */
    public static TextDisplay flatText(Location at, float scale, Color background) {
        if (at.getWorld() == null) return null;
        return at.getWorld().spawn(at, TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.FIXED);
            display.setPersistent(false);
            display.setSeeThrough(false);
            display.setShadowed(false);
            display.setBackgroundColor(background);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f), FLAT, new Vector3f(scale, scale, scale), new Quaternionf()));
        });
    }

    /**
     * A piece of text that always turns to face whoever is reading it.
     *
     * @param at    where it floats
     * @param scale how big
     * @return the display
     */
    public static TextDisplay floatingText(Location at, float scale) {
        if (at.getWorld() == null) return null;
        return at.getWorld().spawn(at, TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.CENTER);
            display.setPersistent(false);
            display.setSeeThrough(true);
            display.setShadowed(false);
            display.setBackgroundColor(Color.fromARGB(90, 0, 0, 0));
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f), new Quaternionf(),
                    new Vector3f(scale, scale, scale), new Quaternionf()));
        });
    }

    /**
     * A block squashed into a disc, which is what a chip is.
     *
     * @param at       where it sits
     * @param material the colour of the chip
     * @param diameter how wide
     * @param height   how thick
     * @return the display
     */
    public static BlockDisplay disc(Location at, org.bukkit.Material material, float diameter, float height) {
        if (at.getWorld() == null) return null;
        return at.getWorld().spawn(at, BlockDisplay.class, display -> {
            display.setBlock(material.createBlockData());
            display.setPersistent(false);
            // a block is drawn from its corner, so it has to be pulled back by half of itself to sit
            // where it was put rather than up and to one side of it
            display.setTransformation(new Transformation(
                    new Vector3f(-diameter / 2f, 0f, -diameter / 2f), new Quaternionf(),
                    new Vector3f(diameter, height, diameter), new Quaternionf()));
        });
    }

    /**
     * An item hanging in the air, like the head over a chair.
     *
     * @param at    where it floats
     * @param stack what to show
     * @param scale how big
     * @return the display
     */
    public static ItemDisplay item(Location at, ItemStack stack, float scale) {
        if (at.getWorld() == null) return null;
        return at.getWorld().spawn(at, ItemDisplay.class, display -> {
            display.setItemStack(stack);
            display.setPersistent(false);
            display.setBillboard(Display.Billboard.FIXED);
            display.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f), new Quaternionf(),
                    new Vector3f(scale, scale, scale), new Quaternionf()));
        });
    }

    /**
     * Shows something to exactly one person and to nobody else.
     * <p>
     * This is what makes hole cards work: the card lies on the felt where everybody can see there is a
     * card, and only its owner sees which one it is.
     *
     * @param plugin the plugin the hiding is registered under
     * @param entity what to hide
     * @param only   who may see it, or {@code null} for nobody
     */
    public static void showOnlyTo(Plugin plugin, Entity entity, Player only) {
        if (entity == null) return;
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.equals(only)) {
                player.showEntity(plugin, entity);
            } else {
                player.hideEntity(plugin, entity);
            }
        }
    }

    /**
     * Makes something visible to everybody again, which is what a showdown does to the hole cards.
     */
    public static void showToAll(Plugin plugin, Entity entity) {
        if (entity == null) return;
        for (Player player : entity.getWorld().getPlayers()) {
            player.showEntity(plugin, entity);
        }
    }

    /**
     * Removes a set of displays and empties the list. Safe on entities that are already gone.
     *
     * @param entities the displays to take away
     */
    public static void removeAll(List<? extends Entity> entities) {
        for (Entity entity : new ArrayList<>(entities)) {
            if (entity != null && entity.isValid()) entity.remove();
        }
        entities.clear();
    }
}

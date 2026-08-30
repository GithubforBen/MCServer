package de.hems.paper.hologram;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Floating text in the world.
 * <p>
 * One {@link TextDisplay} carrying however many lines are given, joined with newlines - not one entity per
 * line. A display draws its own lines, so a stack of entities would only add entities to keep in step with
 * each other, and any of them could be left behind on a crash.
 * <p>
 * Never persistent. A hologram belongs to whatever is running - a generator, a parkour course, a
 * leaderboard - and a hologram that survives into the world file is a hologram nobody owns any more:
 * it cannot be updated, it cannot be found, and the only way to remove it is to go and kill it by hand.
 * That is also why {@link #remove()} exists and why {@link Holograms} keeps the ones a plugin made.
 */
public class Hologram {

    /** How far over the spot the text floats when nothing else is asked for. */
    public static final double DEFAULT_HEIGHT = 1.0d;

    private final List<Component> lines = new ArrayList<>();
    private Location anchor;
    private double height = DEFAULT_HEIGHT;
    private TextDisplay display;
    private boolean seeThrough;
    private Display.Billboard billboard = Display.Billboard.CENTER;

    /**
     * @param anchor where it belongs. The text floats {@link #DEFAULT_HEIGHT} over it.
     */
    public Hologram(Location anchor) {
        this.anchor = anchor.clone();
    }

    /**
     * @param anchor where it belongs
     * @param lines  what it says
     * @return the hologram, not yet in the world - call {@link #spawn()}
     */
    public static Hologram of(Location anchor, Component... lines) {
        return new Hologram(anchor).setLines(lines);
    }

    // ------------------------------------------------------------------- shaping

    /**
     * @param height how far over the anchor the text floats
     */
    public Hologram height(double height) {
        this.height = height;
        if (display != null && display.isValid()) display.teleport(textLocation());
        return this;
    }

    /**
     * @param seeThrough whether the text is visible through walls
     */
    public Hologram seeThrough(boolean seeThrough) {
        this.seeThrough = seeThrough;
        if (display != null && display.isValid()) display.setSeeThrough(seeThrough);
        return this;
    }

    /**
     * @param billboard which way it turns; {@link Display.Billboard#CENTER} always faces the reader
     */
    public Hologram billboard(Display.Billboard billboard) {
        this.billboard = billboard;
        if (display != null && display.isValid()) display.setBillboard(billboard);
        return this;
    }

    // -------------------------------------------------------------------- text

    /**
     * @param lines what it should say from now on
     */
    public Hologram setLines(Component... lines) {
        return setLines(Arrays.asList(lines));
    }

    /**
     * @param lines what it should say from now on
     */
    public Hologram setLines(List<Component> lines) {
        this.lines.clear();
        this.lines.addAll(lines);
        if (display != null && display.isValid()) display.text(joined());
        return this;
    }

    /**
     * @return what it says, as one component with newlines between the lines
     */
    public Component joined() {
        Component text = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) text = text.append(Component.newline());
            text = text.append(lines.get(i));
        }
        return text;
    }

    // ------------------------------------------------------------------- world

    /**
     * Puts it into the world, or updates the one that is already there.
     *
     * @return itself
     */
    public Hologram spawn() {
        if (display != null && display.isValid()) {
            display.text(joined());
            return this;
        }
        Location at = textLocation();
        if (at.getWorld() == null) return this;
        display = at.getWorld().spawn(at, TextDisplay.class, entity -> {
            entity.setBillboard(billboard);
            entity.setSeeThrough(seeThrough);
            entity.setPersistent(false);
            entity.text(joined());
        });
        Holograms.remember(this);
        return this;
    }

    /**
     * Moves it. Nothing happens to a hologram that is not in the world yet beyond remembering the spot.
     *
     * @param anchor where it belongs now
     */
    public Hologram move(Location anchor) {
        this.anchor = anchor.clone();
        if (display != null && display.isValid()) display.teleport(textLocation());
        return this;
    }

    /**
     * Takes it out of the world. Safe to call twice, and safe to call on one that never appeared.
     */
    public void remove() {
        if (display != null && display.isValid()) display.remove();
        display = null;
        Holograms.forget(this);
    }

    /**
     * @return whether it is in the world right now
     */
    public boolean isSpawned() {
        return display != null && display.isValid();
    }

    public @Nullable TextDisplay getDisplay() {
        return display;
    }

    public Location getAnchor() {
        return anchor.clone();
    }

    /**
     * @return where the text itself sits, which is the anchor plus the height
     */
    private Location textLocation() {
        return anchor.clone().add(0.0d, height, 0.0d);
    }
}

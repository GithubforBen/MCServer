package de.schnorrenbergers.lobby.parkour;

import de.hems.paper.hologram.Hologram;
import de.schnorrenbergers.lobby.LobbyWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The floating text of the parkour: a sign over every start, and a list of the best times.
 * <p>
 * A course built out of blocks in mid air is a course nobody finds, and a time nobody can see is a time
 * nobody races. Both are the same problem, so both are solved here - the text is the only part of a
 * parkour that says what it is.
 * <p>
 * Rebuilt wholesale rather than edited in place. There are a handful of courses, this runs when a time
 * changes or a course is edited, and a rebuild cannot drift out of step with what is in the file.
 */
public final class ParkourHolograms {

    /** How many places the board over a course shows. */
    private static final int BOARD_PLACES = 5;
    /** How far over its point the start sign floats. */
    private static final double SIGN_HEIGHT = 1.6d;
    /** And the board, which needs room for its lines under it. */
    private static final double BOARD_HEIGHT = 2.6d;

    private final ParkourStore store;
    /** Course name to the text hanging over it, so a rebuild can take the old ones down first. */
    private final Map<String, List<Hologram>> shown = new HashMap<>();

    public ParkourHolograms(ParkourStore store) {
        this.store = store;
    }

    /**
     * Puts the text of every course back up, taking down whatever was there.
     */
    public void refresh() {
        World world = LobbyWorld.get();
        clear();
        if (world == null) return;
        for (ParkourCourse course : store.all()) {
            List<Hologram> made = new ArrayList<>();
            if (course.getStart() != null) made.add(startSign(world, course));
            if (course.getBoard() != null) made.add(board(world, course));
            if (!made.isEmpty()) shown.put(course.getName(), made);
        }
    }

    /**
     * Takes every hologram of the parkour down again.
     */
    public void clear() {
        for (List<Hologram> holograms : shown.values()) {
            for (Hologram hologram : holograms) hologram.remove();
        }
        shown.clear();
    }

    /**
     * @return the sign over the start: what the course is called and what has to be done on it
     */
    private Hologram startSign(World world, ParkourCourse course) {
        Location at = course.getStart().toLocation(world);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.text(course.getDisplayName(), NamedTextColor.AQUA));
        if (!course.isComplete()) {
            lines.add(Component.text("noch nicht fertig gebaut", NamedTextColor.RED));
        } else {
            lines.add(Component.text(course.getCheckpoints().size() + " Checkpoints",
                    NamedTextColor.GRAY));
            List<ParkourStore.Record> best = store.leaderboard(course.getName(), 1);
            lines.add(best.isEmpty()
                    ? Component.text("noch keine Zeit", NamedTextColor.DARK_GRAY)
                    : Component.text("Rekord: " + ParkourService.format(best.getFirst().millis())
                            + "  " + best.getFirst().name(), NamedTextColor.YELLOW));
            lines.add(Component.text("Lauf hinein und los", NamedTextColor.DARK_GRAY));
        }
        return Hologram.of(at, lines.toArray(new Component[0])).height(SIGN_HEIGHT).spawn();
    }

    /**
     * @return the list of best times, or a note that nobody has finished yet
     */
    private Hologram board(World world, ParkourCourse course) {
        Location at = course.getBoard().toLocation(world);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.text("Bestzeiten", NamedTextColor.GOLD));
        lines.add(Component.text(course.getDisplayName(), NamedTextColor.WHITE));
        List<ParkourStore.Record> records = store.leaderboard(course.getName(), BOARD_PLACES);
        if (records.isEmpty()) {
            lines.add(Component.text("Noch niemand hat es geschafft.", NamedTextColor.DARK_GRAY));
        }
        for (int i = 0; i < records.size(); i++) {
            ParkourStore.Record record = records.get(i);
            lines.add(Component.text((i + 1) + ". ", NamedTextColor.DARK_AQUA)
                    .append(Component.text(record.name(), NamedTextColor.WHITE))
                    .append(Component.text("  " + ParkourService.format(record.millis()),
                            NamedTextColor.GRAY)));
        }
        return Hologram.of(at, lines.toArray(new Component[0])).height(BOARD_HEIGHT).spawn();
    }
}

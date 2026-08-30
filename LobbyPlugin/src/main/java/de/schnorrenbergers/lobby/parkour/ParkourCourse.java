package de.schnorrenbergers.lobby.parkour;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One course: where it starts, what has to be touched on the way, and where it ends.
 * <p>
 * The checkpoints are ordered and have to be taken in order. That is what makes a time mean something -
 * without it, the fastest way through any course is the shortcut, and the leaderboard measures who found
 * the best one rather than who jumps best.
 */
public class ParkourCourse {

    private final String name;
    private String displayName;
    private ParkourPoint start;
    private ParkourPoint finish;
    /** Where the leaderboard hangs, or {@code null} when this course does not show one. */
    private ParkourPoint board;
    private final List<ParkourPoint> checkpoints = new ArrayList<>();

    public ParkourCourse(String name) {
        this.name = name.toLowerCase(Locale.ROOT);
        this.displayName = name;
    }

    /**
     * @param name    the course
     * @param section its block in {@code parkour.yml}
     * @return the course as it is written there
     */
    public static ParkourCourse read(String name, ConfigurationSection section) {
        ParkourCourse course = new ParkourCourse(name);
        course.displayName = section.getString("display-name", name);
        course.start = ParkourPoint.read(section, "start");
        course.finish = ParkourPoint.read(section, "finish");
        course.board = ParkourPoint.read(section, "board");
        ConfigurationSection points = section.getConfigurationSection("checkpoints");
        if (points != null) {
            List<String> keys = new ArrayList<>(points.getKeys(false));
            keys.sort((one, other) -> Integer.compare(number(one), number(other)));
            for (String key : keys) {
                ParkourPoint point = ParkourPoint.read(points, key);
                if (point != null) course.checkpoints.add(point);
            }
        }
        return course;
    }

    /**
     * @param section its block in {@code parkour.yml}
     */
    public void write(ConfigurationSection section) {
        section.set("display-name", displayName);
        if (start != null) start.write(section, "start");
        if (finish != null) finish.write(section, "finish");
        if (board != null) board.write(section, "board");
        ConfigurationSection points = section.createSection("checkpoints");
        for (int i = 0; i < checkpoints.size(); i++) {
            checkpoints.get(i).write(points, String.valueOf(i));
        }
    }

    /**
     * @return whether the course can be run: it has somewhere to begin and somewhere to end
     */
    public boolean isComplete() {
        return start != null && finish != null;
    }

    /**
     * @param index which checkpoint
     * @return it, or {@code null} when the course does not have that many
     */
    public @Nullable ParkourPoint getCheckpoint(int index) {
        return index >= 0 && index < checkpoints.size() ? checkpoints.get(index) : null;
    }

    public List<ParkourPoint> getCheckpoints() {
        return List.copyOf(checkpoints);
    }

    public void addCheckpoint(ParkourPoint point) {
        checkpoints.add(point);
    }

    /**
     * @return the checkpoint that was taken away, or {@code null} when there was none left
     */
    public @Nullable ParkourPoint removeLastCheckpoint() {
        return checkpoints.isEmpty() ? null : checkpoints.removeLast();
    }

    public void clearCheckpoints() {
        checkpoints.clear();
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public @Nullable ParkourPoint getStart() {
        return start;
    }

    public void setStart(ParkourPoint start) {
        this.start = start;
    }

    public @Nullable ParkourPoint getFinish() {
        return finish;
    }

    /**
     * @return where the list of best times hangs, or {@code null} when the course shows none
     */
    public @Nullable ParkourPoint getBoard() {
        return board;
    }

    public void setBoard(@Nullable ParkourPoint board) {
        this.board = board;
    }

    public void setFinish(ParkourPoint finish) {
        this.finish = finish;
    }

    /**
     * @param key a checkpoint key out of the file
     * @return the number in it, so that 10 sorts after 9 rather than after 1
     */
    private static int number(String key) {
        try {
            return Integer.parseInt(key);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }
}

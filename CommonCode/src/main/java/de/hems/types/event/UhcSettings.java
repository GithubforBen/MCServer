package de.hems.types.event;

/**
 * The knobs of a run event, read out of the free settings an {@link EventData} carries.
 * <p>
 * They live as plain strings on the event so the website, the game and the config all speak the same
 * language without another table. This is the one place that knows their names and their defaults.
 */
public final class UhcSettings {

    /** Whether a death ends the run. */
    public static final String HARDCORE = "hardcore";
    /** How many players one run may have. */
    public static final String TEAM_SIZE = "team-size";
    /** How often the same player may run. */
    public static final String MAX_RUNS = "max-runs";
    /** Whether a run may start below the full team size. */
    public static final String ALLOW_UNDERMANNED = "allow-undermanned";

    private static final int DEFAULT_TEAM_SIZE = 1;
    private static final int DEFAULT_MAX_RUNS = 3;

    /** The knobs as the settings panel draws them. */
    public static final java.util.List<EventSetting> SETTINGS = java.util.List.of(
            EventSetting.toggle(HARDCORE, "Hardcore", "RED_DYE", true,
                    "Ein Tod beendet den Lauf."),
            EventSetting.choice(TEAM_SIZE, "Teamgröße", "PLAYER_HEAD", DEFAULT_TEAM_SIZE, "",
                    null, new int[]{1, 2, 3, 4}, "So viele laufen zusammen."),
            EventSetting.choice(MAX_RUNS, "Versuche", "CLOCK", DEFAULT_MAX_RUNS, "", "unbegrenzt",
                    new int[]{1, 3, 5, 10, 0}, "Pro Person."),
            EventSetting.toggle(ALLOW_UNDERMANNED, "Unterbesetzt starten", "LIME_DYE", true,
                    "Eine kleinere Gruppe darf trotzdem los.", "Sie hat es dann schwerer."));

    private final EventData event;

    public UhcSettings(EventData event) {
        this.event = event;
    }

    /**
     * @return whether a death ends the run - the point of the whole thing, so it is on by default
     */
    public boolean isHardcore() {
        return event.getFlag(HARDCORE, true);
    }

    /**
     * @return how many players a run may have, at least one
     */
    public int getTeamSize() {
        return Math.max(1, event.getNumber(TEAM_SIZE, DEFAULT_TEAM_SIZE));
    }

    /**
     * @return how often one player may run, or {@link Integer#MAX_VALUE} when it is not limited
     */
    public int getMaxRuns() {
        int runs = event.getNumber(MAX_RUNS, DEFAULT_MAX_RUNS);
        return runs <= 0 ? Integer.MAX_VALUE : runs;
    }

    /**
     * @return whether a group smaller than the team size may start anyway, taking the handicap
     */
    public boolean isAllowUndermanned() {
        return event.getFlag(ALLOW_UNDERMANNED, true);
    }

    public void setHardcore(boolean hardcore) {
        event.setSetting(HARDCORE, String.valueOf(hardcore));
    }

    public void setTeamSize(int teamSize) {
        event.setSetting(TEAM_SIZE, String.valueOf(Math.max(1, teamSize)));
    }

    public void setMaxRuns(int maxRuns) {
        event.setSetting(MAX_RUNS, String.valueOf(Math.max(0, maxRuns)));
    }

    public void setAllowUndermanned(boolean allow) {
        event.setSetting(ALLOW_UNDERMANNED, String.valueOf(allow));
    }
}

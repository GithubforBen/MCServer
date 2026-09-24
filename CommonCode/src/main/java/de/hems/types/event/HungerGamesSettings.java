package de.hems.types.event;

import java.util.List;

/**
 * The knobs of a hunger games event, read out of the free settings an {@link EventData} carries.
 * <p>
 * The game runs in three stretches, and most of the knobs say how long each one is: a grace period in
 * which nobody can be hurt by another player, open play on the full map, and then the border, which closes
 * in over a set time until what is left of the map is the arena for the showdown.
 * <p>
 * The server name is written by whoever put the arena up, the same way it is for bedwars and poker: the
 * arena reads its own name, finds its event by it, and takes the rules from there.
 */
public final class HungerGamesSettings {

    /** The server the game is played on, written when it is started. */
    public static final String SERVER = "hg.server";
    /** How many have to be there before it starts. */
    public static final String MIN_PLAYERS = "hg.min-players";
    /** How long nobody can hurt anybody else, in minutes. */
    public static final String GRACE_MINUTES = "hg.grace-minutes";
    /** How wide the border is at the start, in blocks. */
    public static final String BORDER_START = "hg.border-start";
    /** How wide it is at the showdown, in blocks. */
    public static final String BORDER_END = "hg.border-end";
    /** When the border starts to close in, in minutes after the start. */
    public static final String SHRINK_AFTER = "hg.shrink-after";
    /** How long it takes to close, in minutes. */
    public static final String SHRINK_MINUTES = "hg.shrink-minutes";
    /** How often a supply package comes down, in minutes, zero for never. */
    public static final String DROP_MINUTES = "hg.drop-minutes";
    /** Whether the last ones standing glow once the border has closed. */
    public static final String SHOWDOWN_GLOW = "hg.showdown-glow";
    /**
     * How many play together. Read, but only ever one so far: teams need their own placing and friendly
     * fire rules, and those are not built yet. The key exists so an event set up now keeps its meaning
     * once they are.
     */
    public static final String TEAM_SIZE = "hg.team-size";

    public static final int DEFAULT_MIN_PLAYERS = 2;
    public static final int DEFAULT_GRACE_MINUTES = 1;
    public static final int DEFAULT_BORDER_START = 500;
    public static final int DEFAULT_BORDER_END = 20;
    public static final int DEFAULT_SHRINK_AFTER = 10;
    public static final int DEFAULT_SHRINK_MINUTES = 10;
    public static final int DEFAULT_DROP_MINUTES = 4;

    /** The knobs as the settings panel draws them. */
    public static final List<EventSetting> SETTINGS = List.of(
            EventSetting.choice(MIN_PLAYERS, "Mindestens Spieler", "PLAYER_HEAD", DEFAULT_MIN_PLAYERS, "",
                    null, new int[]{2, 3, 4, 6, 8, 12, 16},
                    "Vorher wird gewartet, auch wenn", "die Eventzeit schon da ist."),
            EventSetting.choice(GRACE_MINUTES, "Schutzzeit", "SHIELD", DEFAULT_GRACE_MINUTES, "Min", "keine",
                    new int[]{0, 1, 2, 3, 5},
                    "So lange kann niemand einem", "anderen Spieler schaden."),
            EventSetting.choice(BORDER_START, "Grenze am Anfang", "MAP", DEFAULT_BORDER_START, "Blöcke",
                    null, new int[]{200, 300, 500, 750, 1000, 1500, 2000},
                    "Durchmesser der Welt beim Start."),
            EventSetting.choice(BORDER_END, "Grenze beim Showdown", "BARRIER", DEFAULT_BORDER_END, "Blöcke",
                    null, new int[]{10, 20, 30, 50, 80},
                    "So klein wird die Welt am Ende."),
            EventSetting.choice(SHRINK_AFTER, "Grenze schrumpft ab", "CLOCK", DEFAULT_SHRINK_AFTER, "Min",
                    null, new int[]{3, 5, 10, 15, 20, 30},
                    "Minuten nach dem Start."),
            EventSetting.choice(SHRINK_MINUTES, "Schrumpfdauer", "REPEATER", DEFAULT_SHRINK_MINUTES, "Min",
                    null, new int[]{3, 5, 10, 15, 20, 30},
                    "So lange braucht die Grenze", "bis zur Showdown-Größe."),
            EventSetting.choice(DROP_MINUTES, "Supply Drops", "CHEST", DEFAULT_DROP_MINUTES, "Min", "aus",
                    new int[]{0, 2, 3, 4, 5, 8, 10},
                    "Alle so viele Minuten fällt ein Paket", "mit gutem Loot in die Welt."),
            EventSetting.toggle(SHOWDOWN_GLOW, "Leuchten im Showdown", "GLOWSTONE_DUST", true,
                    "Wer im Showdown noch lebt, leuchtet.", "Verstecken geht dann nicht mehr."));

    private final EventData event;

    public HungerGamesSettings(EventData event) {
        this.event = event;
    }

    public void applyDefaults() {
        EventSetting.applyDefaults(event, SETTINGS);
    }

    /**
     * @return the server the game runs on, or {@code null} while it has not been started yet
     */
    public String getServer() {
        String server = event.getSetting(SERVER, "");
        return server == null || server.isBlank() ? null : server;
    }

    public void setServer(String server) {
        event.setSetting(SERVER, server == null ? "" : server);
    }

    public int getMinPlayers() {
        return Math.max(2, event.getNumber(MIN_PLAYERS, DEFAULT_MIN_PLAYERS));
    }

    public int getGraceMinutes() {
        return Math.max(0, event.getNumber(GRACE_MINUTES, DEFAULT_GRACE_MINUTES));
    }

    public int getBorderStart() {
        return Math.max(getBorderEnd(), event.getNumber(BORDER_START, DEFAULT_BORDER_START));
    }

    public int getBorderEnd() {
        return Math.max(5, event.getNumber(BORDER_END, DEFAULT_BORDER_END));
    }

    public int getShrinkAfterMinutes() {
        return Math.max(0, event.getNumber(SHRINK_AFTER, DEFAULT_SHRINK_AFTER));
    }

    public int getShrinkMinutes() {
        return Math.max(1, event.getNumber(SHRINK_MINUTES, DEFAULT_SHRINK_MINUTES));
    }

    /**
     * @return minutes between supply packages, zero when there are none
     */
    public int getDropMinutes() {
        return Math.max(0, event.getNumber(DROP_MINUTES, DEFAULT_DROP_MINUTES));
    }

    public boolean isShowdownGlow() {
        return event.getFlag(SHOWDOWN_GLOW, true);
    }

    /**
     * @return how many play together - always one until teams are built, whatever is stored
     */
    public int getTeamSize() {
        return 1;
    }

    /**
     * @return how long a game takes at the most before the border has closed, in minutes
     */
    public int getMinutesToShowdown() {
        return getShrinkAfterMinutes() + getShrinkMinutes();
    }
}

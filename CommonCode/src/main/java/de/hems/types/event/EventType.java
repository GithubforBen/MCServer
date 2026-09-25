package de.hems.types.event;

import java.io.Serializable;

/**
 * The kinds of event the network knows.
 * <p>
 * Everything that needs its own rules lives in code and gets an entry here. The two open ones -
 * {@link #SIMPLE} and {@link #OTHER_WORLD} - carry no logic of their own, which is what lets an admin
 * announce something without a developer having to touch the plugin.
 */
public enum EventType implements Serializable {

    // title, only once, timed (a race with runs), ranked, counts kills, reports results, server setting
    /** Just an announcement with a time frame. No mechanics attached. */
    SIMPLE("Einfaches Event", false, false, false, false, false, null),
    /** Everyone moves to another world for a while. */
    OTHER_WORLD("Andere Welt", false, false, false, false, false, null),
    /** Opens the End for good. Happens exactly once. */
    END("Das End öffnet", true, false, false, false, false, null),
    /** Ultra hardcore: kill every boss as fast as possible. */
    UHC_BOSSES("Alle Bosse (UHC)", false, true, true, false, false, null),
    /** Ultra hardcore: kill the dragon as fast as possible. */
    UHC_DRAGON("Enderdrache (UHC)", false, true, true, false, false, null),
    /**
     * A round of bedwars at a fixed time. The network puts a bedwars server up when it starts and takes
     * everybody who is in the lobby along; how big the teams are is a setting of the event. The round
     * reports the order the teams went out in and everybody's kills.
     */
    BEDWARS("Bedwars", false, false, true, true, true, BedwarsEventSettings.SERVER),
    /**
     * A poker night. The network puts a casino server up when it starts and takes everybody in the lobby
     * along; the stakes are the bits of the network, and what the house keeps of every pot is a setting of
     * the event. Ranked by its own record of the tables, not by reported results.
     */
    POKER("Pokernacht", false, false, true, false, false, PokerEventSettings.SERVER),
    /**
     * Hunger games: everybody into one prepared world, loot in the middle, supply drops over the map, and a
     * border that closes in until only one is left.
     */
    HUNGER_GAMES("Hunger Games", false, false, true, true, true, HungerGamesSettings.SERVER);

    private final String title;
    private final boolean onlyOnce;
    private final boolean timed;
    private final boolean ranked;
    private final boolean kills;
    private final boolean results;
    private final String serverKey;

    EventType(String title, boolean onlyOnce, boolean timed, boolean ranked, boolean kills, boolean results,
              String serverKey) {
        this.title = title;
        this.onlyOnce = onlyOnce;
        this.timed = timed;
        this.ranked = ranked;
        this.kills = kills;
        this.results = results;
        this.serverKey = serverKey;
    }

    /**
     * Whether its game server reports placings and kills as {@link EventResultData}, which the launcher
     * keeps and pays the rewards out of when the event is over.
     *
     * @return whether the event is settled from reported results
     */
    public boolean reportsResults() {
        return results;
    }

    /**
     * @return the setting the name of the event's own server is written to, or {@code null} for a kind that
     *         is not played on a server of its own
     */
    public String getServerKey() {
        return serverKey;
    }

    public String getTitle() {
        return title;
    }

    /**
     * @return whether the network may only ever hold one of these
     */
    public boolean isOnlyOnce() {
        return onlyOnce;
    }

    /**
     * @return whether runs of this event are raced against a clock, which is what needs a queue, a
     *         leaderboard and its own server
     */
    public boolean isTimed() {
        return timed;
    }

    /**
     * Whether the event finds out who did how well, which is what rewards are handed out by. An event that
     * ranks nobody can only be announced, not won.
     *
     * @return whether rewards mean anything on this type
     */
    public boolean isRanked() {
        return ranked;
    }

    /**
     * @return whether the event counts kills, which is what makes a reward "from five kills on" possible
     */
    public boolean countsKills() {
        return kills;
    }

    /**
     * Whether the type carries mechanics that live in code. Scheduling is open to admins either way - what
     * this says is whether anything happens beyond the announcement.
     *
     * @return whether code reacts to this type
     */
    public boolean hasMechanics() {
        return this != SIMPLE;
    }

    /**
     * @param name the name as it arrived from a command or the website
     * @return the type, or {@code null} if there is none by that name
     */
    public static EventType byName(String name) {
        if (name == null) return null;
        for (EventType type : values()) {
            if (type.name().equalsIgnoreCase(name)) return type;
        }
        return null;
    }
}

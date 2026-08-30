package de.schnorrenbergers.bedwars.config;

import de.schnorrenbergers.bedwars.game.Standings;
import de.schnorrenbergers.bedwars.game.timeline.TimelineEvent;
import de.schnorrenbergers.bedwars.util.ConfigFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * When the round does things to itself, out of {@code timeline.yml}.
 * <p>
 * The hypixel schedule is the default, but the whole point of this file is that it is short enough to
 * rewrite: a test round wants bed destruction after two minutes, not after thirty. The scoring weights at
 * the bottom belong here as well, because they only ever matter for the event that ends the round on time.
 */
public final class TimelineSettings {

    private final ConfigFile file;
    private final List<TimelineEvent> events = new ArrayList<>();

    private int dragonsPerTeam;
    private int dragonBuffDragons;
    private double dragonHealth;
    private int dragonHeight;
    private double dragonRadius;
    private Standings.Weights weights = new Standings.Weights(10, 5, 1);

    public TimelineSettings() {
        file = new ConfigFile("timeline.yml");
        load();
    }

    /**
     * Reads the file, writing the hypixel schedule into it the first time.
     */
    public void load() {
        file.reload();
        events.clear();
        file.section("events",
                "What the round does to itself, and how many seconds in.",
                "'action' has to be one of:",
                "  GENERATOR_TIER - raises the generators in the middle to 'tier'",
                "  BED_DESTRUCTION - every bed still standing falls",
                "  SUDDEN_DEATH - a dragon per living team",
                "  GAME_END - the round ends and the score below decides it",
                "  ANNOUNCE - nothing but the message, for an addon to hang itself on",
                "Shorten these while testing: waiting half an hour for bed destruction is not a test.");
        writeDefaults();
        read();
        file.save();
    }

    private void writeDefaults() {
        generator("diamond-2", "<aqua>Diamond II", 360, "DIAMOND", 2,
                "The diamond generators in the middle start dropping faster. Holding the middle is worth"
                        + " more from here on than it was before.");
        generator("emerald-2", "<green>Emerald II", 720, "EMERALD", 2,
                "The emerald generators speed up. Emeralds buy the armour and the diamond sword, so this"
                        + " is when fights start being decided by what people are wearing.");
        generator("diamond-3", "<aqua>Diamond III", 1080, "DIAMOND", 3,
                "Diamonds again, faster still. Team upgrades that were out of reach are now affordable.");
        generator("emerald-3", "<green>Emerald III", 1440, "EMERALD", 3,
                "Emeralds at their fastest. This is the last thing that makes anybody stronger before"
                        + " the beds fall.");
        event("bed-destruction", "<red>Bed Destruction", 1800, TimelineEvent.Action.BED_DESTRUCTION,
                "Every bed still standing is destroyed at once. From here nobody respawns: one death and"
                        + " you are out of the round for good.");
        event("sudden-death", "<dark_red>Sudden Death", 2400, TimelineEvent.Action.SUDDEN_DEATH,
                "A dragon appears over the middle for every team that is still alive, and it hunts"
                        + " everybody who is not on its own team. Rounds do not last long after this.");
        event("game-end", "<gold>Game End", 3000, TimelineEvent.Action.GAME_END,
                "The hard time limit. The round stops and is decided on points: ten for a bed, five for"
                        + " a final kill, one for an ordinary kill. A tie at the top means nobody won.");

        file.section("sudden-death", "The dragons of the sudden death event.");
        file.get("sudden-death.dragons-per-team", 1,
                "How many dragons every living team is given.");
        file.get("sudden-death.dragon-buff-dragons", 1,
                "How many more a team that bought the dragon buff gets.");
        file.get("sudden-death.health", 200.0d,
                "How much health one dragon has. 200 is what a vanilla dragon has.");
        file.get("sudden-death.height", 25,
                "How far above the middle of the map the dragons appear.");
        file.get("sudden-death.radius", 140.0d,
                "How far a dragon may drift from the middle before it is put back.",
                "Without this they wander off across the void and the event is over without a fight.");

        file.section("points",
                "What decides a round that runs into the hard time limit.",
                "The numbers are the whole ranking: whoever adds up to most wins, and a tie at the top",
                "means nobody won - a drawn round is more honest than a coin toss.");
        file.get("points.bed", 10, "Points for every bed a team broke.");
        file.get("points.final-kill", 5, "Points for every final kill.");
        file.get("points.kill", 1, "Points for every ordinary kill.");
    }

    /**
     * Writes one generator step, without touching what is already in the file.
     */
    private void generator(String id, String displayName, int seconds, String type, int tier,
                           String description) {
        String path = "events." + id;
        event(id, displayName, seconds, TimelineEvent.Action.GENERATOR_TIER, description);
        file.get(path + ".generator", type);
        file.get(path + ".tier", tier);
    }

    /**
     * @param description what the event means in plain words. The sidebar has room for a name and a
     *                    countdown and nothing else, so "Diamond II in 3:20" tells a player when
     *                    something happens without ever telling them what - this is where that goes.
     */
    private void event(String id, String displayName, int seconds, TimelineEvent.Action action,
                       String description) {
        String path = "events." + id;
        file.get(path + ".at", seconds);
        file.get(path + ".display-name", displayName);
        file.get(path + ".action", action.name());
        file.get(path + ".description", description);
    }

    // ------------------------------------------------------------------ reading

    private void read() {
        for (String id : file.keys("events")) {
            String path = "events." + id;
            events.add(new TimelineEvent(id,
                    file.read(path + ".display-name", id),
                    file.read(path + ".description", ""),
                    Math.max(0, file.read(path + ".at", 0)),
                    TimelineEvent.Action.byName(file.read(path + ".action",
                            TimelineEvent.Action.ANNOUNCE.name())),
                    file.read(path + ".generator", ""),
                    Math.max(1, file.read(path + ".tier", 1))));
        }
        // by time, not by the order they are written: a schedule read out of order would fire an event
        // early and then never fire the ones before it
        events.sort(Comparator.comparingInt(TimelineEvent::seconds));

        dragonsPerTeam = Math.max(0, file.read("sudden-death.dragons-per-team", 1));
        dragonBuffDragons = Math.max(0, file.read("sudden-death.dragon-buff-dragons", 1));
        dragonHealth = Math.max(1.0d, file.read("sudden-death.health", 200.0d));
        dragonHeight = Math.max(0, file.read("sudden-death.height", 25));
        dragonRadius = Math.max(8.0d, file.read("sudden-death.radius", 140.0d));
        weights = new Standings.Weights(
                file.read("points.bed", 10),
                file.read("points.final-kill", 5),
                file.read("points.kill", 1));
    }

    // ------------------------------------------------------------------ lookups

    /**
     * @return every event of the round, earliest first
     */
    public List<TimelineEvent> getEvents() {
        return List.copyOf(events);
    }

    public int getDragonsPerTeam() {
        return dragonsPerTeam;
    }

    public int getDragonBuffDragons() {
        return dragonBuffDragons;
    }

    public double getDragonHealth() {
        return dragonHealth;
    }

    public int getDragonHeight() {
        return dragonHeight;
    }

    public double getDragonRadius() {
        return dragonRadius;
    }

    /**
     * @return what a bed, a final kill and a kill are worth when the time limit decides the round
     */
    public Standings.Weights getWeights() {
        return weights;
    }

    public ConfigFile getFile() {
        return file;
    }
}

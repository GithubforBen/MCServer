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
        generator("diamond-2", "<aqua>Diamond II", 360, "DIAMOND", 2);
        generator("emerald-2", "<green>Emerald II", 720, "EMERALD", 2);
        generator("diamond-3", "<aqua>Diamond III", 1080, "DIAMOND", 3);
        generator("emerald-3", "<green>Emerald III", 1440, "EMERALD", 3);
        event("bed-destruction", "<red>Bed Destruction", 1800, TimelineEvent.Action.BED_DESTRUCTION);
        event("sudden-death", "<dark_red>Sudden Death", 2400, TimelineEvent.Action.SUDDEN_DEATH);
        event("game-end", "<gold>Game End", 3000, TimelineEvent.Action.GAME_END);

        file.section("sudden-death", "The dragons of the sudden death event.");
        file.get("sudden-death.dragons-per-team", 1,
                "How many dragons every living team is given.");
        file.get("sudden-death.dragon-buff-dragons", 1,
                "How many more a team that bought the dragon buff gets.");
        file.get("sudden-death.health", 200.0d,
                "How much health one dragon has. 200 is what a vanilla dragon has.");
        file.get("sudden-death.height", 25,
                "How far above the middle of the map the dragons appear.");
        file.get("sudden-death.radius", 60.0d,
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
    private void generator(String id, String displayName, int seconds, String type, int tier) {
        String path = "events." + id;
        event(id, displayName, seconds, TimelineEvent.Action.GENERATOR_TIER);
        file.get(path + ".generator", type);
        file.get(path + ".tier", tier);
    }

    private void event(String id, String displayName, int seconds, TimelineEvent.Action action) {
        String path = "events." + id;
        file.get(path + ".at", seconds);
        file.get(path + ".display-name", displayName);
        file.get(path + ".action", action.name());
    }

    // ------------------------------------------------------------------ reading

    private void read() {
        for (String id : file.keys("events")) {
            String path = "events." + id;
            events.add(new TimelineEvent(id,
                    file.read(path + ".display-name", id),
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
        dragonRadius = Math.max(8.0d, file.read("sudden-death.radius", 60.0d));
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

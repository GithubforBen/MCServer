package de.schnorrenbergers.hungergames;

import de.hems.paper.event.EventService;
import de.hems.types.event.EventData;
import de.hems.types.event.EventType;
import de.hems.types.event.HungerGamesSettings;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

/**
 * Which hunger games event this arena is for, and under what rules.
 * <p>
 * A server is told its own name and nothing else. The lobby wrote that name onto the event before it ordered
 * the server, so the arena looks itself up once at startup and takes its rules from the event it finds.
 * <p>
 * An arena that finds no event is a test arena: it plays on the defaults, can be started by hand with
 * {@code /hg start}, and reports nothing, because there is no event for a result to belong to.
 */
public final class ArenaContext {

    private static volatile EventData event;
    private static volatile HungerGamesSettings settings;

    private ArenaContext() {
    }

    /**
     * Looks this server up in the calendar. Blocks, and is meant to: the border and the timings have to be
     * known before anybody walks in.
     *
     * @param server what this server is called on the network
     */
    public static void load(String server) {
        EventService.refreshBlocking();
        for (EventData candidate : EventService.getEvents()) {
            if (candidate.getType() != EventType.HUNGER_GAMES) continue;
            if (!server.equalsIgnoreCase(new HungerGamesSettings(candidate).getServer())) continue;
            event = candidate;
            settings = new HungerGamesSettings(candidate);
            Bukkit.getLogger().info("This arena is playing \"" + candidate.getName() + "\" - border "
                    + settings.getBorderStart() + " to " + settings.getBorderEnd() + ", showdown after "
                    + settings.getMinutesToShowdown() + " minutes.");
            return;
        }
        EventData standalone = new EventData("Testarena", EventType.HUNGER_GAMES,
                System.currentTimeMillis(), System.currentTimeMillis() + 86_400_000L);
        settings = new HungerGamesSettings(standalone);
        settings.applyDefaults();
        Bukkit.getLogger().info("This arena belongs to no event - it plays on the defaults, waits for /hg start "
                + "and reports nothing.");
    }

    /**
     * @return the event as fresh as the network has it, or {@code null} for a test arena
     */
    public static @Nullable EventData getEvent() {
        if (event == null) return null;
        EventData fresh = EventService.getEvent(event.getId());
        return fresh == null ? event : fresh;
    }

    public static boolean hasEvent() {
        return event != null;
    }

    /**
     * The rules as they were read at startup. They deliberately do not follow edits made while the game
     * runs: a border that changes its mind halfway through is a different game for the ones still in it.
     *
     * @return the rules
     */
    public static HungerGamesSettings getSettings() {
        return settings;
    }

    public static String getTitle() {
        return event == null ? "Hunger Games" : event.getName();
    }
}

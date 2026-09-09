package de.schnorrenbergers.poker;

import de.hems.paper.event.EventService;
import de.hems.types.event.EventData;
import de.hems.types.event.EventType;
import de.hems.types.event.PokerEventSettings;
import de.hems.types.poker.PokerFormat;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

/**
 * Which poker night this server is dealing, and under what terms.
 * <p>
 * A server is told its own name and nothing else. The event holds the rest - the buy-in, the blinds, what
 * the house keeps - and the two find each other because whoever ordered the casino wrote its name onto the
 * event before ordering it. So this looks the network up once, at startup, and blocks while it does:
 * the stakes have to be known before the first player can sit down, and dealing at made-up stakes for one
 * hand would be dealing for real bits at the wrong price.
 * <p>
 * A casino that finds no event is not broken. It runs as a house table on the defaults, which is what a
 * server started by hand for testing should do - and it takes no ranking, because there is no night for a
 * ranking to belong to.
 */
public final class CasinoContext {

    private static volatile EventData event;
    private static volatile PokerEventSettings settings;
    private static volatile String serverName;

    private CasinoContext() {
    }

    /**
     * Looks this server up in the calendar. Blocks, and is meant to.
     *
     * @param server what this server is called on the network
     */
    public static void load(String server) {
        serverName = server;
        EventService.refreshBlocking();
        for (EventData candidate : EventService.getEvents()) {
            if (candidate.getType() != EventType.POKER) continue;
            if (!server.equalsIgnoreCase(new PokerEventSettings(candidate).getServer())) continue;
            event = candidate;
            settings = new PokerEventSettings(candidate);
            Bukkit.getLogger().info("This casino is dealing \"" + candidate.getName() + "\" - "
                    + settings.getFormat().getTitle() + ", buy-in " + settings.getBuyIn()
                    + ", blinds " + settings.getSmallBlind() + "/" + settings.getBigBlind()
                    + ", house " + settings.getRakeText());
            return;
        }
        // no event: a table on the defaults, and nothing is recorded anywhere
        EventData standalone = new EventData("Haustisch", EventType.POKER,
                System.currentTimeMillis(), System.currentTimeMillis() + 86_400_000L);
        settings = new PokerEventSettings(standalone);
        settings.applyDefaults();
        Bukkit.getLogger().info("This casino belongs to no poker night - it deals on the defaults and "
                + "keeps no ranking.");
    }

    /**
     * @return the night being dealt, or {@code null} for a house table
     */
    public static @Nullable EventData getEvent() {
        return event;
    }

    /**
     * @return whether this casino belongs to a poker night, which is what decides if anything is recorded
     */
    public static boolean hasEvent() {
        return event != null;
    }

    public static PokerEventSettings getSettings() {
        return settings;
    }

    public static PokerFormat getFormat() {
        return settings == null ? PokerFormat.CASH : settings.getFormat();
    }

    public static String getServerName() {
        return serverName;
    }

    /**
     * @return what to call the night in a message
     */
    public static String getTitle() {
        return event == null ? "Haustisch" : event.getName();
    }

    /**
     * Takes over a change to the event that arrived from the launcher.
     * <p>
     * The stakes of a running night are not meant to move under the players - a raised buy-in halfway
     * through would make the ranking compare two different games - so only the event itself is refreshed
     * here, for the name and the end time. What the tables deal on stays as it was read at startup.
     *
     * @param updated the event as it now stands
     */
    public static void refresh(EventData updated) {
        if (event == null || updated == null) return;
        if (!event.getId().equals(updated.getId())) return;
        event = updated;
    }
}

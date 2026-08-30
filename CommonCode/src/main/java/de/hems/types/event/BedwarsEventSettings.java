package de.hems.types.event;

/**
 * The knobs of a bedwars event, read out of the free settings an {@link EventData} carries.
 * <p>
 * Only two things are decided here, and one of them is really only decided once: how big the teams are,
 * and which server the round ended up on. The second is written by whoever started the round, and it is
 * what lets the bedwars server recognise which event it was started for - a server is told its name and
 * nothing else, so the event has to be the one holding the two together.
 */
public final class BedwarsEventSettings {

    /** How many players fit into one team. */
    public static final String TEAM_SIZE = "team-size";
    /** The server the round is being played on, written when it is started. */
    public static final String SERVER = "server";

    /** Doubles, which is what the maps that ship with the network are built for. */
    public static final int DEFAULT_TEAM_SIZE = 2;
    /** The largest team the mode list knows what to do with. */
    public static final int MAX_TEAM_SIZE = 8;

    private final EventData event;

    public BedwarsEventSettings(EventData event) {
        this.event = event;
    }

    /**
     * @return how many players fit into one team, between one and {@link #MAX_TEAM_SIZE}
     */
    public int getTeamSize() {
        return Math.max(1, Math.min(MAX_TEAM_SIZE,
                event.getNumber(TEAM_SIZE, DEFAULT_TEAM_SIZE)));
    }

    public void setTeamSize(int teamSize) {
        event.setSetting(TEAM_SIZE, String.valueOf(Math.max(1, Math.min(MAX_TEAM_SIZE, teamSize))));
    }

    /**
     * @return the server the round runs on, or {@code null} while it has not been started yet
     */
    public String getServer() {
        String server = event.getSetting(SERVER, "");
        return server == null || server.isBlank() ? null : server;
    }

    public void setServer(String server) {
        event.setSetting(SERVER, server == null ? "" : server);
    }

    /**
     * The mode a team size means.
     * <p>
     * These are the ids of {@code modes.yml} on the bedwars server, which is the only place that decides
     * how many teams play - the event says how big a team is and the mode list says the rest.
     *
     * @return the id of the mode to play
     */
    public String getMode() {
        return switch (getTeamSize()) {
            case 1 -> "solo";
            case 2 -> "doubles";
            case 3 -> "trio";
            default -> "quad";
        };
    }
}

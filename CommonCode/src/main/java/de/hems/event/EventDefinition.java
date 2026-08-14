package de.hems.event;

import de.hems.event.ranking.RankingStrategies;
import de.hems.event.ranking.RankingStrategy;
import de.hems.types.FileType;
import de.hems.types.ServerTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * The blueprint of a kind of event - bedwars, a tournament, a speedrun, whatever comes next.
 * <p>
 * A definition describes everything that is the same for every event of that kind: which plugin brings the
 * game, on what kind of server it runs, how many teams there are and how those teams are compared. A new
 * kind of event is a new subclass that is handed to {@link EventRegistry#register(EventDefinition)} - the
 * calendar, the UI and the API pick it up without any changes.
 * <p>
 * The definition holds the behaviour, {@link ScheduledEvent} holds the data of one concrete event. That
 * split is what lets an event travel through the network as plain data while every server resolves the
 * behaviour locally.
 */
public abstract class EventDefinition {

    /**
     * @return the id this kind of event is stored and synchronised with, e.g. {@code BEDWARS}
     */
    public abstract String getId();

    /**
     * @return the name shown in the calendar
     */
    public abstract String getDisplayName();

    /**
     * @return a short explanation shown as lore
     */
    public abstract String getDescription();

    /**
     * The plugin that brings the game. An event almost always has its own plugin - it is installed on the
     * event server automatically.
     *
     * @return the plugin of this event, or {@code null} if it only needs a plain server
     */
    public abstract FileType.PLUGIN getPlugin();

    /**
     * @return the name of the bukkit material the UI shows for this kind of event
     */
    public String getIconMaterial() {
        return "PAPER";
    }

    /**
     * @return the kind of server the event runs on
     */
    public ServerTemplate getServerTemplate() {
        return ServerTemplate.EVENT;
    }

    /**
     * @return how much memory the event server gets by default
     */
    public int getDefaultMemoryMB() {
        return getServerTemplate().getDefaultMemoryMB();
    }

    /**
     * @return plugins the event needs on top of its own plugin
     */
    public List<FileType.PLUGIN> getAdditionalPlugins() {
        return List.of();
    }

    /**
     * @return every plugin that is installed for this event
     */
    public final List<FileType.PLUGIN> getAllPlugins() {
        List<FileType.PLUGIN> plugins = new ArrayList<>();
        if (getPlugin() != null) plugins.add(getPlugin());
        plugins.addAll(getAdditionalPlugins());
        return plugins;
    }

    /**
     * @return the smallest number of teams the event works with
     */
    public int getMinTeams() {
        return 2;
    }

    /**
     * @return the largest number of teams the event works with
     */
    public int getMaxTeams() {
        return 8;
    }

    /**
     * @return how many teams a new event of this kind starts with
     */
    public int getDefaultTeamCount() {
        return Math.min(4, getMaxTeams());
    }

    /**
     * @return how many players fit into one team, {@code 0} for no limit
     */
    public int getMaxTeamSize() {
        return 0;
    }

    /**
     * @return whether players can join the teams themselves through the calendar
     */
    public boolean allowsPlayerSignup() {
        return true;
    }

    /**
     * @return how the teams of this event are compared by default
     */
    public RankingStrategy getDefaultRanking() {
        return RankingStrategies.HIGHEST_SCORE;
    }

    /**
     * Builds the teams a new event of this kind starts with. Override it to give the teams names that fit
     * the game.
     *
     * @param teamCount how many teams to build
     * @return the teams of the new event
     */
    public List<EventTeam> createTeams(int teamCount) {
        List<EventTeam> teams = new ArrayList<>();
        for (int i = 0; i < teamCount; i++) {
            EventTeamColor color = EventTeamColor.byIndex(i);
            teams.add(new EventTeam("Team " + color.getDisplayName(), color));
        }
        return teams;
    }

    /**
     * Called on the server of the event once it was started, after the teams are in place. The event plugin
     * hooks its game logic in here.
     *
     * @param event the event that started
     */
    public void onEventStart(ScheduledEvent event) {
    }

    /**
     * Called when the event is over, e.g. to store the ranking.
     *
     * @param event the event that ended
     */
    public void onEventEnd(ScheduledEvent event) {
    }

    /**
     * @param name the name of a new event
     * @return the freshly configured event, ready to be scheduled
     */
    public ScheduledEvent createEvent(String name) {
        ScheduledEvent event = new ScheduledEvent(this, name);
        event.setTeams(createTeams(getDefaultTeamCount()));
        return event;
    }

    @Override
    public String toString() {
        return getId();
    }
}

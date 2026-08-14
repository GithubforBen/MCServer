package de.hems.event;

import de.hems.communication.ListenerAdapter;
import de.hems.event.ranking.Ranking;
import de.hems.event.ranking.RankingStrategies;
import de.hems.event.ranking.RankingStrategy;
import de.hems.types.FileType;
import de.hems.types.ServerTemplate;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * One event in the calendar: what it is, on which days it happens, which teams take part and how they are
 * compared.
 * <p>
 * This is pure data - it is what travels between the servers. The behaviour lives in the
 * {@link EventDefinition} that {@link #getDefinition()} resolves locally, so a server that does not have
 * the plugin of an event can still show it.
 */
public class ScheduledEvent implements Serializable {
    private static final long serialVersionUID = 310L;

    private UUID id;
    private String definitionId;
    private String name;
    private String description;
    private List<LocalDate> days;
    private List<EventTeam> teams;
    private String rankingId;
    private ServerTemplate template;
    private List<FileType.PLUGIN> plugins;
    private int memoryMB;
    private boolean autoStartServer;
    private String serverName;
    private UUID createdBy;
    private String createdByName;
    private long createdAt;
    private List<LocalDate> startedDays;

    public ScheduledEvent() {
        this.id = UUID.randomUUID();
        this.days = new ArrayList<>();
        this.teams = new ArrayList<>();
        this.plugins = new ArrayList<>();
        this.startedDays = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * Creates an event of the given kind, taking over everything the definition brings with it.
     *
     * @param definition the kind of event
     * @param name       the name of this event
     */
    public ScheduledEvent(EventDefinition definition, String name) {
        this();
        this.definitionId = definition.getId();
        this.name = name;
        this.description = definition.getDescription();
        this.rankingId = definition.getDefaultRanking().getId();
        this.template = definition.getServerTemplate();
        this.plugins = new ArrayList<>(definition.getAllPlugins());
        this.memoryMB = definition.getDefaultMemoryMB();
        this.autoStartServer = true;
        this.serverName = ListenerAdapter.ServerName.normalize(name);
    }

    public UUID getId() {
        return id;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    /**
     * @return the kind of event, resolved on this server
     */
    public EventDefinition getDefinition() {
        return EventRegistry.get(definitionId);
    }

    /**
     * @return whether the plugin that runs this kind of event is installed here
     */
    public boolean isDefinitionKnown() {
        return EventRegistry.isKnown(definitionId);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.serverName = ListenerAdapter.ServerName.normalize(name);
    }

    public String getDescription() {
        return description == null ? getDefinition().getDescription() : description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /* ------------------------------------------------------------------------------ days */

    /**
     * @return the days this event takes place on, in order
     */
    public List<LocalDate> getDays() {
        if (days == null) days = new ArrayList<>();
        return days;
    }

    public void setDays(List<LocalDate> days) {
        this.days = new ArrayList<>(days == null ? List.of() : days);
        this.days.sort(LocalDate::compareTo);
    }

    /**
     * Adds or removes a day - this is what a click on a day in the calendar does.
     *
     * @param day the day to switch
     * @return whether the event now takes place on that day
     */
    public boolean toggleDay(LocalDate day) {
        if (getDays().remove(day)) return false;
        getDays().add(day);
        getDays().sort(LocalDate::compareTo);
        return true;
    }

    /**
     * @param day the day to check
     * @return whether the event takes place on that day
     */
    public boolean isOn(LocalDate day) {
        return getDays().contains(day);
    }

    /**
     * @return the first day that is not over yet, or {@code null} if the event is in the past
     */
    public LocalDate getNextDay() {
        LocalDate today = LocalDate.now();
        for (LocalDate day : getDays()) {
            if (!day.isBefore(today)) return day;
        }
        return null;
    }

    /**
     * @return whether the event takes place today
     */
    public boolean isToday() {
        return isOn(LocalDate.now());
    }

    /* ----------------------------------------------------------------------------- teams */

    public List<EventTeam> getTeams() {
        if (teams == null) teams = new ArrayList<>();
        return teams;
    }

    public void setTeams(List<EventTeam> teams) {
        this.teams = new ArrayList<>(teams == null ? List.of() : teams);
    }

    /**
     * Changes the number of teams, keeping the teams that already exist and their members.
     *
     * @param teamCount how many teams the event should have
     */
    public void setTeamCount(int teamCount) {
        EventDefinition definition = getDefinition();
        teamCount = Math.max(definition.getMinTeams(), Math.min(definition.getMaxTeams(), teamCount));
        List<EventTeam> current = getTeams();
        while (current.size() > teamCount) current.remove(current.size() - 1);
        List<EventTeam> blueprint = definition.createTeams(teamCount);
        while (current.size() < teamCount) current.add(blueprint.get(current.size()));
    }

    /**
     * @param id the id of a team
     * @return the team, or {@code null} if this event has no such team
     */
    public EventTeam getTeam(UUID id) {
        for (EventTeam team : getTeams()) {
            if (team.getId().equals(id)) return team;
        }
        return null;
    }

    /**
     * @param player a player
     * @return the team the player signed up for, or {@code null}
     */
    public EventTeam getTeamOf(UUID player) {
        for (EventTeam team : getTeams()) {
            if (team.hasMember(player)) return team;
        }
        return null;
    }

    /**
     * Puts a player into a team, taking them out of the team they were in before.
     *
     * @param player the player
     * @param teamId the team to join
     * @return whether the player is in that team now
     */
    public boolean join(UUID player, UUID teamId) {
        EventTeam target = getTeam(teamId);
        if (target == null) return false;
        int maxSize = getDefinition().getMaxTeamSize();
        if (maxSize > 0 && target.getSize() >= maxSize && !target.hasMember(player)) return false;
        for (EventTeam team : getTeams()) team.removeMember(player);
        target.addMember(player);
        return true;
    }

    /**
     * Takes a player out of every team of this event.
     *
     * @param player the player
     */
    public void leave(UUID player) {
        for (EventTeam team : getTeams()) team.removeMember(player);
    }

    /**
     * @return every player that signed up for this event
     */
    public Set<UUID> getParticipants() {
        Set<UUID> participants = new LinkedHashSet<>();
        for (EventTeam team : getTeams()) participants.addAll(team.getMembers());
        return participants;
    }

    /* --------------------------------------------------------------------------- ranking */

    public String getRankingId() {
        return rankingId;
    }

    public void setRanking(RankingStrategy strategy) {
        this.rankingId = strategy == null ? RankingStrategies.NONE.getId() : strategy.getId();
    }

    /**
     * @return how the teams of this event are compared
     */
    public RankingStrategy getRankingStrategy() {
        return RankingStrategies.get(rankingId);
    }

    /**
     * @return the current leaderboard of this event
     */
    public Ranking getRanking() {
        return Ranking.of(getRankingStrategy(), getTeams());
    }

    /**
     * @return whether this event has a leaderboard at all
     */
    public boolean isRanked() {
        return getRankingStrategy().isRanked();
    }

    /* ---------------------------------------------------------------------------- server */

    public ServerTemplate getTemplate() {
        return template == null ? getDefinition().getServerTemplate() : template;
    }

    public void setTemplate(ServerTemplate template) {
        this.template = template;
    }

    /**
     * @return the plugins the event server gets, normally including the plugin of the event
     */
    public List<FileType.PLUGIN> getPlugins() {
        if (plugins == null) plugins = new ArrayList<>();
        return plugins;
    }

    public void setPlugins(List<FileType.PLUGIN> plugins) {
        this.plugins = new ArrayList<>(plugins == null ? List.of() : plugins);
    }

    public int getMemoryMB() {
        return memoryMB <= 0 ? getDefinition().getDefaultMemoryMB() : memoryMB;
    }

    public void setMemoryMB(int memoryMB) {
        this.memoryMB = memoryMB;
    }

    /**
     * @return whether the host starts the server of this event automatically on the day of the event
     */
    public boolean isAutoStartServer() {
        return autoStartServer;
    }

    public void setAutoStartServer(boolean autoStartServer) {
        this.autoStartServer = autoStartServer;
    }

    /**
     * @return the name of the server this event runs on
     */
    public String getServerName() {
        if (serverName == null && name != null) serverName = ListenerAdapter.ServerName.normalize(name);
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = ListenerAdapter.ServerName.normalize(serverName);
    }

    /**
     * @return the days the server of this event was already started for
     */
    public List<LocalDate> getStartedDays() {
        if (startedDays == null) startedDays = new ArrayList<>();
        return startedDays;
    }

    public void markStarted(LocalDate day) {
        if (!getStartedDays().contains(day)) getStartedDays().add(day);
    }

    public boolean wasStartedOn(LocalDate day) {
        return getStartedDays().contains(day);
    }

    /* ----------------------------------------------------------------------------- meta */

    public UUID getCreatedBy() {
        return createdBy;
    }

    public String getCreatedByName() {
        return createdByName == null ? "unbekannt" : createdByName;
    }

    public void setCreatedBy(UUID createdBy, String createdByName) {
        this.createdBy = createdBy;
        this.createdByName = createdByName;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setDefinitionId(String definitionId) {
        this.definitionId = definitionId;
    }

    public void setRankingId(String rankingId) {
        this.rankingId = rankingId;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setStartedDays(List<LocalDate> startedDays) {
        this.startedDays = new ArrayList<>(startedDays == null ? List.of() : startedDays);
    }

    /**
     * Makes an independent copy, e.g. to edit an event without changing the calendar until it is saved.
     *
     * @return a copy of this event
     */
    public ScheduledEvent copy() {
        try {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(bytes)) {
                out.writeObject(this);
            }
            try (java.io.ObjectInputStream in = new java.io.ObjectInputStream(
                    new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
                return (ScheduledEvent) in.readObject();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not copy the event " + name, e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScheduledEvent)) return false;
        return id.equals(((ScheduledEvent) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return name + " [" + definitionId + "] " + getDays();
    }
}

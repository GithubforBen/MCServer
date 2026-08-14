package de.hems.events;

import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.calendar.CancelEventRequest;
import de.hems.communication.events.calendar.EventCalendarUpdatedEvent;
import de.hems.communication.events.calendar.EventScoreRequest;
import de.hems.communication.events.calendar.JoinEventTeamRequest;
import de.hems.communication.events.calendar.RequestEventCalendarEvent;
import de.hems.communication.events.calendar.RespondEventCalendarEvent;
import de.hems.communication.events.calendar.ScheduleEventRequest;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventHandler;
import de.hems.event.EventCalendar;
import de.hems.event.EventTeam;
import de.hems.event.EventTeamColor;
import de.hems.event.ScheduledEvent;
import de.hems.types.FileType;
import de.hems.types.ServerTemplate;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The host side of the event calendar.
 * <p>
 * The host owns the calendar: it stores the events in {@code events.yml}, answers requests from the servers
 * and announces every change to the whole network, which is what keeps the calendar identical everywhere.
 * It also starts the server of an event on the day it takes place.
 */
public class EventCalendarHandler {

    /** How often the host checks whether an event server has to be started. */
    private static final long CHECK_INTERVAL_MS = 60_000L;

    private final File file = new File("./events.yml");
    private final Map<UUID, ScheduledEvent> events = new ConcurrentHashMap<>();

    public EventCalendarHandler() {
        load();
        ListenerAdapter.register(RequestEventCalendarEvent.class, new EventHandler<RequestEventCalendarEvent>() {
            @Override
            public void onEvent(Event event) throws Exception {
                if (!(event instanceof RequestEventCalendarEvent request)) return;
                ListenerAdapter.sendListeners(new RespondEventCalendarEvent(
                        request.getSender(), snapshot(), request.getEventId()));
            }
        });
        ListenerAdapter.register(ScheduleEventRequest.class, new EventHandler<ScheduleEventRequest>() {
            @Override
            public void onEvent(Event event) {
                if (!(event instanceof ScheduleEventRequest request) || request.getEvent() == null) return;
                store(request.getEvent());
            }
        });
        ListenerAdapter.register(CancelEventRequest.class, new EventHandler<CancelEventRequest>() {
            @Override
            public void onEvent(Event event) {
                if (!(event instanceof CancelEventRequest request)) return;
                remove(request.getEventId());
            }
        });
        ListenerAdapter.register(JoinEventTeamRequest.class, new EventHandler<JoinEventTeamRequest>() {
            @Override
            public void onEvent(Event event) {
                if (!(event instanceof JoinEventTeamRequest request)) return;
                changeSignup(request.getEventId(), request.getTeamId(), request.getPlayer());
            }
        });
        ListenerAdapter.register(EventScoreRequest.class, new EventHandler<EventScoreRequest>() {
            @Override
            public void onEvent(Event event) {
                if (!(event instanceof EventScoreRequest request)) return;
                changeScore(request.getEventId(), request.getTeamId(), request.getScore(), request.isRelative());
            }
        });
        new Timer("event-calendar", true).scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    startDueEvents();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, CHECK_INTERVAL_MS, CHECK_INTERVAL_MS);
        EventCalendar.update(new ArrayList<>(events.values()));
        System.out.println("Loaded " + events.size() + " events from the calendar");
    }

    /* --------------------------------------------------------------------------- changes */

    /**
     * Stores an event, replacing an older version of it, and tells the network about it.
     *
     * @param event the event to store
     */
    public void store(ScheduledEvent event) {
        events.put(event.getId(), event);
        save();
        broadcast();
        System.out.println("Event '" + event.getName() + "' was planned for " + event.getDays());
    }

    /**
     * Removes an event and tells the network about it.
     *
     * @param eventId the id of the event
     */
    public void remove(UUID eventId) {
        ScheduledEvent removed = events.remove(eventId);
        if (removed == null) return;
        save();
        broadcast();
        System.out.println("Event '" + removed.getName() + "' was cancelled");
    }

    private void changeSignup(UUID eventId, UUID teamId, UUID player) {
        ScheduledEvent event = events.get(eventId);
        if (event == null || player == null) return;
        if (teamId == null) {
            event.leave(player);
        } else if (!event.join(player, teamId)) {
            return;
        }
        save();
        broadcast();
    }

    private void changeScore(UUID eventId, UUID teamId, double score, boolean relative) {
        ScheduledEvent event = events.get(eventId);
        if (event == null) return;
        EventTeam team = event.getTeam(teamId);
        if (team == null) return;
        if (relative) {
            team.addScore(score);
        } else {
            team.setScore(score);
        }
        save();
        broadcast();
    }

    /**
     * Starts the servers of the events that take place today and were not started yet.
     */
    private void startDueEvents() {
        LocalDate today = LocalDate.now();
        for (ScheduledEvent event : events.values()) {
            if (!event.isAutoStartServer() || !event.isOn(today) || event.wasStartedOn(today)) continue;
            ListenerAdapter.ServerName serverName = ListenerAdapter.ServerName.valueOf(event.getServerName());
            if (Main.getInstance().getServerHandler().doesInstanceExist(serverName)) {
                event.markStarted(today);
                continue;
            }
            try {
                List<FileType.PLUGIN> plugins = new ArrayList<>(event.getPlugins());
                for (FileType.PLUGIN plugin : event.getDefinition().getAllPlugins()) {
                    if (!plugins.contains(plugin)) plugins.add(plugin);
                }
                Main.getInstance().getServerHandler().startNewInstance(serverName, event.getTemplate(),
                        event.getMemoryMB(), plugins.toArray(new FileType.PLUGIN[0]));
                event.markStarted(today);
                save();
                broadcast();
                System.out.println("Started the server for the event '" + event.getName() + "'");
            } catch (Exception e) {
                System.out.println("Could not start the server for '" + event.getName() + "': " + e.getMessage());
            }
        }
    }

    /**
     * @return every event of the calendar
     */
    public ScheduledEvent[] snapshot() {
        return events.values().toArray(new ScheduledEvent[0]);
    }

    private void broadcast() {
        ScheduledEvent[] snapshot = snapshot();
        EventCalendar.update(List.of(snapshot));
        try {
            ListenerAdapter.sendListeners(new EventCalendarUpdatedEvent(ListenerAdapter.ServerName.ALL, snapshot));
        } catch (Exception e) {
            System.out.println("Could not announce the calendar: " + e.getMessage());
        }
    }

    /* ----------------------------------------------------------------------- persistence */

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("events");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            try {
                ScheduledEvent event = read(root.getConfigurationSection(key), UUID.fromString(key));
                events.put(event.getId(), event);
            } catch (Exception e) {
                System.out.println("Skipping broken event '" + key + "': " + e.getMessage());
            }
        }
    }

    private ScheduledEvent read(ConfigurationSection section, UUID id) {
        ScheduledEvent event = new ScheduledEvent();
        event.setId(id);
        event.setDefinitionId(section.getString("definition"));
        event.setName(section.getString("name", "Event"));
        event.setDescription(section.getString("description"));
        List<LocalDate> days = new ArrayList<>();
        for (String day : section.getStringList("days")) days.add(LocalDate.parse(day));
        event.setDays(days);
        List<LocalDate> startedDays = new ArrayList<>();
        for (String day : section.getStringList("started-days")) startedDays.add(LocalDate.parse(day));
        event.setStartedDays(startedDays);
        event.setRankingId(section.getString("ranking"));
        String template = section.getString("template");
        if (template != null) {
            ServerTemplate found = ServerTemplate.find(template);
            if (found != null) event.setTemplate(found);
        }
        List<FileType.PLUGIN> plugins = new ArrayList<>();
        for (String plugin : section.getStringList("plugins")) {
            try {
                plugins.add(FileType.PLUGIN.valueOf(plugin));
            } catch (IllegalArgumentException ignored) {
                // a plugin that does not exist anymore is simply dropped
            }
        }
        event.setPlugins(plugins);
        event.setMemoryMB(section.getInt("memory"));
        event.setAutoStartServer(section.getBoolean("autostart", true));
        if (section.getString("server") != null) event.setServerName(section.getString("server"));
        String createdBy = section.getString("created-by");
        event.setCreatedBy(createdBy == null ? null : UUID.fromString(createdBy), section.getString("created-by-name"));
        event.setCreatedAt(section.getLong("created-at", System.currentTimeMillis()));

        List<EventTeam> teams = new ArrayList<>();
        ConfigurationSection teamSection = section.getConfigurationSection("teams");
        if (teamSection != null) {
            for (String teamKey : teamSection.getKeys(false)) {
                ConfigurationSection team = teamSection.getConfigurationSection(teamKey);
                if (team == null) continue;
                Set<UUID> members = new LinkedHashSet<>();
                for (String member : team.getStringList("members")) members.add(UUID.fromString(member));
                EventTeamColor color;
                try {
                    color = EventTeamColor.valueOf(team.getString("color", "WHITE"));
                } catch (IllegalArgumentException e) {
                    color = EventTeamColor.WHITE;
                }
                teams.add(new EventTeam(UUID.fromString(teamKey), team.getString("name", "Team"), color,
                        members, team.getDouble("score")));
            }
        }
        event.setTeams(teams);
        return event;
    }

    private synchronized void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (ScheduledEvent event : events.values()) {
            String path = "events." + event.getId();
            config.set(path + ".definition", event.getDefinitionId());
            config.set(path + ".name", event.getName());
            config.set(path + ".description", event.getDescription());
            List<String> days = new ArrayList<>();
            for (LocalDate day : event.getDays()) days.add(day.toString());
            config.set(path + ".days", days);
            List<String> startedDays = new ArrayList<>();
            for (LocalDate day : event.getStartedDays()) startedDays.add(day.toString());
            config.set(path + ".started-days", startedDays);
            config.set(path + ".ranking", event.getRankingId());
            config.set(path + ".template", event.getTemplate().name());
            List<String> plugins = new ArrayList<>();
            for (FileType.PLUGIN plugin : event.getPlugins()) plugins.add(plugin.name());
            config.set(path + ".plugins", plugins);
            config.set(path + ".memory", event.getMemoryMB());
            config.set(path + ".autostart", event.isAutoStartServer());
            config.set(path + ".server", event.getServerName());
            config.set(path + ".created-by", event.getCreatedBy() == null ? null : event.getCreatedBy().toString());
            config.set(path + ".created-by-name", event.getCreatedByName());
            config.set(path + ".created-at", event.getCreatedAt());
            for (EventTeam team : event.getTeams()) {
                String teamPath = path + ".teams." + team.getId();
                config.set(teamPath + ".name", team.getName());
                config.set(teamPath + ".color", team.getColor().name());
                config.set(teamPath + ".score", team.getScore());
                List<String> members = new ArrayList<>();
                for (UUID member : team.getMembers()) members.add(member.toString());
                config.set(teamPath + ".members", members);
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            System.out.println("Could not save the event calendar: " + e.getMessage());
        }
    }
}

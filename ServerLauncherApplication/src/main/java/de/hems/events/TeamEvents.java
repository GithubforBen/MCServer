package de.hems.events;

import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.team.DeleteTeamEvent;
import de.hems.communication.events.team.RequestBackpackEvent;
import de.hems.communication.events.team.RequestTeamsEvent;
import de.hems.communication.events.team.RespondBackpackEvent;
import de.hems.communication.events.team.RespondBackpackSaveEvent;
import de.hems.communication.events.team.RespondTeamSaveEvent;
import de.hems.communication.events.team.RespondTeamsEvent;
import de.hems.communication.events.team.SaveBackpackEvent;
import de.hems.communication.events.team.SaveTeamEvent;
import de.hems.communication.events.team.TeamUpdatedEvent;
import de.hems.types.team.BackpackData;
import de.hems.types.team.TeamData;
import de.hems.utils.team.BackpackStore;
import de.hems.utils.team.TeamStore;

import java.util.ArrayList;

/**
 * Serves the teams and their backpacks to the rest of the network.
 * <p>
 * The launcher is the only node that writes them, which is what makes a change on one server visible on all
 * the others: after every write the new state is announced, so nobody has to poll.
 */
public class TeamEvents {

    private final TeamStore teams;
    private final BackpackStore backpacks;

    public TeamEvents(TeamStore teams, BackpackStore backpacks) {
        this.teams = teams;
        this.backpacks = backpacks;
        ListenerAdapter.register(RequestTeamsEvent.class, event -> onRequestTeams((RequestTeamsEvent) event));
        ListenerAdapter.register(SaveTeamEvent.class, event -> onSaveTeam((SaveTeamEvent) event));
        ListenerAdapter.register(DeleteTeamEvent.class, event -> onDeleteTeam((DeleteTeamEvent) event));
        ListenerAdapter.register(RequestBackpackEvent.class, event -> onRequestBackpack((RequestBackpackEvent) event));
        ListenerAdapter.register(SaveBackpackEvent.class, event -> onSaveBackpack((SaveBackpackEvent) event));
    }

    private void onRequestTeams(RequestTeamsEvent request) throws Exception {
        ListenerAdapter.sendListeners(new RespondTeamsEvent(
                request.getSender(), new ArrayList<>(teams.getTeams()), request.getEventId()));
    }

    private void onSaveTeam(SaveTeamEvent request) throws Exception {
        TeamStore.Result result = teams.put(request.getTeam(), request.isCreateIfMissing());
        ListenerAdapter.sendListeners(new RespondTeamSaveEvent(
                request.getSender(), result.successful(), result.message(), result.team(), request.getEventId()));
        if (result.successful()) announce(result.team().getName(), result.team());
    }

    private void onDeleteTeam(DeleteTeamEvent request) throws Exception {
        boolean existed = teams.delete(request.getTeamName());
        if (existed) {
            backpacks.delete(request.getTeamName());
            announce(request.getTeamName(), null);
        }
    }

    private void onRequestBackpack(RequestBackpackEvent request) throws Exception {
        BackpackData backpack = null;
        // only a team that actually exists gets a backpack, so a stale client cannot create one
        if (teams.getTeam(request.getTeamName()) != null) {
            backpack = backpacks.get(request.getTeamName(), request.getWantedSize());
        }
        ListenerAdapter.sendListeners(new RespondBackpackEvent(
                request.getSender(), backpack, request.getEventId()));
    }

    private void onSaveBackpack(SaveBackpackEvent request) throws Exception {
        BackpackStore.Result result = backpacks.put(request.getBackpack());
        ListenerAdapter.sendListeners(new RespondBackpackSaveEvent(
                request.getSender(), result.successful(), result.revision(), result.message(),
                request.getEventId()));
    }

    /**
     * Tells the network that a team changed.
     *
     * @param name the team
     * @param team its new state, or {@code null} when it was deleted
     */
    private void announce(String name, TeamData team) {
        try {
            ListenerAdapter.sendListeners(new TeamUpdatedEvent(name, team));
        } catch (Exception e) {
            System.out.println("Could not announce the change to team " + name + ": " + e.getMessage());
        }
    }

    public TeamStore getTeams() {
        return teams;
    }

    public BackpackStore getBackpacks() {
        return backpacks;
    }
}

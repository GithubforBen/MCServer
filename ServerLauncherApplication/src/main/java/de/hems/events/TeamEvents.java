package de.hems.events;

import de.hems.Main;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.money.BalanceUpdatedEvent;
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
import de.hems.utils.money.MoneyStore;
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
    private final MoneyStore money;

    public TeamEvents(TeamStore teams, BackpackStore backpacks, MoneyStore money) {
        this.teams = teams;
        this.backpacks = backpacks;
        this.money = money;
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
        String renameFrom = request.getRenameFrom();
        TeamStore.Result result = teams.put(request.getTeam(), request.isCreateIfMissing(), renameFrom);
        ListenerAdapter.sendListeners(new RespondTeamSaveEvent(
                request.getSender(), result.successful(), result.message(), result.team(), request.getEventId()));
        if (!result.successful()) return;
        // the backpack lives under the team name, so it has to follow the rename before anyone reopens it
        boolean renamed = renameFrom != null && !renameFrom.equalsIgnoreCase(result.team().getName());
        if (renamed) {
            backpacks.rename(renameFrom, result.team().getName());
            // and the team's money, which is kept under its name just like the backpack
            moveMoney(renameFrom, result.team().getName(), "Umbenennung");
            announce(renameFrom, null, null);
        }
        // the servers are told what the team was called, so whatever they keep under the old name follows
        announce(result.team().getName(), result.team(), renamed ? renameFrom : null);
    }

    private void onDeleteTeam(DeleteTeamEvent request) throws Exception {
        TeamData team = teams.getTeam(request.getTeamName());
        boolean existed = teams.delete(request.getTeamName());
        if (existed) {
            backpacks.delete(request.getTeamName());
            // the money of a disbanded team goes to its leader rather than staying behind under the name
            if (team != null && team.getLeader() != null) {
                moveMoney(team.getName(), team.getLeader().toString(), "Auflösung");
            }
            announce(request.getTeamName(), null, null);
        }
    }

    /**
     * Moves a team's account and tells every server what both accounts hold now.
     *
     * @param from   the account to empty
     * @param to     the account that gets it
     * @param reason what it is for, for the log
     */
    private void moveMoney(String from, String to, String reason) {
        MoneyStore.Transfer transfer = money.moveAll(from, to);
        if (transfer.amount() > 0) {
            System.out.println("Balance " + from + " -> " + to + ": " + transfer.amount() + " (" + reason + ")");
        }
        try {
            ListenerAdapter.sendListeners(new BalanceUpdatedEvent(from, transfer.fromLeft()));
            ListenerAdapter.sendListeners(new BalanceUpdatedEvent(to, transfer.toNow()));
        } catch (Exception e) {
            System.out.println("Could not announce the move of " + from + "'s money: " + e.getMessage());
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
     * @param previousName what it was called before, when it was renamed
     */
    private void announce(String name, TeamData team, String previousName) {
        try {
            ListenerAdapter.sendListeners(new TeamUpdatedEvent(name, team, previousName));
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

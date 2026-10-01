package de.hems.communication.events.configs;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The list of paying players, sent by the launcher to every server the moment it changes - from discord or
 * the website. Without it a server only learnt of a new supporter at its next refresh, up to a minute later,
 * and a player who had just been added found the perks not there yet.
 */
public class PayingPlayersChangedEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4907L;

    /** The uuids, as the launcher stores them. */
    private ArrayList<String> players;

    public PayingPlayersChangedEvent() {
    }

    public PayingPlayersChangedEvent(List<String> players) {
        super(ListenerAdapter.ServerName.ALL);
        this.players = new ArrayList<>(players);
    }

    public List<String> getPlayers() {
        return players == null ? List.of() : players;
    }
}

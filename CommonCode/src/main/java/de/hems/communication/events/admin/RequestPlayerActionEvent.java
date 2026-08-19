package de.hems.communication.events.admin;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.UUID;

/**
 * Does something to a player. Broadcast; only the server that has them acts.
 */
public class RequestPlayerActionEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 3106L;

    /** What the website can do to a player. */
    public enum Action {
        KICK,
        HEAL,
        FEED,
        CLEAR_INVENTORY,
        SET_GAMEMODE,
        SET_OP,
        SEND_MESSAGE,
        TELEPORT_TO_SPAWN
    }

    private UUID playerId;
    private Action action;
    /** The argument the action needs, for example the game mode or the kick reason. */
    private String argument;
    private String editor;

    public RequestPlayerActionEvent(UUID playerId, Action action, String argument, String editor) {
        super(ListenerAdapter.ServerName.ALL);
        this.playerId = playerId;
        this.action = action;
        this.argument = argument;
        this.editor = editor;
    }

    public RequestPlayerActionEvent() {
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Action getAction() {
        return action;
    }

    public String getArgument() {
        return argument;
    }

    public String getEditor() {
        return editor;
    }
}

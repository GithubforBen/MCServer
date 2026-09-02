package de.hems.communication.events.discord;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.UUID;

/**
 * Hands in the code a player was given on discord.
 * <p>
 * This is the half that proves the two accounts belong together. Anybody can type somebody else's
 * minecraft name into a discord command; only the person actually logged in as that account can type the
 * code back on the server.
 */
public class ConfirmAccountLinkEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4904L;

    private UUID playerId;
    private String playerName;
    private String code;

    public ConfirmAccountLinkEvent() {
    }

    public ConfirmAccountLinkEvent(UUID playerId, String playerName, String code) {
        super(ListenerAdapter.ServerName.HOST);
        this.playerId = playerId;
        this.playerName = playerName;
        this.code = code;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getCode() {
        return code;
    }
}

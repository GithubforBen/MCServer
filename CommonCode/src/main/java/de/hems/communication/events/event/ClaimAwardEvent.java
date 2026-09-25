package de.hems.communication.events.event;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;

import java.io.Serializable;
import java.util.UUID;

/**
 * Reserves a prize for handing over, or gives a reservation back.
 * <p>
 * The launcher answers with {@link RespondClaimAwardEvent}, and a game server hands a prize over only after
 * the launcher said yes. It used to be the other way round - hand over, then tell the launcher - and a
 * player who switched servers before that message arrived, or whose message was lost, got the prize a
 * second time on the next join, money included. Now only one server can ever get the yes.
 * <p>
 * A reservation that could not be handed over after all (the player left in that moment, the inventory
 * filled up) is given back with {@code release}, so the prize waits for the next join.
 */
public class ClaimAwardEvent extends EventFoundationData implements Event, Serializable {

    private static final long serialVersionUID = 4325L;

    private UUID awardId;
    private boolean release;

    public ClaimAwardEvent(UUID awardId, boolean release) {
        super(ListenerAdapter.ServerName.HOST);
        this.awardId = awardId;
        this.release = release;
    }

    public ClaimAwardEvent() {
    }

    public UUID getAwardId() {
        return awardId;
    }

    /**
     * @return whether this gives a reservation back rather than making one
     */
    public boolean isRelease() {
        return release;
    }
}

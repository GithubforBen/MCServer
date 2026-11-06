package com.benschnorr.communication.events.round;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.RespondDataEvent;
import com.benschnorr.types.round.RoundData;

import java.io.Serializable;
import java.util.UUID;

/** What the launcher made of a {@link SaveRoundEvent}. */
public class RespondRoundSaveEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4714L;

    private boolean successful;
    private String message;

    public RespondRoundSaveEvent(ListenerAdapter.ServerName receiver, boolean successful, String message,
                                 RoundData round, UUID requestId) {
        super(receiver, round, requestId);
        this.successful = successful;
        this.message = message;
    }

    public RespondRoundSaveEvent() {
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getMessage() {
        return message;
    }
}

package com.benschnorr.communication.events.lotto;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.RespondDataEvent;

import java.io.Serializable;
import java.util.UUID;

/** The answer to a {@link LottoRequestEvent}: what was asked for, or why it was refused. */
public class RespondLottoEvent extends RespondDataEvent implements Event, Serializable {

    private static final long serialVersionUID = 4604L;

    private String error;

    public RespondLottoEvent(ListenerAdapter.ServerName receiver, Object data, String error, UUID requestId) {
        super(receiver, data, requestId);
        this.error = error;
    }

    public RespondLottoEvent() {
    }

    public String getError() {
        return error;
    }
}

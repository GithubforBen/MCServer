package com.benschnorr.communication.events.money;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.RespondDataEvent;

import java.io.Serializable;
import java.util.UUID;

public class RespondPlayerMoneyEvent extends RespondDataEvent implements Event, Serializable {
    private static final long serialVersionUID = 4L;

    public RespondPlayerMoneyEvent(ListenerAdapter.ServerName receiver, UUID requestID, int money) {
        super(receiver, Integer.valueOf(money), requestID);
    }

    public RespondPlayerMoneyEvent() {
    }
}

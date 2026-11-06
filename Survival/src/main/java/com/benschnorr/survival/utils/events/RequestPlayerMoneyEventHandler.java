package com.benschnorr.survival.utils.events;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.money.RespondPlayerMoneyEvent;
import com.benschnorr.communication.events.types.Event;
import com.benschnorr.communication.events.types.EventHandler;
import com.benschnorr.communication.events.money.RequestPlayerMoneyEvent;
import com.benschnorr.survival.Survival;
import com.benschnorr.survival.featrues.money.MoneyHandler;

public class RequestPlayerMoneyEventHandler implements EventHandler<RequestPlayerMoneyEvent> {
    public RequestPlayerMoneyEventHandler() {
        ListenerAdapter.register(RequestPlayerMoneyEvent.class, this);
    }

    @Override
    public void onEvent(Event event) throws Exception {
        RequestPlayerMoneyEvent e = (RequestPlayerMoneyEvent) event;
        RespondPlayerMoneyEvent respondDataFromConfigEvent = new RespondPlayerMoneyEvent(
                e.getSender(), e.getEventId(),
                MoneyHandler.getMoney(e.getPlayerId()));
        ListenerAdapter.sendListeners(respondDataFromConfigEvent);
    }
}

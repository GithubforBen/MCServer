package de.schnorrenbergers.survival.utils.events;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.money.RespondPlayerMoneyEvent;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventHandler;
import de.hems.communication.events.money.RequestPlayerMoneyEvent;
import de.schnorrenbergers.survival.Survival;
import de.schnorrenbergers.survival.utils.MoneyHandler;

public class RequestPlayerMoneyEventHandler implements EventHandler<RequestPlayerMoneyEvent> {
    public RequestPlayerMoneyEventHandler() {
        ListenerAdapter.register(RequestPlayerMoneyEvent.class, this);
    }

    @Override
    public void onEvent(Event event) {
        RequestPlayerMoneyEvent e = (RequestPlayerMoneyEvent) event;
        RespondPlayerMoneyEvent respondDataFromConfigEvent = new RespondPlayerMoneyEvent(Survival.getInstance().getListenerAdapter().getName(),
                e.getSender(), e.getPlayerId(),
                MoneyHandler.getMoney(e.getPlayerId()));
        ListenerAdapter.excecuteListeners(respondDataFromConfigEvent);
    }
}

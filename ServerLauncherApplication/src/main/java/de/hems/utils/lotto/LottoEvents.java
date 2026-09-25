package de.hems.utils.lotto;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.lotto.LottoRequestEvent;
import de.hems.communication.events.lotto.RespondLottoEvent;

import de.hems.types.lotto.LottoDraw;
import de.hems.types.lotto.LottoTicket;

import java.util.ArrayList;
import java.util.List;

/**
 * Answers what players and admins do with the lotto in the game. Whether somebody is an admin is what
 * their game server says, as with every other request from a game server.
 */
public class LottoEvents {

    private final LottoService lotto;

    public LottoEvents(LottoService lotto) {
        this.lotto = lotto;
        ListenerAdapter.register(LottoRequestEvent.class, event -> onRequest((LottoRequestEvent) event));
    }

    private void onRequest(LottoRequestEvent request) throws Exception {
        Object data = null;
        String error = null;
        switch (request.getAction()) {
            case STATUS -> data = lotto.status();
            case MINE -> data = new ArrayList<>(lotto.ticketsOf(request.getPlayer()));
            case BUY -> {
                LottoService.Result<List<LottoTicket>> result =
                        lotto.buy(request.getPlayer(), request.getPlayerName(), request.getTips());
                data = result.value() == null ? null : new ArrayList<>(result.value());
                error = result.error();
            }
            case DRAW_NOW, SET_SCHEDULE, SET_PRICE -> {
                if (!request.isStaff()) {
                    error = "Das dürfen nur Admins.";
                } else if (request.getAction() == LottoRequestEvent.Action.DRAW_NOW) {
                    LottoService.Result<LottoDraw> result = lotto.draw(null);
                    data = result.value();
                    error = result.error();
                } else {
                    error = request.getAction() == LottoRequestEvent.Action.SET_PRICE
                            ? lotto.setPrice(request.getAmount())
                            : lotto.setSchedule(request.getText());
                    data = lotto.status();
                }
            }
        }
        ListenerAdapter.sendListeners(new RespondLottoEvent(request.getSender(), data, error, request.getEventId()));
    }
}

package de.hems.events;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.money.BalanceUpdatedEvent;
import de.hems.communication.events.money.ChangeBalanceEvent;
import de.hems.communication.events.money.RequestBalancesEvent;
import de.hems.communication.events.money.RequestPlayerMoneyEvent;
import de.hems.communication.events.money.RespondBalanceChangeEvent;
import de.hems.communication.events.money.RespondBalancesEvent;
import de.hems.communication.events.money.RespondPlayerMoneyEvent;
import de.hems.types.money.BalanceResult;
import de.hems.utils.money.MoneyStore;

/**
 * Serves the money to the rest of the network.
 * <p>
 * The launcher is the only node that writes a balance, which is what makes a purchase in the lobby and a
 * payout on survival add up instead of overwriting each other. Every change is announced, so no server has
 * to poll to keep its copy current.
 */
public class MoneyEvents {

    private final MoneyStore money;

    public MoneyEvents(MoneyStore money) {
        this.money = money;
        ListenerAdapter.register(RequestBalancesEvent.class, event -> onRequest((RequestBalancesEvent) event));
        ListenerAdapter.register(ChangeBalanceEvent.class, event -> onChange((ChangeBalanceEvent) event));
        ListenerAdapter.register(RequestPlayerMoneyEvent.class,
                event -> onRequestPlayer((RequestPlayerMoneyEvent) event));
    }

    private void onRequest(RequestBalancesEvent request) throws Exception {
        ListenerAdapter.sendListeners(new RespondBalancesEvent(
                request.getSender(), money.all(), request.getEventId()));
    }

    /**
     * The single account lookup the older code asks for. Answered from the same store as everything else,
     * so it does not matter whether survival is running.
     */
    private void onRequestPlayer(RequestPlayerMoneyEvent request) throws Exception {
        if (request.getPlayerId() == null) return;
        ListenerAdapter.sendListeners(new RespondPlayerMoneyEvent(
                request.getSender(), request.getEventId(), money.get(request.getPlayerId().toString())));
    }

    private void onChange(ChangeBalanceEvent request) throws Exception {
        BalanceResult result = refuseExpired(money, request, System.currentTimeMillis());
        if (result != null) {
            System.out.println("Balance " + request.getHolder() + " " + (request.getDelta() >= 0 ? "+" : "")
                    + request.getDelta() + " dropped: deadline passed, the sender no longer waits"
                    + (request.getReason() == null ? "" : " (" + request.getReason() + ")"));
            ListenerAdapter.sendListeners(new RespondBalanceChangeEvent(
                    request.getSender(), result, request.getEventId()));
            return;
        }
        result = money.change(request.getHolder(), request.getDelta(), request.isRequireCover());
        ListenerAdapter.sendListeners(new RespondBalanceChangeEvent(
                request.getSender(), result, request.getEventId()));
        if (!result.isSuccessful()) return;
        System.out.println("Balance " + request.getHolder() + " " + (request.getDelta() >= 0 ? "+" : "")
                + request.getDelta() + " -> " + result.getBalance()
                + (request.getReason() == null ? "" : " (" + request.getReason() + ")"));
        ListenerAdapter.sendListeners(new BalanceUpdatedEvent(result.getHolder(), result.getBalance()));
    }

    /**
     * @param money   the store, only read
     * @param request the request
     * @param now     epoch millis
     * @return a failed result with the current balance when the request is too late, else {@code null}
     */
    static BalanceResult refuseExpired(MoneyStore money, ChangeBalanceEvent request, long now) {
        if (!request.isExpired(now)) return null;
        return BalanceResult.failed(request.getHolder(), money.get(request.getHolder()),
                "Der Host war zu langsam, nichts wurde abgebucht.");
    }

    public MoneyStore getMoney() {
        return money;
    }
}

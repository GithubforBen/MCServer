package de.hems.events;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.cosmetic.BuyCosmeticEvent;
import de.hems.communication.events.cosmetic.CosmeticUpdatedEvent;
import de.hems.communication.events.cosmetic.PlayerCosmeticsUpdatedEvent;
import de.hems.communication.events.cosmetic.RequestCosmeticsEvent;
import de.hems.communication.events.cosmetic.RespondCosmeticBuyEvent;
import de.hems.communication.events.cosmetic.RespondCosmeticsEvent;
import de.hems.communication.events.cosmetic.SaveCosmeticEvent;
import de.hems.communication.events.cosmetic.SelectCosmeticEvent;
import de.hems.communication.events.money.BalanceUpdatedEvent;
import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.CosmeticPurchase;
import de.hems.types.cosmetic.PlayerCosmetics;
import de.hems.types.money.BalanceResult;
import de.hems.utils.cosmetic.CosmeticStore;
import de.hems.utils.money.MoneyStore;

import java.util.UUID;

/**
 * Serves the cosmetics, and sells them.
 * <p>
 * Selling belongs here and nowhere else. The price, the bits and the ownership are all things the launcher
 * owns, so a purchase is one method under one lock: read the price, take the money, hand the cosmetic over.
 * Split across two servers it would be three round trips with two places to fail halfway, and halfway means
 * somebody paid for nothing.
 */
public class CosmeticEvents {

    private final CosmeticStore cosmetics;
    private final MoneyStore money;

    public CosmeticEvents(CosmeticStore cosmetics, MoneyStore money) {
        this.cosmetics = cosmetics;
        this.money = money;
        ListenerAdapter.register(RequestCosmeticsEvent.class, event -> onRequest((RequestCosmeticsEvent) event));
        ListenerAdapter.register(SaveCosmeticEvent.class, event -> onSave((SaveCosmeticEvent) event));
        ListenerAdapter.register(BuyCosmeticEvent.class, event -> onBuy((BuyCosmeticEvent) event));
        ListenerAdapter.register(SelectCosmeticEvent.class, event -> onSelect((SelectCosmeticEvent) event));
    }

    private void onRequest(RequestCosmeticsEvent request) throws Exception {
        ListenerAdapter.sendListeners(new RespondCosmeticsEvent(
                request.getSender(), cosmetics.snapshot(), request.getEventId()));
    }

    private void onSave(SaveCosmeticEvent request) throws Exception {
        CosmeticData stored = cosmetics.put(request.getCosmetic());
        if (stored == null) return;
        ListenerAdapter.sendListeners(new CosmeticUpdatedEvent(stored.copy()));
        System.out.println("Cosmetic " + stored.getId() + ": " + (stored.isEnabled() ? "on" : "off")
                + ", " + (stored.isBuyable() ? "for sale at " + stored.getPriceBits() + " bits" : "not for sale"));
    }

    /**
     * Takes the money and hands the cosmetic over, in that order and under one lock.
     */
    private synchronized void onBuy(BuyCosmeticEvent request) throws Exception {
        UUID player = request.getPlayerId();
        String id = request.getCosmeticId();
        String account = player == null ? null : player.toString();
        int balance = money.get(account);

        CosmeticData cosmetic = cosmetics.get(id);
        if (player == null || cosmetic == null) {
            respond(request, CosmeticPurchase.failed(id, balance, "Dieses Cosmetic gibt es nicht."));
            return;
        }
        if (!cosmetic.isEnabled()) {
            respond(request, CosmeticPurchase.failed(id, balance, "Dieses Cosmetic ist gerade abgeschaltet."));
            return;
        }
        if (!cosmetic.isBuyable()) {
            respond(request, CosmeticPurchase.failed(id, balance, "Dieses Cosmetic steht nicht zum Verkauf."));
            return;
        }
        if (cosmetics.owns(player, id)) {
            respond(request, CosmeticPurchase.failed(id, balance, "Du hast das schon."));
            return;
        }
        int price = cosmetic.getPriceBits();
        if (price > 0) {
            BalanceResult paid = money.change(account, -price, true);
            if (!paid.isSuccessful()) {
                respond(request, CosmeticPurchase.failed(id, paid.getBalance(), paid.getMessage()));
                return;
            }
            balance = paid.getBalance();
            ListenerAdapter.sendListeners(new BalanceUpdatedEvent(account, balance));
        }
        PlayerCosmetics owned = cosmetics.grant(player, id);
        respond(request, CosmeticPurchase.ok(id, price, balance));
        ListenerAdapter.sendListeners(new PlayerCosmeticsUpdatedEvent(owned.copy()));
        System.out.println("Cosmetic " + id + " sold to " + player + " for " + price + " bits.");
    }

    private void respond(BuyCosmeticEvent request, CosmeticPurchase purchase) throws Exception {
        ListenerAdapter.sendListeners(new RespondCosmeticBuyEvent(
                request.getSender(), purchase, request.getEventId()));
    }

    private void onSelect(SelectCosmeticEvent request) throws Exception {
        PlayerCosmetics updated = cosmetics.select(
                request.getPlayerId(), request.getType(), request.getCosmeticId());
        if (updated == null) return;
        ListenerAdapter.sendListeners(new PlayerCosmeticsUpdatedEvent(updated.copy()));
    }

    public CosmeticStore getCosmetics() {
        return cosmetics;
    }
}

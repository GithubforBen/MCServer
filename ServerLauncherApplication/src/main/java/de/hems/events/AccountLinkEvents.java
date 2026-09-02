package de.hems.events;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.discord.AccountLinkUpdatedEvent;
import de.hems.communication.events.discord.ConfirmAccountLinkEvent;
import de.hems.communication.events.discord.RequestAccountLinksEvent;
import de.hems.communication.events.discord.RespondAccountLinkEvent;
import de.hems.communication.events.discord.RespondAccountLinksEvent;
import de.hems.utils.bot.verification.AccountLinkStore;

/**
 * Serves the discord links to the network and takes in the codes.
 * <p>
 * The codes are checked here and nowhere else. A server that decided for itself whether a code was right
 * would be a server that could be told to say yes.
 */
public class AccountLinkEvents {

    private final AccountLinkStore links;

    public AccountLinkEvents(AccountLinkStore links) {
        this.links = links;
        ListenerAdapter.register(RequestAccountLinksEvent.class,
                event -> onRequest((RequestAccountLinksEvent) event));
        ListenerAdapter.register(ConfirmAccountLinkEvent.class,
                event -> onConfirm((ConfirmAccountLinkEvent) event));
    }

    private void onRequest(RequestAccountLinksEvent request) throws Exception {
        ListenerAdapter.sendListeners(new RespondAccountLinksEvent(
                request.getSender(), links.all(), request.getEventId()));
    }

    private void onConfirm(ConfirmAccountLinkEvent request) throws Exception {
        AccountLinkStore.Result result = links.confirm(
                request.getPlayerId(), request.getPlayerName(), request.getCode());
        ListenerAdapter.sendListeners(new RespondAccountLinkEvent(
                request.getSender(), result.successful(), result.message(), result.link(),
                request.getEventId()));
        if (!result.successful()) return;
        System.out.println("Linked " + result.link().getMinecraftName() + " to "
                + result.link().describeDiscord());
        ListenerAdapter.sendListeners(new AccountLinkUpdatedEvent(
                result.link().getMinecraftId(), result.link()));
    }

    public AccountLinkStore getLinks() {
        return links;
    }
}

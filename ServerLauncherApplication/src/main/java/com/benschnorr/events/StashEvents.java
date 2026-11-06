package com.benschnorr.events;

import com.benschnorr.communication.ListenerAdapter;
import com.benschnorr.communication.events.admin.RequestStashEvent;
import com.benschnorr.communication.events.admin.RespondStashEvent;
import com.benschnorr.communication.events.admin.RespondStashSaveEvent;
import com.benschnorr.communication.events.admin.SaveStashEvent;
import com.benschnorr.utils.admin.StashStore;

/**
 * Serves the admin stash to the game servers.
 * <p>
 * The website reaches the same store directly, so a change made in the browser is in the chest a player
 * opens a moment later and the other way round.
 */
public class StashEvents {

    private final StashStore stashes;

    public StashEvents(StashStore stashes) {
        this.stashes = stashes;
        ListenerAdapter.register(RequestStashEvent.class, event -> onRequest((RequestStashEvent) event));
        ListenerAdapter.register(SaveStashEvent.class, event -> onSave((SaveStashEvent) event));
    }

    private void onRequest(RequestStashEvent request) throws Exception {
        ListenerAdapter.sendListeners(new RespondStashEvent(
                request.getSender(), stashes.get(request.getStashId()), request.getEventId()));
    }

    private void onSave(SaveStashEvent request) throws Exception {
        StashStore.Result result = stashes.put(request.getStash());
        if (result.successful()) {
            System.out.println("[Stash] " + request.getEditor() + " changed the admin stash.");
        }
        ListenerAdapter.sendListeners(new RespondStashSaveEvent(
                request.getSender(), result.successful(), result.revision(), result.message(),
                request.getEventId()));
    }
}

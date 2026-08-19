package de.hems.paper.admin;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.admin.RequestStashEvent;
import de.hems.communication.events.admin.RespondStashSaveEvent;
import de.hems.communication.events.admin.SaveStashEvent;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.paper.PaperContext;
import de.hems.types.admin.ItemData;
import de.hems.types.admin.StashData;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

/**
 * Reads and writes the admin stash from a game server.
 * <p>
 * The stash itself lives on the launcher, which is what lets the website drop something into it and a
 * player pick it up in game a moment later.
 */
public final class StashService {

    /** How long to wait for the launcher. */
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private StashService() {
    }

    /**
     * Fetches the stash in the background and hands it over on the main thread.
     *
     * @param stashId  which stash
     * @param callback what to do with it; gets {@code null} when it could not be read
     */
    public static void loadAsync(String stashId, Consumer<StashData> callback) {
        PaperContext.async(() -> {
            StashData stash = loadBlocking(stashId);
            PaperContext.sync(() -> callback.accept(stash));
        });
    }

    /**
     * Fetches the stash. Blocks, so it must not run on the main thread.
     *
     * @param stashId which stash
     * @return it, or {@code null} if the launcher did not answer
     */
    public static StashData loadBlocking(String stashId) {
        try {
            RequestStashEvent request = new RequestStashEvent(stashId);
            ListenerAdapter.sendListeners(request);
            RespondDataEvent response = ListenerAdapter.waitForEvent(request.getEventId(), TIMEOUT);
            if (response == null) return null;
            return response.getData() instanceof StashData stash ? stash : null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Writes the stash back in the background.
     *
     * @param stash    the stash, carrying the revision it was read at
     * @param editor   who changed it
     * @param callback what to do with the result, on the main thread
     */
    public static void saveAsync(StashData stash, String editor, Consumer<Result> callback) {
        PaperContext.async(() -> {
            Result result = saveBlocking(stash, editor);
            if (callback == null) return;
            PaperContext.sync(() -> callback.accept(result));
        });
    }

    /**
     * Writes the stash back. Blocks, so it must not run on the main thread.
     *
     * @param stash  the stash to store
     * @param editor who changed it
     * @return what the launcher made of it
     */
    public static Result saveBlocking(StashData stash, String editor) {
        try {
            SaveStashEvent request = new SaveStashEvent(stash, editor);
            ListenerAdapter.sendListeners(request);
            RespondDataEvent response = ListenerAdapter.waitForEvent(request.getEventId(), TIMEOUT);
            if (!(response instanceof RespondStashSaveEvent saved)) {
                return new Result(false, "Der Hauptserver hat nicht geantwortet - nichts gespeichert.");
            }
            return new Result(saved.isSuccessful(), String.valueOf(saved.getData()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result(false, "Unterbrochen.");
        } catch (Exception e) {
            return new Result(false, "Konnte nicht gespeichert werden: " + e.getMessage());
        }
    }

    /**
     * @param stash the stash as it came from the launcher
     * @param size  how many slots the window has
     * @return its items, ready to be put into an inventory
     */
    public static ItemStack[] toItems(StashData stash, int size) {
        if (stash == null) return new ItemStack[size];
        return ItemCodec.toContents(stash.getItems(), size);
    }

    /**
     * @param contents the slots of the stash inventory
     * @return the same slots in the shape the launcher stores and the website reads
     */
    public static List<ItemData> toItemData(ItemStack[] contents) {
        return ItemCodec.toData(contents);
    }

    /**
     * How a write ended.
     *
     * @param successful whether it was stored
     * @param message    what to tell the admin
     */
    public record Result(boolean successful, String message) {
    }
}

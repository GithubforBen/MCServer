package de.hems.paper.team;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.team.RequestBackpackEvent;
import de.hems.communication.events.team.RespondBackpackSaveEvent;
import de.hems.communication.events.team.SaveBackpackEvent;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.paper.PaperContext;
import de.hems.types.team.BackpackData;
import de.hems.types.team.TeamData;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Reads and writes the shared backpack of a team.
 * <p>
 * The contents live on the launcher, so a team's backpack is the same one on every server. The items travel
 * as the bytes bukkit serialises them into - the launcher stores them without ever looking inside, since it
 * has no bukkit to make sense of them with.
 */
public final class BackpackService {

    /** How long to wait for the launcher. */
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private BackpackService() {
    }

    /**
     * Works out how big a team's backpack should be right now.
     * <p>
     * A team whose paying members are in the majority gets a double chest, everybody else a single one. It
     * is recalculated on every open, so a team that gains or loses a paying member sees the change the next
     * time somebody opens the backpack.
     *
     * @param team the team
     * @return the number of slots
     */
    public static int sizeFor(TeamData team) {
        return TeamService.isMajorityPaying(team) ? BackpackData.DOUBLE_CHEST : BackpackData.SINGLE_CHEST;
    }

    /**
     * Fetches a backpack in the background and hands it over on the main thread.
     *
     * @param team     the team whose backpack to open
     * @param callback what to do with it, on the main thread; gets {@code null} when it could not be read
     */
    public static void loadAsync(TeamData team, Consumer<BackpackData> callback) {
        int size = sizeFor(team);
        PaperContext.async(() -> {
            BackpackData backpack = loadBlocking(team.getName(), size);
            PaperContext.sync(() -> callback.accept(backpack));
        });
    }

    /**
     * Fetches a backpack. Blocks, so it must not run on the main thread.
     *
     * @param teamName   the team
     * @param wantedSize how big it should be
     * @return the backpack, or {@code null} if the launcher did not answer or the team is unknown there
     */
    public static BackpackData loadBlocking(String teamName, int wantedSize) {
        try {
            RequestBackpackEvent request = new RequestBackpackEvent(teamName, wantedSize);
            ListenerAdapter.sendListeners(request);
            RespondDataEvent response = ListenerAdapter.waitForEvent(request.getEventId(), TIMEOUT);
            if (response == null) return null;
            return response.getData() instanceof BackpackData backpack ? backpack : null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Writes a backpack back in the background.
     *
     * @param backpack the backpack, carrying the revision it was opened at
     * @param callback what to do with the result, on the main thread
     */
    public static void saveAsync(BackpackData backpack, Consumer<Result> callback) {
        PaperContext.async(() -> {
            Result result = saveBlocking(backpack);
            if (callback == null) return;
            PaperContext.sync(() -> callback.accept(result));
        });
    }

    /**
     * Writes a backpack back. Blocks, so it must not run on the main thread.
     *
     * @param backpack the backpack to store
     * @return what the launcher made of it
     */
    public static Result saveBlocking(BackpackData backpack) {
        try {
            SaveBackpackEvent request = new SaveBackpackEvent(backpack);
            ListenerAdapter.sendListeners(request);
            RespondDataEvent response = ListenerAdapter.waitForEvent(request.getEventId(), TIMEOUT);
            if (!(response instanceof RespondBackpackSaveEvent saved)) {
                return new Result(false, "Der Hauptserver hat nicht geantwortet - der Rucksack wurde nicht gespeichert.");
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
     * @param backpack the backpack as it came from the launcher
     * @return its items, ready to be put into an inventory
     */
    public static ItemStack[] toItems(BackpackData backpack) {
        if (backpack == null || backpack.getContents() == null || backpack.getContents().length == 0) {
            return new ItemStack[0];
        }
        try {
            return ItemStack.deserializeItemsFromBytes(backpack.getContents());
        } catch (RuntimeException e) {
            // rather show an empty backpack than refuse to open it at all
            return new ItemStack[0];
        }
    }

    /**
     * @param contents the slots of the backpack inventory
     * @return the bytes to store, or {@code null} when the backpack is empty
     */
    public static byte[] toBytes(ItemStack[] contents) {
        if (contents == null) return null;
        boolean empty = true;
        for (ItemStack item : contents) {
            if (item != null && !item.getType().isAir()) {
                empty = false;
                break;
            }
        }
        if (empty) return null;
        try {
            return ItemStack.serializeItemsAsBytes(contents);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * How a write ended.
     *
     * @param successful whether it was stored
     * @param message    what to tell the player
     */
    public record Result(boolean successful, String message) {
    }
}

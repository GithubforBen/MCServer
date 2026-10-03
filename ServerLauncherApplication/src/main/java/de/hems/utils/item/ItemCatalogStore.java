package de.hems.utils.item;

import de.hems.types.item.ItemCatalog;
import de.hems.utils.webconsole.AdminNetwork;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Callable;

/**
 * Keeps what items can be made of, so the item editor has suggestions and the launcher something to check
 * against even while no game server is running.
 * <p>
 * The catalog comes from a game server - only there are bukkit's registries filled. The first answer of a
 * running server is kept for the life of the launcher and written to disk; after a restart the copy on disk
 * is used until a server answers again, which is also how a minecraft update reaches the website.
 */
public final class ItemCatalogStore {

    /** How long to wait before asking the network again after nobody answered. */
    private static final long RETRY_MILLIS = 60_000L;

    private static final ItemCatalogStore INSTANCE = new ItemCatalogStore(new File("./item-catalog.json"),
            AdminNetwork::requestCatalog);

    private final File file;
    private final Callable<ItemCatalog> network;

    /** The catalog a running server sent during this launcher's life, which is never asked for again. */
    private volatile ItemCatalog live;
    /** The copy from disk, read on first use. */
    private volatile ItemCatalog stored;
    private volatile boolean storedRead;
    private volatile long lastAttempt;

    /**
     * @param file    where the copy on disk lives
     * @param network how a catalog is asked for, returning {@code null} when nobody answered
     */
    public ItemCatalogStore(File file, Callable<ItemCatalog> network) {
        this.file = file;
        this.network = network;
    }

    public static ItemCatalogStore get() {
        return INSTANCE;
    }

    /**
     * @return the best catalog there is - live, from disk, or an empty one; never {@code null}
     */
    public ItemCatalog catalog() {
        ItemCatalog current = live;
        if (current != null) return current;
        synchronized (this) {
            if (live != null) return live;
            long now = System.currentTimeMillis();
            if (now - lastAttempt >= RETRY_MILLIS) {
                lastAttempt = now;
                try {
                    ItemCatalog answered = network.call();
                    if (answered != null && !answered.isEmpty()) {
                        live = answered;
                        stored = answered;
                        storedRead = true;
                        write(answered);
                        return answered;
                    }
                } catch (Exception e) {
                    // nobody to ask right now - the copy on disk has to do
                }
            }
            return stored();
        }
    }

    /**
     * @return whether the catalog came from a server that is running now, rather than from disk
     */
    public boolean isLive() {
        return live != null;
    }

    private ItemCatalog stored() {
        if (!storedRead) {
            storedRead = true;
            stored = read();
        }
        return stored == null ? ItemCatalog.empty() : stored;
    }

    private ItemCatalog read() {
        if (!file.isFile()) return null;
        try {
            return ItemCatalog.fromJson(new JSONObject(Files.readString(file.toPath(), StandardCharsets.UTF_8)));
        } catch (Exception e) {
            System.out.println("Could not read " + file.getName() + ", item suggestions wait for a game server: "
                    + e.getMessage());
            return null;
        }
    }

    private void write(ItemCatalog catalog) {
        Path target = file.toPath().toAbsolutePath();
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Files.writeString(temporary, catalog.toJson().toString(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.out.println("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }
}

package de.hems.paper.admin;

import de.hems.api.CoreProtectAPI;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.admin.RequestCoreProtectEvent;
import de.hems.communication.events.admin.RespondCoreProtectEvent;
import de.hems.types.admin.CoreProtectEntry;
import de.hems.types.admin.LookupQuery;
import net.coreprotect.api.LookupOptions;
import net.coreprotect.api.result.CoreProtectResult;
import net.coreprotect.api.result.ContainerResult;
import net.coreprotect.api.result.InventoryResult;
import net.coreprotect.api.result.ItemResult;
import net.coreprotect.api.result.MessageResult;
import net.coreprotect.api.result.SignResult;
import net.coreprotect.api.result.UsernameResult;
import de.hems.paper.PaperContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs CoreProtect lookups for the admin website.
 * <p>
 * Everything the {@code /co lookup} command can do is reachable from the browser: blocks, containers,
 * items, inventories, sessions, chat, commands, signs and name changes.
 * <p>
 * The work happens on the jgroups receive thread, which is deliberately not the main thread - a lookup
 * queries CoreProtect's database and would otherwise freeze the server for as long as it takes.
 */
public class CoreProtectLookupHandler {

    /** The most rows a single lookup may return, so a careless query cannot flood the network. */
    private static final int MAX_LIMIT = 500;
    /** How long resolving the block on the main thread may take. */
    private static final long BLOCK_TIMEOUT_MS = 5_000L;

    public CoreProtectLookupHandler() {
        ListenerAdapter.register(RequestCoreProtectEvent.class, event -> onLookup((RequestCoreProtectEvent) event));
    }

    private void onLookup(RequestCoreProtectEvent request) throws Exception {
        ArrayList<CoreProtectEntry> entries = new ArrayList<>();
        String error = null;
        try {
            error = lookup(request.getQuery(), entries);
        } catch (NoClassDefFoundError e) {
            // the server runs without CoreProtect at all
            error = "CoreProtect ist auf diesem Server nicht installiert.";
        } catch (Exception e) {
            error = "Die Abfrage ist fehlgeschlagen: " + e.getMessage();
        }
        ListenerAdapter.sendListeners(new RespondCoreProtectEvent(
                request.getSender(), entries, error, request.getEventId()));
    }

    /**
     * Runs one lookup and fills the result list.
     *
     * @param query   what to look up
     * @param entries where to put the rows
     * @return an error to report, or {@code null} when the lookup ran
     */
    private static String lookup(LookupQuery query, List<CoreProtectEntry> entries) {
        if (query == null) return "Es wurde keine Abfrage übergeben.";
        net.coreprotect.CoreProtectAPI api = CoreProtectAPI.getCoreProtect();
        if (api == null) return "CoreProtect ist nicht verfügbar oder die API ist deaktiviert.";

        LookupOptions options = buildOptions(query);
        List<? extends CoreProtectResult> results;
        if (query.getKind() == LookupQuery.Kind.BLOCK) {
            // a block lookup is always about one specific block, so it needs a place to look at
            Block block = resolveBlock(query);
            if (block == null) {
                return "Eine Block-Abfrage braucht eine Position (Welt, X, Y, Z).";
            }
            results = api.blockLookup(block, options);
        } else {
            results = switch (query.getKind()) {
                case CONTAINER -> api.containerLookup(options);
                case ITEM -> api.itemLookup(options);
                case INVENTORY -> api.inventoryLookup(options);
                case SESSION -> api.sessionLookup(options);
                case CHAT -> api.chatLookup(options);
                case COMMAND -> api.commandLookup(options);
                case SIGN -> api.signLookup(options);
                case USERNAME -> api.usernameLookup(options);
                case BLOCK -> throw new IllegalStateException("handled above");
            };
        }
        if (results == null) return null;
        for (CoreProtectResult result : results) entries.add(convert(result));
        return null;
    }

    /**
     * @param query what the browser asked for
     * @return the same thing in CoreProtect's own shape
     */
    private static LookupOptions buildOptions(LookupQuery query) {
        LookupOptions.Builder builder = LookupOptions.builder()
                .time(Math.max(1, query.getTimeSeconds()))
                .limit(Math.max(0, query.getOffset()), Math.min(MAX_LIMIT, Math.max(1, query.getLimit())));
        if (query.getUser() != null && !query.getUser().isBlank()) builder.user(query.getUser().trim());
        Location location = locationOf(query);
        if (location != null) {
            if (query.getRadius() > 0) {
                builder.radius(location, query.getRadius());
            } else {
                builder.location(location);
            }
        }
        return builder.build();
    }

    /**
     * Looks the block up on the main thread.
     * <p>
     * Reading a block can load its chunk, which must not happen from this thread - so the hop is made even
     * though the lookup that follows deliberately stays off the main thread.
     *
     * @param query the query carrying the position
     * @return the block, or {@code null} if no usable position was given
     */
    private static Block resolveBlock(LookupQuery query) {
        Location location = locationOf(query);
        if (location == null || !PaperContext.hasPlugin()) return null;
        try {
            return Bukkit.getScheduler()
                    .callSyncMethod(PaperContext.getPlugin(), location::getBlock)
                    .get(BLOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @param query the query that may carry a place
     * @return that place, or {@code null} if none was given or the world is unknown
     */
    private static Location locationOf(LookupQuery query) {
        if (!query.hasLocation() || query.getWorld() == null || query.getWorld().isBlank()) return null;
        World world = Bukkit.getWorld(query.getWorld());
        if (world == null) return null;
        return new Location(world, query.getX(), query.getY(), query.getZ());
    }

    /**
     * Flattens one row into something that survives the trip to the browser. The shared fields come from
     * the common interface, the interesting extras from whichever result type it actually is.
     *
     * @param result the row CoreProtect produced
     * @return the same row as plain data
     */
    private static CoreProtectEntry convert(CoreProtectResult result) {
        String target = null;
        String detail = null;
        boolean rolledBack = false;

        if (result instanceof ContainerResult container) {
            target = String.valueOf(container.getType());
            detail = container.getAmount() + "x";
            rolledBack = container.isRolledBack();
        } else if (result instanceof ItemResult item) {
            target = String.valueOf(item.getType());
            detail = item.getAmount() + "x";
            rolledBack = item.isRolledBack();
        } else if (result instanceof InventoryResult inventory) {
            target = String.valueOf(inventory.getType());
            detail = inventory.getAmount() + "x " + inventory.getTransactionActionString();
            rolledBack = inventory.isRolledBack();
        } else if (result instanceof MessageResult message) {
            detail = message.getMessage();
        } else if (result instanceof SignResult sign) {
            detail = String.join(" | ", sign.getLines() == null ? new String[0] : sign.getLines());
        } else if (result instanceof UsernameResult username) {
            target = username.getUsername();
            detail = username.getUuid();
        } else if (result instanceof net.coreprotect.api.result.BlockResult block) {
            target = String.valueOf(block.getType());
            rolledBack = block.isRolledBack();
        }

        return new CoreProtectEntry(
                result.getTimestamp() * 1000L,
                result.getPlayer(),
                result.getActionString(),
                target,
                result.worldName(),
                result.getX(), result.getY(), result.getZ(),
                rolledBack,
                detail);
    }
}

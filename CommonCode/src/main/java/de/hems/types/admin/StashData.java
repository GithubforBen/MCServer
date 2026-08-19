package de.hems.types.admin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The admin stash: a container that lives on the launcher and is reachable both from the website and in
 * game with {@code /admin}.
 * <p>
 * It is what the item management drops into. An admin pulling something out of a player's inventory in the
 * browser has to put it somewhere, and a chest they can open on the server is somewhere they can reach it.
 * <p>
 * Unlike the team backpack the contents are kept slot by slot rather than as one opaque blob. The launcher
 * has no bukkit, so it could never unpack a blob into something the browser can draw - but it can pass a
 * list of slots along, each carrying its material, its count and the bytes bukkit made of that one item.
 * The website reads the first two, the game server rebuilds the item from the third.
 */
public class StashData implements Serializable {

    private static final long serialVersionUID = 5001L;

    /** The stash everybody shares. Kept as an id so separate stashes stay possible later. */
    public static final String GLOBAL = "global";

    private String id = GLOBAL;
    private int size = 54;
    private List<ItemData> items = new ArrayList<>();
    /** Bumped on every save, so a stale write is refused instead of silently winning. */
    private long revision;

    public StashData() {
    }

    public StashData(String id, int size, List<ItemData> items, long revision) {
        this.id = id;
        this.size = size;
        this.items = items == null ? new ArrayList<>() : items;
        this.revision = revision;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public List<ItemData> getItems() {
        return items;
    }

    public void setItems(List<ItemData> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }

    public long getRevision() {
        return revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }
}

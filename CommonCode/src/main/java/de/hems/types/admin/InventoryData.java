package de.hems.types.admin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The contents of one container belonging to a player.
 */
public class InventoryData implements Serializable {

    private static final long serialVersionUID = 3002L;

    /** Which container of a player is meant. */
    public enum Kind {
        /** The main inventory including hotbar, armour and off hand. */
        INVENTORY,
        /** The ender chest. */
        ENDER_CHEST,
        /** A backpack, addressed by its id. */
        BACKPACK
    }

    private UUID playerId;
    private String playerName;
    private Kind kind;
    /** Which backpack is meant, empty for the other kinds. */
    private String containerId;
    private String containerTitle;
    private int size;
    private List<ItemData> items = new ArrayList<>();

    public InventoryData() {
    }

    public InventoryData(UUID playerId, String playerName, Kind kind, String containerId,
                         String containerTitle, int size, List<ItemData> items) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.kind = kind;
        this.containerId = containerId;
        this.containerTitle = containerTitle;
        this.size = size;
        this.items = items == null ? new ArrayList<>() : items;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Kind getKind() {
        return kind;
    }

    public String getContainerId() {
        return containerId;
    }

    public String getContainerTitle() {
        return containerTitle;
    }

    public int getSize() {
        return size;
    }

    public List<ItemData> getItems() {
        return items;
    }

    public void setItems(List<ItemData> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }
}

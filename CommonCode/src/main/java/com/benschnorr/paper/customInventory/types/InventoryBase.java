package com.benschnorr.paper.customInventory.types;

import com.benschnorr.api.ServerApi;
import com.benschnorr.paper.customInventory.CustomInventory;
import com.benschnorr.paper.servermanager.ServerManagerUi;
import com.benschnorr.types.Server;
import org.bukkit.entity.Player;

/**
 * Shared inventories. The server management screens moved to {@link ServerManagerUi}, which loads the
 * server list off the main thread and can handle any number of servers.
 */
public class InventoryBase {

    /**
     * Opens the server manager for a player. Prefer this over {@link #SERVERINVENTORY()}, it does not block
     * the server while the host is asked for its servers.
     *
     * @param player the player that opens the manager
     */
    public static void openServerManager(Player player) {
        ServerManagerUi.openServerList(player);
    }

    /**
     * Builds the server overview. Blocks until the host answered, so it must not be called on the main
     * thread - {@link #openServerManager(Player)} does that for you.
     *
     * @return the overview of every running server
     * @deprecated use {@link #openServerManager(Player)}
     */
    @Deprecated
    public static CustomInventory SERVERINVENTORY() throws Exception {
        Server[] servers = ServerApi.listServers();
        return ServerManagerUi.serverOverview(servers);
    }
}

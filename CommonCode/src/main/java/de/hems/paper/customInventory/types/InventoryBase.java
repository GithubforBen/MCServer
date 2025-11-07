package de.hems.paper.customInventory.types;

import de.hems.api.ItemApi;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.server.RequestServerRestartEvent;
import de.hems.communication.events.server.RequestServerStopEvent;
import de.hems.communication.events.server.RequestServersEvent;
import de.hems.communication.events.server.RespondServersEvent;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.types.Server;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

import java.util.List;
import java.util.UUID;

public class InventoryBase {
    public static CustomInventory SERVERINVENTORY() throws Exception {
        CustomInventory customInventory = new CustomInventory(5*9, "Servers", null);
        RequestServersEvent requestServersEvent = new RequestServersEvent(ListenerAdapter.ServerName.HOST);
        ListenerAdapter.sendListeners(requestServersEvent);
        Server[] data = ((RespondServersEvent) ListenerAdapter.waitForEvent(requestServersEvent.getEventId())).getData();
        for (int i = 0; i < data.length; i++) {
            int finalI = i;
            customInventory.setItem(i, new ItemApi(Material.RED_WOOL, data[i].name, List.of("RAM [MB]:" + data[i].memory, "PORT:" + data[i].port)).build(), new ItemAction() {
                @Override
                public UUID getID() {
                    return UUID.fromString("6c27eae2-740c-4416-b781-21a989f73de5");
                }

                @Override
                public void onClick(InventoryClickEvent event) {
                }

                @Override
                public boolean isMovable() {
                    return false;
                }

                @Override
                public boolean fireEvent() {
                    return false;
                }

                @Override
                public CustomInventory loadInventoryOnClick() {
                    return SERVERSELECTORINVENTORY(data[finalI].name);
                }
            });
        }
        //TODO: start new instance
        return customInventory;
    }

    private static CustomInventory SERVERSELECTORINVENTORY(String server) {
        CustomInventory customInventory = new CustomInventory(InventoryType.DROPPER, "Server settings:" + server, null);
        for (int i = 0; i <3; i++) {
            customInventory.setPlaceHolder(i);
        }
        customInventory.setItem(3, new ItemApi(Material.RED_WOOL, "STOP").build(), new ItemAction() {
            @Override
            public UUID getID() {
                return UUID.fromString("c576c769-435b-4dda-8d39-b1da466df00a");
            }

            @Override
            public void onClick(InventoryClickEvent event) {
                try {
                    ListenerAdapter.sendListeners(new RequestServerStopEvent(ListenerAdapter.ServerName.HOST, event.getView().getTitle().replace("Server settings:", "")));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                event.getWhoClicked().closeInventory();
            }

            @Override
            public boolean isMovable() {
                return false;
            }

            @Override
            public boolean fireEvent() {
                return true;
            }

            @Override
            public CustomInventory loadInventoryOnClick() {
                return null;
            }
        });
        customInventory.setItem(4, new ItemApi(Material.BLUE_WOOL, "Restart").build(), new ItemAction() {
            @Override
            public UUID getID() {
                return UUID.fromString("413ce363-a326-44e0-8a12-47f9d3e8ab93");
            }

            @Override
            public void onClick(InventoryClickEvent event) {
                try {
                    ListenerAdapter.sendListeners(new RequestServerRestartEvent(ListenerAdapter.ServerName.HOST, event.getView().getTitle().replace("Server settings:", "")));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                event.getWhoClicked().closeInventory();
            }

            @Override
            public boolean isMovable() {
                return false;
            }

            @Override
            public boolean fireEvent() {
                return true;
            }

            @Override
            public CustomInventory loadInventoryOnClick() {
                return null;
            }
        });
        customInventory.setItem(5, new ItemApi(Material.ARROW, "Back").build(), new ItemAction() {
            @Override
            public UUID getID() {
                return UUID.fromString("ae4bee14-b9d1-4d58-840f-641defcb9515");
            }

            @Override
            public void onClick(InventoryClickEvent event) {

            }

            @Override
            public boolean isMovable() {
                return false;
            }

            @Override
            public boolean fireEvent() {
                return false;
            }

            @Override
            public CustomInventory loadInventoryOnClick() {
                try {
                    return SERVERINVENTORY();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        for (int i = 6; i <9; i++) {
            customInventory.setPlaceHolder(i);
        }
        return customInventory;
    }
}

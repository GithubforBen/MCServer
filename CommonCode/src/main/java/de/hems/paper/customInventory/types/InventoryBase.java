package de.hems.paper.customInventory.types;

import de.hems.api.ItemApi;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.server.*;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.types.FileType;
import de.hems.types.Server;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.net.MalformedURLException;
import java.net.URL;
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
        customInventory.setItem(5 * 9 - 1, new ItemApi(new URL("http://textures.minecraft.net/texture/171d8979c1878a05987a7faf21b56d1b744f9d068c74cffcde1ea1edad5852"), ChatColor.GREEN + "Neuer Server").buildSkull(), new ItemAction() {//plus
            @Override
            public UUID getID() {
                return UUID.fromString("c68266f9-2cfc-4de9-897b-e465cc037e8f");
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
                return SERVERCREATEINVENTORY();
            }
        });
        return customInventory;
    }
    private static CustomInventory SERVERSTARTINVENTORY(String serverName, int ramGB) throws MalformedURLException {
        CustomInventory customInventory = new CustomInventory(InventoryType.DROPPER, serverName + ":" + ramGB, null);
        for (int i = 0; i < 3; i++) {
            customInventory.setPlaceHolder(i);
        }
        customInventory.setItem(3, new ItemApi(new URL("http://textures.minecraft.net/texture/93d7a9ee31348a35754383c167fa33abc02e8e68ca2c4a9691400e7fe34b3eb5"), "-").buildSkull(), new ItemAction() {
            @Override
            public UUID getID() {
                return UUID.fromString("a83e36bd-9686-40a1-aba7-dad92c12619f");
            }

            @Override
            public void onClick(InventoryClickEvent event) {
                Integer i = Integer.valueOf(event.getView().getTitle().split(":")[1]);
                String name = event.getView().getTitle().split(":")[0];
                try {
                    event.getWhoClicked().openInventory(SERVERSTARTINVENTORY(
                            name,
                            i - 1
                    ).getInventory());
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
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
        });//minus
        customInventory.setItem(4, new ItemApi(Material.REDSTONE_BLOCK, ramGB, "Ram").build(), ItemAction.NOTMOVABLE);
        customInventory.setItem(5, new ItemApi(new URL("http://textures.minecraft.net/texture/171d8979c1878a05987a7faf21b56d1b744f9d068c74cffcde1ea1edad5852"), "+").buildSkull(), new ItemAction() {
            @Override
            public UUID getID() {
                return UUID.fromString("6b04c776-a580-4f36-b7ff-fca7c431f4d6");
            }

            @Override
            public void onClick(InventoryClickEvent event) {
                Integer i = Integer.valueOf(event.getView().getTitle().split(":")[1]);
                String name = event.getView().getTitle().split(":")[0];
                try {
                    event.getWhoClicked().openInventory(SERVERSTARTINVENTORY(
                            name,
                            i + 1
                    ).getInventory());
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
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
        });//plus
        customInventory.setPlaceHolder(6);
        customInventory.setPlaceHolder(7);
        customInventory.setItem(8, new  ItemApi(new URL("http://textures.minecraft.net/texture/a79a5c95ee17abfef45c8dc224189964944d560f19a44f19f8a46aef3fee4756"), ChatColor.GREEN + "Starten").buildSkull(), new ItemAction() {

            @Override
            public UUID getID() {
                return UUID.fromString("31c64fb5-9b3e-4daa-b987-d4fc5c558587");
            }

            @Override
            public void onClick(InventoryClickEvent event) {
                Integer i = Integer.valueOf(event.getView().getTitle().split(":")[1]);
                String name = event.getView().getTitle().split(":")[0];
                try {
                    ListenerAdapter.sendListeners(new RequestServerStartEvent(ListenerAdapter.ServerName.HOST, ListenerAdapter.ServerName.valueOf(name), FileType.SERVER.PAPER, i*1024, new FileType.PLUGIN[]{FileType.PLUGIN.WORLDEDIT}));
                    event.getWhoClicked().closeInventory();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
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
        return customInventory;
    }

    private static CustomInventory SERVERCREATEINVENTORY() {
        CustomInventory customInventory = new CustomInventory(InventoryType.DROPPER, "Server erstellen", null);
        for (int i = 0; i < ListenerAdapter.ServerName.values().length; i++) {//TODO: use actual server list e.g. SURRVIVAL, EVENT, LOBBY, SERVER1
            customInventory.setItem(i, new ItemApi(Material.WHITE_WOOL, ListenerAdapter.ServerName.values()[i].name()).build(), new ItemAction() {

                @Override
                public UUID getID() {
                    return UUID.fromString("35b4aac5-5ab8-452d-b9bc-603d291326de");
                }

                @Override
                public void onClick(InventoryClickEvent event) {
                    ItemStack currentItem = event.getCurrentItem();
                    System.out.println(currentItem);
                    if (currentItem == null) return;
                    if (currentItem.getItemMeta() == null) return;
                    String name = currentItem.getItemMeta().getDisplayName();
                    try {
                        System.out.println("Open onventory" + name);
                        event.getWhoClicked().closeInventory();
                        event.getWhoClicked().openInventory(SERVERSTARTINVENTORY(name, 2).getInventory());
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
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
        }
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
                    ListenerAdapter.sendListeners(new RequestServerStopEvent(ListenerAdapter.ServerName.HOST, ListenerAdapter.ServerName.valueOf(event.getView().getTitle().replace("Server settings:", ""))));
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
                    ListenerAdapter.sendListeners(new RequestServerRestartEvent(ListenerAdapter.ServerName.HOST, ListenerAdapter.ServerName.valueOf(event.getView().getTitle().replace("Server settings:", ""))));
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

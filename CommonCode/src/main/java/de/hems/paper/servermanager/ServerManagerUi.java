package de.hems.paper.servermanager;

import de.hems.api.ItemApi;
import de.hems.api.ServerApi;
import de.hems.communication.ListenerAdapter;
import de.hems.paper.PaperContext;
import de.hems.paper.commands.WarpCommand;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.hems.paper.util.ChatPrompt;
import de.hems.paper.warp.ServerConnector;
import de.hems.types.FileType;
import de.hems.types.Server;
import de.hems.types.ServerTemplate;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * The in-game server manager.
 * <p>
 * It shows every running server, lets an admin build a new one step by step - name, memory and, most
 * importantly, a free selection of the available plugins - and warps players to any of them. Everything it
 * does goes through {@link ServerApi}, so the exact same actions are available programmatically for
 * automatically created event servers.
 */
public final class ServerManagerUi {

    private static final int ROWS = 5;
    private static final int SIZE = ROWS * 9;

    private ServerManagerUi() {
    }

    /* ------------------------------------------------------------------ server list */

    /**
     * Loads the servers from the host and shows them.
     *
     * @param player the admin that opened the manager
     */
    public static void openServerList(Player player) {
        player.sendMessage(ChatColor.GRAY + "Lade Server...");
        PaperContext.async(() -> {
            Server[] servers;
            try {
                servers = ServerApi.listServers();
            } catch (Exception e) {
                PaperContext.sync(() -> player.sendMessage(ChatColor.RED + "Der Host antwortet gerade nicht."));
                return;
            }
            List<Server> joinable = new ArrayList<>();
            for (Server server : servers) if (server.isJoinable()) joinable.add(server);
            WarpCommand.refreshCompletions(joinable);
            PaperContext.sync(() -> player.openInventory(serverListInventory(servers).getInventory()));
        });
    }

    /**
     * Builds the overview of every running server.
     *
     * @param servers the servers the host reported
     * @return the inventory to show
     */
    public static CustomInventory serverOverview(Server[] servers) {
        return serverListInventory(servers);
    }

    private static CustomInventory serverListInventory(Server[] servers) {
        CustomInventory inventory = new CustomInventory(SIZE, "Server (" + servers.length + ")", null);
        for (int i = 0; i < servers.length && i < SIZE - 9; i++) {
            Server server = servers[i];
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Status: " + (server.online ? ChatColor.GREEN + "läuft" : ChatColor.RED + "offline"));
            lore.add(ChatColor.GRAY + "RAM: " + ChatColor.WHITE + server.memory + " MB");
            lore.add(ChatColor.GRAY + "Port: " + ChatColor.WHITE + server.port);
            if (server.template != null) {
                lore.add(ChatColor.GRAY + "Vorlage: " + ChatColor.WHITE + server.template.getDisplayName());
            }
            lore.add(ChatColor.GRAY + "Plugins: " + ChatColor.WHITE + server.getPlugins().size());
            for (FileType.PLUGIN plugin : server.getPlugins()) {
                lore.add(ChatColor.DARK_GRAY + " - " + plugin.getDisplayName());
            }
            lore.add(" ");
            lore.add(ChatColor.YELLOW + "Linksklick: " + ChatColor.GRAY + "Einstellungen");
            if (server.isJoinable()) lore.add(ChatColor.YELLOW + "Rechtsklick: " + ChatColor.GRAY + "hin warpen");
            Material material = server.online ? Material.LIME_WOOL : Material.RED_WOOL;
            inventory.setItem(i, new ItemApi(material, ChatColor.AQUA + server.name, lore).build(),
                    new SimpleItemAction((event) -> {
                        Player clicker = (Player) event.getWhoClicked();
                        if (event.isRightClick() && server.isJoinable()) {
                            clicker.closeInventory();
                            ServerConnector.connect(clicker, server.name);
                            return;
                        }
                        openServerSettings(clicker, server);
                    }));
        }
        for (int i = SIZE - 9; i < SIZE; i++) inventory.setPlaceHolder(i);
        inventory.setItem(SIZE - 9, new ItemApi(Material.CLOCK, ChatColor.YELLOW + "Aktualisieren").build(),
                new SimpleItemAction((event) -> openServerList((Player) event.getWhoClicked())));
        inventory.setItem(SIZE - 5, new ItemApi(Material.ENDER_PEARL, ChatColor.LIGHT_PURPLE + "Warp Menü",
                        List.of(ChatColor.GRAY + "Zu einem Server springen")).build(),
                new SimpleItemAction((event) -> openWarpMenu((Player) event.getWhoClicked())));
        inventory.setItem(SIZE - 1, new ItemApi(Material.NETHER_STAR, ChatColor.GREEN + "Neuer Server",
                        List.of(ChatColor.GRAY + "Vorlage, RAM und Plugins auswählen")).build(),
                new SimpleItemAction((event) -> openTemplateMenu((Player) event.getWhoClicked())));
        return inventory;
    }

    /* -------------------------------------------------------------- server settings */

    /**
     * Shows what can be done with one running server.
     *
     * @param player the admin
     * @param server the server that was clicked
     */
    public static void openServerSettings(Player player, Server server) {
        CustomInventory inventory = new CustomInventory(9, "Server: " + server.name, null);
        for (int i = 0; i < 9; i++) inventory.setPlaceHolder(i);
        inventory.setItem(1, new ItemApi(Material.RED_WOOL, ChatColor.RED + "Stoppen",
                        List.of(ChatColor.GRAY + server.name + " herunterfahren")).build(),
                new SimpleItemAction((event) -> {
                    Player clicker = (Player) event.getWhoClicked();
                    clicker.closeInventory();
                    run(clicker, () -> ServerApi.stopServer(server.name),
                            ChatColor.YELLOW + server.name + " wird gestoppt.");
                }));
        inventory.setItem(3, new ItemApi(Material.BLUE_WOOL, ChatColor.BLUE + "Neustarten",
                        List.of(ChatColor.GRAY + server.name + " neu starten")).build(),
                new SimpleItemAction((event) -> {
                    Player clicker = (Player) event.getWhoClicked();
                    clicker.closeInventory();
                    run(clicker, () -> ServerApi.restartServer(server.name),
                            ChatColor.YELLOW + server.name + " wird neu gestartet.");
                }));
        inventory.setItem(5, new ItemApi(Material.ENDER_PEARL, ChatColor.LIGHT_PURPLE + "Hin warpen",
                        List.of(ChatColor.GRAY + "Zu " + server.name + " springen")).build(),
                new SimpleItemAction((event) -> {
                    Player clicker = (Player) event.getWhoClicked();
                    clicker.closeInventory();
                    ServerConnector.connect(clicker, server.name);
                }));
        inventory.setItem(7, new ItemApi(Material.ARROW, ChatColor.GRAY + "Zurück").build(),
                new SimpleItemAction((event) -> openServerList((Player) event.getWhoClicked())));
        player.openInventory(inventory.getInventory());
    }

    /* --------------------------------------------------------------- server creation */

    /**
     * Step one of creating a server: pick the blueprint.
     *
     * @param player the admin
     */
    public static void openTemplateMenu(Player player) {
        CustomInventory inventory = new CustomInventory(27, "Vorlage wählen", null);
        int slot = 10;
        for (ServerTemplate template : ServerTemplate.values()) {
            if (template == ServerTemplate.PROXY) continue; // there is exactly one proxy, it is never created here
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + template.getDescription());
            lore.add(ChatColor.GRAY + "RAM: " + ChatColor.WHITE + template.getDefaultMemoryMB() + " MB");
            lore.add(ChatColor.GRAY + "Plugins:");
            for (FileType.PLUGIN plugin : template.getRequiredPlugins()) {
                lore.add(ChatColor.DARK_GRAY + " - " + plugin.getDisplayName());
            }
            inventory.setItem(slot, new ItemApi(materialOf(template), ChatColor.AQUA + template.getDisplayName(), lore).build(),
                    new SimpleItemAction((event) -> {
                        Player clicker = (Player) event.getWhoClicked();
                        startDraft(clicker, template);
                    }));
            slot++;
        }
        inventory.setItem(18, new ItemApi(Material.ARROW, ChatColor.GRAY + "Zurück").build(),
                new SimpleItemAction((event) -> openServerList((Player) event.getWhoClicked())));
        player.openInventory(inventory.getInventory());
    }

    private static void startDraft(Player player, ServerTemplate template) {
        player.closeInventory();
        PaperContext.async(() -> {
            String name;
            try {
                name = ServerApi.freeName(template.name());
            } catch (Exception e) {
                name = template.name();
            }
            String suggested = name;
            PaperContext.sync(() -> {
                ServerDraft.start(player, suggested, template);
                openConfigMenu(player);
            });
        });
    }

    /**
     * Step two: name, memory and the plugin selection of the server that is being created.
     *
     * @param player the admin
     */
    public static void openConfigMenu(Player player) {
        ServerDraft draft = ServerDraft.of(player);
        if (draft == null) {
            openTemplateMenu(player);
            return;
        }
        CustomInventory inventory = new CustomInventory(SIZE, "Server erstellen", null);
        for (int i = 0; i < SIZE; i++) inventory.setPlaceHolder(i);

        inventory.setItem(10, new ItemApi(Material.NAME_TAG, ChatColor.AQUA + "Name: " + ChatColor.WHITE + draft.getName(),
                        List.of(ChatColor.GRAY + "Klicken um den Namen zu ändern")).build(),
                new SimpleItemAction((event) -> {
                    Player clicker = (Player) event.getWhoClicked();
                    ChatPrompt.ask(clicker, "Wie soll der Server heißen?", (answer) -> {
                        ServerDraft current = ServerDraft.of(clicker);
                        if (current == null) return;
                        try {
                            current.setName(ListenerAdapter.ServerName.normalize(answer));
                        } catch (IllegalArgumentException e) {
                            clicker.sendMessage(ChatColor.RED + "'" + answer + "' geht nicht als Name.");
                        }
                        openConfigMenu(clicker);
                    });
                }));

        inventory.setItem(12, new ItemApi(materialOf(draft.getTemplate()),
                        ChatColor.AQUA + "Vorlage: " + ChatColor.WHITE + draft.getTemplate().getDisplayName(),
                        List.of(ChatColor.GRAY + draft.getTemplate().getDescription(),
                                ChatColor.GRAY + "Software: " + ChatColor.WHITE + draft.getTemplate().getSoftware().getDisplayName(),
                                ChatColor.YELLOW + "Klicken für eine andere Vorlage")).build(),
                new SimpleItemAction((event) -> openTemplateMenu((Player) event.getWhoClicked())));

        inventory.setItem(14, new ItemApi(Material.REDSTONE_BLOCK,
                        ChatColor.AQUA + "RAM: " + ChatColor.WHITE + draft.getMemoryMB() + " MB",
                        List.of(ChatColor.YELLOW + "Linksklick: " + ChatColor.GRAY + "+" + ServerDraft.MEMORY_STEP_MB + " MB",
                                ChatColor.YELLOW + "Rechtsklick: " + ChatColor.GRAY + "-" + ServerDraft.MEMORY_STEP_MB + " MB",
                                ChatColor.YELLOW + "Shift: " + ChatColor.GRAY + "4x so viel")).build(),
                new SimpleItemAction((event) -> {
                    Player clicker = (Player) event.getWhoClicked();
                    ServerDraft current = ServerDraft.of(clicker);
                    if (current == null) return;
                    int step = ServerDraft.MEMORY_STEP_MB * (event.isShiftClick() ? 4 : 1);
                    current.addMemory(event.isRightClick() ? -step : step);
                    openConfigMenu(clicker);
                }));

        inventory.setItem(16, new ItemApi(Material.CHEST,
                        ChatColor.AQUA + "Plugins: " + ChatColor.WHITE + draft.getSelectedPlugins().size(),
                        pluginSummary(draft)).build(),
                new SimpleItemAction((event) -> openPluginMenu((Player) event.getWhoClicked())));

        inventory.setItem(31, new ItemApi(Material.EMERALD_BLOCK, ChatColor.GREEN + "Server starten",
                        List.of(ChatColor.GRAY + draft.getName() + " mit " + draft.getMemoryMB() + " MB starten")).build(),
                new SimpleItemAction((event) -> {
                    Player clicker = (Player) event.getWhoClicked();
                    ServerDraft current = ServerDraft.of(clicker);
                    if (current == null) return;
                    clicker.closeInventory();
                    createServer(clicker, current);
                }));

        inventory.setItem(SIZE - 9, new ItemApi(Material.ARROW, ChatColor.GRAY + "Zurück").build(),
                new SimpleItemAction((event) -> openServerList((Player) event.getWhoClicked())));
        player.openInventory(inventory.getInventory());
    }

    private static List<String> pluginSummary(ServerDraft draft) {
        List<String> lore = new ArrayList<>();
        for (FileType.PLUGIN plugin : draft.getSelectedPlugins()) {
            lore.add(ChatColor.GREEN + " + " + plugin.getDisplayName());
        }
        lore.add(" ");
        lore.add(ChatColor.YELLOW + "Klicken um Plugins zu wählen");
        return lore;
    }

    /**
     * Step three: turn the available plugins on and off.
     *
     * @param player the admin
     */
    public static void openPluginMenu(Player player) {
        ServerDraft draft = ServerDraft.of(player);
        if (draft == null) {
            openTemplateMenu(player);
            return;
        }
        FileType.SERVER software = draft.getTemplate().getSoftware();
        CustomInventory inventory = new CustomInventory(SIZE, "Plugins auswählen", null);
        List<FileType.PLUGIN> plugins = FileType.PLUGIN.selectableFor(software);
        for (int i = 0; i < plugins.size() && i < SIZE - 9; i++) {
            FileType.PLUGIN plugin = plugins.get(i);
            boolean selected = draft.isSelected(plugin);
            boolean required = draft.isRequired(plugin);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + plugin.getDescription());
            lore.add(" ");
            if (required) {
                lore.add(ChatColor.GOLD + "Gehört zur Vorlage " + draft.getTemplate().getDisplayName());
                lore.add(ChatColor.GRAY + "Wird immer installiert");
            } else if (selected) {
                lore.add(ChatColor.GREEN + "Ausgewählt");
                lore.add(ChatColor.YELLOW + "Klicken zum Entfernen");
            } else {
                lore.add(ChatColor.RED + "Nicht ausgewählt");
                lore.add(ChatColor.YELLOW + "Klicken zum Hinzufügen");
            }
            Material material = required ? Material.GOLDEN_APPLE : (selected ? Material.LIME_DYE : Material.GRAY_DYE);
            String title = (selected ? ChatColor.GREEN : ChatColor.RED) + plugin.getDisplayName();
            inventory.setItem(i, new ItemApi(material, title, lore).build(), new SimpleItemAction((event) -> {
                Player clicker = (Player) event.getWhoClicked();
                ServerDraft current = ServerDraft.of(clicker);
                if (current == null) return;
                if (current.isRequired(plugin)) {
                    clicker.sendMessage(ChatColor.GRAY + plugin.getDisplayName()
                            + " gehört fest zur Vorlage " + current.getTemplate().getDisplayName() + ".");
                    return;
                }
                current.toggle(plugin);
                openPluginMenu(clicker);
            }));
        }
        for (int i = SIZE - 9; i < SIZE; i++) inventory.setPlaceHolder(i);
        inventory.setItem(SIZE - 9, new ItemApi(Material.ARROW, ChatColor.GRAY + "Zurück").build(),
                new SimpleItemAction((event) -> openConfigMenu((Player) event.getWhoClicked())));
        inventory.setItem(SIZE - 1, new ItemApi(Material.EMERALD, ChatColor.GREEN + "Fertig",
                        List.of(ChatColor.GRAY + "" + draft.getSelectedPlugins().size() + " Plugins ausgewählt")).build(),
                new SimpleItemAction((event) -> openConfigMenu((Player) event.getWhoClicked())));
        player.openInventory(inventory.getInventory());
    }

    private static void createServer(Player player, ServerDraft draft) {
        String name = draft.getName();
        ServerTemplate template = draft.getTemplate();
        int memory = draft.getMemoryMB();
        List<FileType.PLUGIN> extras = new ArrayList<>(draft.getExtraPlugins());
        ServerDraft.clear(player);
        player.sendMessage(ChatColor.GRAY + "Starte " + ChatColor.AQUA + name + ChatColor.GRAY + "...");
        PaperContext.async(() -> {
            try {
                ServerApi.createServer(name, template, memory, extras);
            } catch (Exception e) {
                PaperContext.sync(() -> player.sendMessage(ChatColor.RED + "Konnte " + name + " nicht starten: " + e.getMessage()));
                return;
            }
            PaperContext.sync(() -> player.sendMessage(ChatColor.GREEN + name
                    + " wird gestartet. Sobald er läuft kannst du mit /warp " + name + " hin."));
        });
    }

    /* ------------------------------------------------------------------------ warping */

    /**
     * Shows every server players can be warped to.
     *
     * @param player the player that wants to warp
     */
    public static void openWarpMenu(Player player) {
        player.sendMessage(ChatColor.GRAY + "Lade Server...");
        PaperContext.async(() -> {
            List<Server> servers;
            try {
                servers = ServerApi.listJoinableServers();
            } catch (Exception e) {
                PaperContext.sync(() -> player.sendMessage(ChatColor.RED + "Der Host antwortet gerade nicht."));
                return;
            }
            WarpCommand.refreshCompletions(servers);
            PaperContext.sync(() -> player.openInventory(warpInventory(servers).getInventory()));
        });
    }

    private static CustomInventory warpInventory(List<Server> servers) {
        int rows = Math.max(1, Math.min(6, (servers.size() / 9) + 1));
        CustomInventory inventory = new CustomInventory(rows * 9, "Wohin willst du?", null);
        for (int i = 0; i < servers.size() && i < rows * 9; i++) {
            Server server = servers.get(i);
            boolean here = ListenerAdapter.ServerName.valueOf(server.name).equals(ListenerAdapter.getName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "RAM: " + ChatColor.WHITE + server.memory + " MB");
            if (server.template != null) {
                lore.add(ChatColor.GRAY + "Vorlage: " + ChatColor.WHITE + server.template.getDisplayName());
            }
            lore.add(here ? ChatColor.YELLOW + "Du bist hier" : ChatColor.GREEN + "Klicken zum Warpen");
            inventory.setItem(i, new ItemApi(here ? Material.COMPASS : Material.ENDER_PEARL,
                    ChatColor.AQUA + server.name, lore).build(), new SimpleItemAction((event) -> {
                Player clicker = (Player) event.getWhoClicked();
                clicker.closeInventory();
                ServerConnector.connect(clicker, server.name);
            }));
        }
        return inventory;
    }

    /* -------------------------------------------------------------------------- utils */

    private static Material materialOf(ServerTemplate template) {
        return switch (template) {
            case PROXY -> Material.BEACON;
            case LOBBY -> Material.WHITE_WOOL;
            case SURVIVAL -> Material.GRASS_BLOCK;
            case BEDWARS -> Material.RED_BED;
            case EVENT -> Material.CRAFTING_TABLE;
        };
    }

    private interface ApiCall {
        void run() throws Exception;
    }

    private static void run(Player player, ApiCall call, String successMessage) {
        PaperContext.async(() -> {
            try {
                call.run();
            } catch (Exception e) {
                PaperContext.sync(() -> player.sendMessage(ChatColor.RED + "Das hat nicht geklappt: " + e.getMessage()));
                return;
            }
            PaperContext.sync(() -> player.sendMessage(successMessage));
        });
    }
}

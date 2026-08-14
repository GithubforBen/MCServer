package de.hems.paper.eventcalendar;

import de.hems.api.ItemApi;
import de.hems.event.EventApi;
import de.hems.event.EventCalendar;
import de.hems.event.EventDefinition;
import de.hems.event.EventRegistry;
import de.hems.event.EventTeam;
import de.hems.event.ScheduledEvent;
import de.hems.event.ranking.Ranking;
import de.hems.event.ranking.RankingStrategies;
import de.hems.event.ranking.RankingStrategy;
import de.hems.paper.PaperContext;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.hems.paper.util.ChatPrompt;
import de.hems.paper.warp.ServerConnector;
import de.hems.types.FileType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The event calendar as players see it: the next {@value de.hems.event.EventCalendar#DAYS} days, what
 * happens on them and who plays in which team.
 * <p>
 * Admins build new events right here - pick the kind of event, configure it and then click the days it
 * takes place on. Everything that is saved goes to the host, which announces it to all servers, so the
 * calendar looks the same everywhere.
 */
public final class EventCalendarUi {

    private static final int SIZE = 6 * 9;
    /** The first 27 slots are the days - three rows of nine. */
    private static final int DAY_SLOTS = EventCalendar.DAYS;
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("EE, dd.MM.", Locale.GERMAN);
    private static final DateTimeFormatter LONG_FORMAT = DateTimeFormatter.ofPattern("EEEE, dd. MMMM", Locale.GERMAN);

    private EventCalendarUi() {
    }

    /* -------------------------------------------------------------------------- calendar */

    /**
     * Loads the calendar from the host and shows it.
     *
     * @param player the player that opens the calendar
     */
    public static void open(Player player) {
        player.sendMessage(ChatColor.GRAY + "Lade Kalender...");
        PaperContext.async(() -> {
            try {
                EventCalendar.refresh();
            } catch (Exception e) {
                PaperContext.sync(() -> player.sendMessage(ChatColor.RED + "Der Kalender ist gerade nicht erreichbar."));
                return;
            }
            PaperContext.sync(() -> openCalendar(player, false));
        });
    }

    /**
     * Shows the calendar without asking the host again.
     *
     * @param player   the player
     * @param planning whether clicking a day plans the event the player is building
     */
    public static void openCalendar(Player player, boolean planning) {
        player.openInventory(calendarInventory(player, planning).getInventory());
    }

    private static CustomInventory calendarInventory(Player player, boolean planning) {
        EventDraft draft = planning ? EventDraft.of(player) : null;
        if (planning && draft == null) {
            planning = false;
        }
        String title = planning ? "Tage wählen" : "Event Kalender";
        CustomInventory inventory = new CustomInventory(SIZE, title, null);
        List<LocalDate> window = EventCalendar.getWindow();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < DAY_SLOTS; i++) {
            LocalDate day = window.get(i);
            List<ScheduledEvent> events = EventCalendar.getEventsOn(day);
            boolean selected = draft != null && draft.getEvent().isOn(day);
            List<String> lore = new ArrayList<>();
            if (day.equals(today)) lore.add(ChatColor.GOLD + "Heute");
            if (events.isEmpty()) {
                lore.add(ChatColor.DARK_GRAY + "Keine Events");
            } else {
                for (ScheduledEvent event : events) {
                    lore.add(ChatColor.GRAY + " - " + ChatColor.WHITE + event.getName()
                            + ChatColor.DARK_GRAY + " (" + event.getDefinition().getDisplayName() + ")");
                }
            }
            lore.add(" ");
            if (draft != null) {
                lore.add(selected ? ChatColor.GREEN + "Für dieses Event geplant" : ChatColor.RED + "Nicht geplant");
                lore.add(ChatColor.YELLOW + "Klicken zum Umschalten");
            } else if (!events.isEmpty()) {
                lore.add(ChatColor.YELLOW + "Klicken für die Events des Tages");
            }

            Material material;
            if (draft != null) {
                material = selected ? Material.LIME_DYE : Material.GRAY_DYE;
            } else if (!events.isEmpty()) {
                material = Material.ENCHANTED_BOOK;
            } else {
                material = Material.PAPER;
            }
            String name = (day.equals(today) ? ChatColor.GOLD : ChatColor.AQUA) + day.format(DAY_FORMAT);
            ItemApi item = new ItemApi(material, name, lore);
            org.bukkit.inventory.ItemStack stack = item.build();
            stack.setAmount(Math.max(1, day.getDayOfMonth()));
            boolean finalPlanning = draft != null;
            inventory.setItem(i, stack, new SimpleItemAction((event) -> {
                Player clicker = (Player) event.getWhoClicked();
                if (finalPlanning) {
                    EventDraft current = EventDraft.of(clicker);
                    if (current == null) {
                        openCalendar(clicker, false);
                        return;
                    }
                    current.toggleDay(day);
                    openCalendar(clicker, true);
                    return;
                }
                openDay(clicker, day);
            }));
        }

        for (int i = DAY_SLOTS; i < SIZE; i++) inventory.setPlaceHolder(i);

        if (draft != null) {
            ScheduledEvent event = draft.getEvent();
            inventory.setItem(31, new ItemApi(Material.BOOK, ChatColor.AQUA + event.getName(),
                            List.of(ChatColor.GRAY + "Art: " + ChatColor.WHITE + event.getDefinition().getDisplayName(),
                                    ChatColor.GRAY + "Tage: " + ChatColor.WHITE + event.getDays().size())).build(),
                    SimpleItemAction.display());
            inventory.setItem(SIZE - 9, new ItemApi(Material.ARROW, ChatColor.GRAY + "Zurück zum Event").build(),
                    new SimpleItemAction((clickEvent) -> openDraft((Player) clickEvent.getWhoClicked())));
            inventory.setItem(SIZE - 1, new ItemApi(Material.EMERALD_BLOCK, ChatColor.GREEN + "Tage übernehmen",
                            List.of(ChatColor.GRAY + "" + event.getDays().size() + " Tage ausgewählt")).build(),
                    new SimpleItemAction((clickEvent) -> openDraft((Player) clickEvent.getWhoClicked())));
            return inventory;
        }

        // overview of what is coming up next
        List<ScheduledEvent> upcoming = EventCalendar.getEvents();
        int slot = 36;
        for (ScheduledEvent event : upcoming) {
            if (slot > 44) break;
            LocalDate next = event.getNextDay();
            if (next == null || !EventCalendar.isInWindow(next)) continue;
            inventory.setItem(slot++, eventItem(event, player), new SimpleItemAction((clickEvent) ->
                    openEvent((Player) clickEvent.getWhoClicked(), event.getId())));
        }

        inventory.setItem(SIZE - 9, new ItemApi(Material.CLOCK, ChatColor.YELLOW + "Aktualisieren",
                        List.of(ChatColor.GRAY + "Kalender neu vom Host laden")).build(),
                new SimpleItemAction((clickEvent) -> open((Player) clickEvent.getWhoClicked())));
        inventory.setItem(SIZE - 5, new ItemApi(Material.BOOK, ChatColor.AQUA + "Nächste 27 Tage",
                List.of(ChatColor.GRAY + "Events gesamt: " + ChatColor.WHITE + upcoming.size(),
                        ChatColor.GRAY + "Heute: " + ChatColor.WHITE + EventCalendar.getEventsOn(today).size())).build(),
                SimpleItemAction.display());
        if (isAdmin(player)) {
            inventory.setItem(SIZE - 1, new ItemApi(Material.NETHER_STAR, ChatColor.GREEN + "Neues Event",
                            List.of(ChatColor.GRAY + "Art wählen, einstellen und Tage anklicken")).build(),
                    new SimpleItemAction((clickEvent) -> openTypeMenu((Player) clickEvent.getWhoClicked())));
        }
        return inventory;
    }

    /* ------------------------------------------------------------------------------ day */

    /**
     * Shows what happens on one day.
     *
     * @param player the player
     * @param day    the day that was clicked
     */
    public static void openDay(Player player, LocalDate day) {
        List<ScheduledEvent> events = EventCalendar.getEventsOn(day);
        CustomInventory inventory = new CustomInventory(SIZE, day.format(LONG_FORMAT), null);
        for (int i = 0; i < events.size() && i < SIZE - 9; i++) {
            ScheduledEvent event = events.get(i);
            inventory.setItem(i, eventItem(event, player), new SimpleItemAction((clickEvent) ->
                    openEvent((Player) clickEvent.getWhoClicked(), event.getId())));
        }
        if (events.isEmpty()) {
            inventory.setItem(22, new ItemApi(Material.BARRIER, ChatColor.GRAY + "An diesem Tag ist nichts geplant").build(),
                    SimpleItemAction.display());
        }
        for (int i = SIZE - 9; i < SIZE; i++) inventory.setPlaceHolder(i);
        inventory.setItem(SIZE - 9, new ItemApi(Material.ARROW, ChatColor.GRAY + "Zurück zum Kalender").build(),
                new SimpleItemAction((clickEvent) -> openCalendar((Player) clickEvent.getWhoClicked(), false)));
        if (isAdmin(player)) {
            inventory.setItem(SIZE - 1, new ItemApi(Material.NETHER_STAR, ChatColor.GREEN + "Event an diesem Tag",
                            List.of(ChatColor.GRAY + "Neues Event, dieser Tag ist schon gewählt")).build(),
                    new SimpleItemAction((clickEvent) -> openTypeMenu((Player) clickEvent.getWhoClicked(), day)));
        }
        player.openInventory(inventory.getInventory());
    }

    /* ---------------------------------------------------------------------------- event */

    /**
     * Shows one event with its teams and its ranking.
     *
     * @param player  the player
     * @param eventId the event
     */
    public static void openEvent(Player player, UUID eventId) {
        ScheduledEvent event = EventCalendar.getEvent(eventId);
        if (event == null) {
            player.sendMessage(ChatColor.RED + "Dieses Event gibt es nicht mehr.");
            openCalendar(player, false);
            return;
        }
        CustomInventory inventory = new CustomInventory(SIZE, "Event: " + shorten(event.getName()), null);
        for (int i = 0; i < SIZE; i++) inventory.setPlaceHolder(i);
        inventory.setItem(4, eventItem(event, player), SimpleItemAction.display());

        // the teams and, if the event has one, the leaderboard
        Ranking ranking = event.getRanking();
        int slot = 18;
        for (Ranking.Entry entry : ranking.getEntries()) {
            if (slot > 26) break;
            EventTeam team = entry.getTeam();
            List<String> lore = new ArrayList<>();
            if (ranking.isRanked()) lore.add(ChatColor.GOLD + "Platz " + entry.getPlace());
            lore.add(ChatColor.GRAY + "Wertung: " + ChatColor.WHITE + entry.getScore());
            lore.add(ChatColor.GRAY + "Spieler: " + ChatColor.WHITE + team.getSize()
                    + (event.getDefinition().getMaxTeamSize() > 0 ? "/" + event.getDefinition().getMaxTeamSize() : ""));
            for (UUID member : team.getMembers()) {
                OfflinePlayer offline = Bukkit.getOfflinePlayer(member);
                lore.add(ChatColor.DARK_GRAY + " - " + (offline.getName() == null ? member.toString() : offline.getName()));
            }
            lore.add(" ");
            if (event.getDefinition().allowsPlayerSignup()) {
                lore.add(team.hasMember(player.getUniqueId())
                        ? ChatColor.YELLOW + "Klicken um das Team zu verlassen"
                        : ChatColor.YELLOW + "Klicken um beizutreten");
            }
            inventory.setItem(slot++, new ItemApi(material(team.getColor().getMaterialName(), Material.WHITE_WOOL),
                    team.getColor().getColorCode() + team.getName(), lore).build(), new SimpleItemAction((clickEvent) -> {
                Player clicker = (Player) clickEvent.getWhoClicked();
                if (!event.getDefinition().allowsPlayerSignup()) return;
                boolean leaving = team.hasMember(clicker.getUniqueId());
                PaperContext.async(() -> {
                    try {
                        if (leaving) {
                            EventApi.leave(event.getId(), clicker.getUniqueId());
                        } else {
                            EventApi.join(event.getId(), team.getId(), clicker.getUniqueId());
                        }
                        EventCalendar.refresh(); // read back what the host made of it
                    } catch (Exception e) {
                        PaperContext.sync(() -> clicker.sendMessage(ChatColor.RED + "Das hat nicht geklappt."));
                        return;
                    }
                    PaperContext.sync(() -> {
                        clicker.sendMessage(leaving
                                ? ChatColor.GRAY + "Du hast " + team.getDisplayName() + ChatColor.GRAY + " verlassen."
                                : ChatColor.GRAY + "Du bist jetzt in " + team.getDisplayName() + ChatColor.GRAY + ".");
                        openEvent(clicker, event.getId());
                    });
                });
            }));
        }

        inventory.setItem(SIZE - 9, new ItemApi(Material.ARROW, ChatColor.GRAY + "Zurück zum Kalender").build(),
                new SimpleItemAction((clickEvent) -> openCalendar((Player) clickEvent.getWhoClicked(), false)));
        inventory.setItem(SIZE - 6, new ItemApi(Material.ENDER_PEARL, ChatColor.LIGHT_PURPLE + "Zum Event Server",
                        List.of(ChatColor.GRAY + "Server: " + ChatColor.WHITE + event.getServerName())).build(),
                new SimpleItemAction((clickEvent) -> {
                    Player clicker = (Player) clickEvent.getWhoClicked();
                    clicker.closeInventory();
                    ServerConnector.connect(clicker, event.getServerName());
                }));
        if (isAdmin(player)) {
            inventory.setItem(SIZE - 4, new ItemApi(Material.WRITABLE_BOOK, ChatColor.YELLOW + "Bearbeiten",
                            List.of(ChatColor.GRAY + "Name, Teams, Wertung und Tage ändern")).build(),
                    new SimpleItemAction((clickEvent) -> {
                        Player clicker = (Player) clickEvent.getWhoClicked();
                        EventDraft.edit(clicker, event);
                        openDraft(clicker);
                    }));
            inventory.setItem(SIZE - 3, new ItemApi(Material.LIME_DYE, ChatColor.GREEN + "Server jetzt starten",
                            List.of(ChatColor.GRAY + "Startet " + event.getServerName() + " mit den Event Plugins")).build(),
                    new SimpleItemAction((clickEvent) -> {
                        Player clicker = (Player) clickEvent.getWhoClicked();
                        clicker.closeInventory();
                        PaperContext.async(() -> {
                            try {
                                EventApi.startServer(event);
                            } catch (Exception e) {
                                PaperContext.sync(() -> clicker.sendMessage(ChatColor.RED + "Konnte den Server nicht starten: " + e.getMessage()));
                                return;
                            }
                            PaperContext.sync(() -> clicker.sendMessage(ChatColor.GREEN + event.getServerName() + " wird gestartet."));
                        });
                    }));
            inventory.setItem(SIZE - 1, new ItemApi(Material.RED_WOOL, ChatColor.RED + "Event absagen",
                            List.of(ChatColor.GRAY + "Nimmt das Event aus dem Kalender")).build(),
                    new SimpleItemAction((clickEvent) -> {
                        Player clicker = (Player) clickEvent.getWhoClicked();
                        clicker.closeInventory();
                        PaperContext.async(() -> {
                            try {
                                EventApi.cancel(event.getId());
                            } catch (Exception e) {
                                PaperContext.sync(() -> clicker.sendMessage(ChatColor.RED + "Das hat nicht geklappt."));
                                return;
                            }
                            PaperContext.sync(() -> clicker.sendMessage(ChatColor.YELLOW + "'" + event.getName() + "' wurde abgesagt."));
                        });
                    }));
        }
        player.openInventory(inventory.getInventory());
    }

    /* ------------------------------------------------------------------ creating events */

    /**
     * Step one for admins: which kind of event should it be.
     *
     * @param player the admin
     */
    public static void openTypeMenu(Player player) {
        openTypeMenu(player, null);
    }

    /**
     * Step one for admins, with a day that is already picked.
     *
     * @param player the admin
     * @param day    a day the new event takes place on, may be {@code null}
     */
    public static void openTypeMenu(Player player, LocalDate day) {
        if (!requireAdmin(player)) return;
        CustomInventory inventory = new CustomInventory(27, "Event Art wählen", null);
        List<EventDefinition> definitions = EventRegistry.all();
        for (int i = 0; i < definitions.size() && i < 18; i++) {
            EventDefinition definition = definitions.get(i);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + definition.getDescription());
            lore.add(" ");
            lore.add(ChatColor.GRAY + "Plugin: " + ChatColor.WHITE
                    + (definition.getPlugin() == null ? "keins" : definition.getPlugin().getDisplayName()));
            lore.add(ChatColor.GRAY + "Teams: " + ChatColor.WHITE + definition.getMinTeams() + "-" + definition.getMaxTeams());
            lore.add(ChatColor.GRAY + "Wertung: " + ChatColor.WHITE + definition.getDefaultRanking().getDisplayName());
            lore.add(ChatColor.GRAY + "Server: " + ChatColor.WHITE + definition.getServerTemplate().getDisplayName());
            inventory.setItem(i, new ItemApi(material(definition.getIconMaterial(), Material.PAPER),
                    ChatColor.AQUA + definition.getDisplayName(), lore).build(), new SimpleItemAction((clickEvent) -> {
                Player clicker = (Player) clickEvent.getWhoClicked();
                EventDraft draft = EventDraft.start(clicker, definition, definition.getDisplayName());
                if (day != null) draft.toggleDay(day);
                openDraft(clicker);
            }));
        }
        inventory.setItem(26, new ItemApi(Material.ARROW, ChatColor.GRAY + "Zurück").build(),
                new SimpleItemAction((clickEvent) -> openCalendar((Player) clickEvent.getWhoClicked(), false)));
        player.openInventory(inventory.getInventory());
    }

    /**
     * Step two for admins: everything about the event, including the days it takes place on.
     *
     * @param player the admin
     */
    public static void openDraft(Player player) {
        if (!requireAdmin(player)) return;
        EventDraft draft = EventDraft.of(player);
        if (draft == null) {
            openTypeMenu(player);
            return;
        }
        ScheduledEvent event = draft.getEvent();
        EventDefinition definition = event.getDefinition();
        CustomInventory inventory = new CustomInventory(SIZE, draft.isEditing() ? "Event bearbeiten" : "Event erstellen", null);
        for (int i = 0; i < SIZE; i++) inventory.setPlaceHolder(i);

        inventory.setItem(10, new ItemApi(Material.NAME_TAG, ChatColor.AQUA + "Name: " + ChatColor.WHITE + event.getName(),
                        List.of(ChatColor.GRAY + "Server: " + ChatColor.WHITE + event.getServerName(),
                                ChatColor.YELLOW + "Klicken zum Umbenennen")).build(),
                new SimpleItemAction((clickEvent) -> {
                    Player clicker = (Player) clickEvent.getWhoClicked();
                    ChatPrompt.ask(clicker, "Wie soll das Event heißen?", (answer) -> {
                        EventDraft current = EventDraft.of(clicker);
                        if (current == null) return;
                        current.getEvent().setName(answer);
                        openDraft(clicker);
                    });
                }));

        inventory.setItem(11, new ItemApi(material(definition.getIconMaterial(), Material.PAPER),
                        ChatColor.AQUA + "Art: " + ChatColor.WHITE + definition.getDisplayName(),
                        List.of(ChatColor.GRAY + definition.getDescription(),
                                ChatColor.GRAY + "Plugin: " + ChatColor.WHITE
                                        + (definition.getPlugin() == null ? "keins" : definition.getPlugin().getDisplayName()),
                                ChatColor.YELLOW + "Klicken für eine andere Art")).build(),
                new SimpleItemAction((clickEvent) -> openTypeMenu((Player) clickEvent.getWhoClicked())));

        List<String> teamLore = new ArrayList<>();
        for (EventTeam team : event.getTeams()) {
            teamLore.add(ChatColor.DARK_GRAY + " - " + team.getDisplayName()
                    + ChatColor.DARK_GRAY + " (" + team.getSize() + " Spieler)");
        }
        teamLore.add(" ");
        teamLore.add(ChatColor.YELLOW + "Linksklick: " + ChatColor.GRAY + "ein Team mehr");
        teamLore.add(ChatColor.YELLOW + "Rechtsklick: " + ChatColor.GRAY + "ein Team weniger");
        teamLore.add(ChatColor.DARK_GRAY + "erlaubt: " + definition.getMinTeams() + "-" + definition.getMaxTeams());
        inventory.setItem(12, new ItemApi(Material.SHIELD, ChatColor.AQUA + "Teams: " + ChatColor.WHITE + event.getTeams().size(),
                teamLore).build(), new SimpleItemAction((clickEvent) -> {
            Player clicker = (Player) clickEvent.getWhoClicked();
            EventDraft current = EventDraft.of(clicker);
            if (current == null) return;
            current.getEvent().setTeamCount(current.getEvent().getTeams().size() + (clickEvent.isRightClick() ? -1 : 1));
            openDraft(clicker);
        }));

        RankingStrategy ranking = event.getRankingStrategy();
        inventory.setItem(13, new ItemApi(Material.DIAMOND, ChatColor.AQUA + "Wertung: " + ChatColor.WHITE + ranking.getDisplayName(),
                List.of(ChatColor.GRAY + ranking.getDescription(),
                        ChatColor.GRAY + (ranking.isRanked() ? "Es gibt eine Rangliste" : "Es gibt keine Rangliste"),
                        ChatColor.YELLOW + "Klicken für die nächste Wertung")).build(),
                new SimpleItemAction((clickEvent) -> {
                    Player clicker = (Player) clickEvent.getWhoClicked();
                    EventDraft current = EventDraft.of(clicker);
                    if (current == null) return;
                    current.getEvent().setRanking(RankingStrategies.next(current.getEvent().getRankingStrategy()));
                    openDraft(clicker);
                }));

        inventory.setItem(14, new ItemApi(Material.REDSTONE_BLOCK, ChatColor.AQUA + "RAM: " + ChatColor.WHITE + event.getMemoryMB() + " MB",
                List.of(ChatColor.YELLOW + "Linksklick: " + ChatColor.GRAY + "+512 MB",
                        ChatColor.YELLOW + "Rechtsklick: " + ChatColor.GRAY + "-512 MB")).build(),
                new SimpleItemAction((clickEvent) -> {
                    Player clicker = (Player) clickEvent.getWhoClicked();
                    EventDraft current = EventDraft.of(clicker);
                    if (current == null) return;
                    int memory = current.getEvent().getMemoryMB() + (clickEvent.isRightClick() ? -512 : 512);
                    current.getEvent().setMemoryMB(Math.max(512, Math.min(16384, memory)));
                    openDraft(clicker);
                }));

        List<String> pluginLore = new ArrayList<>();
        for (FileType.PLUGIN plugin : event.getPlugins()) {
            pluginLore.add(ChatColor.GREEN + " + " + plugin.getDisplayName());
        }
        if (event.getPlugins().isEmpty()) pluginLore.add(ChatColor.DARK_GRAY + "nur die Standardplugins");
        pluginLore.add(" ");
        pluginLore.add(ChatColor.YELLOW + "Klicken um Plugins zu wählen");
        inventory.setItem(15, new ItemApi(Material.CHEST, ChatColor.AQUA + "Plugins: " + ChatColor.WHITE + event.getPlugins().size(),
                pluginLore).build(), new SimpleItemAction((clickEvent) -> openPluginMenu((Player) clickEvent.getWhoClicked())));

        inventory.setItem(16, new ItemApi(event.isAutoStartServer() ? Material.LEVER : Material.STICK,
                ChatColor.AQUA + "Server automatisch starten: "
                        + (event.isAutoStartServer() ? ChatColor.GREEN + "an" : ChatColor.RED + "aus"),
                List.of(ChatColor.GRAY + "Der Host startet den Server am Event Tag",
                        ChatColor.YELLOW + "Klicken zum Umschalten")).build(),
                new SimpleItemAction((clickEvent) -> {
                    Player clicker = (Player) clickEvent.getWhoClicked();
                    EventDraft current = EventDraft.of(clicker);
                    if (current == null) return;
                    current.getEvent().setAutoStartServer(!current.getEvent().isAutoStartServer());
                    openDraft(clicker);
                }));

        List<String> dayLore = new ArrayList<>();
        for (LocalDate day : event.getDays()) dayLore.add(ChatColor.GREEN + " - " + day.format(DAY_FORMAT));
        if (event.getDays().isEmpty()) dayLore.add(ChatColor.RED + "Noch kein Tag gewählt");
        dayLore.add(" ");
        dayLore.add(ChatColor.YELLOW + "Klicken und dann die Tage anklicken");
        inventory.setItem(29, new ItemApi(Material.CLOCK, ChatColor.AQUA + "Tage: " + ChatColor.WHITE + event.getDays().size(),
                dayLore).build(), new SimpleItemAction((clickEvent) -> openCalendar((Player) clickEvent.getWhoClicked(), true)));

        inventory.setItem(33, new ItemApi(event.getDays().isEmpty() ? Material.BARRIER : Material.EMERALD_BLOCK,
                (event.getDays().isEmpty() ? ChatColor.RED : ChatColor.GREEN) + "Event speichern",
                List.of(event.getDays().isEmpty()
                        ? ChatColor.RED + "Wähle zuerst mindestens einen Tag"
                        : ChatColor.GRAY + "Für alle Server sichtbar")).build(),
                new SimpleItemAction((clickEvent) -> save((Player) clickEvent.getWhoClicked())));

        inventory.setItem(SIZE - 9, new ItemApi(Material.ARROW, ChatColor.GRAY + "Abbrechen").build(),
                new SimpleItemAction((clickEvent) -> {
                    Player clicker = (Player) clickEvent.getWhoClicked();
                    EventDraft.clear(clicker);
                    openCalendar(clicker, false);
                }));
        player.openInventory(inventory.getInventory());
    }

    /**
     * Lets the admin pick the plugins the event server gets on top of its template.
     *
     * @param player the admin
     */
    public static void openPluginMenu(Player player) {
        if (!requireAdmin(player)) return;
        EventDraft draft = EventDraft.of(player);
        if (draft == null) {
            openTypeMenu(player);
            return;
        }
        ScheduledEvent event = draft.getEvent();
        FileType.SERVER software = event.getTemplate().getSoftware();
        List<FileType.PLUGIN> selectable = FileType.PLUGIN.selectableFor(software);
        List<FileType.PLUGIN> required = event.getDefinition().getAllPlugins();
        CustomInventory inventory = new CustomInventory(SIZE, "Event Plugins", null);
        for (int i = 0; i < selectable.size() && i < SIZE - 9; i++) {
            FileType.PLUGIN plugin = selectable.get(i);
            boolean isRequired = required.contains(plugin);
            boolean selected = isRequired || event.getPlugins().contains(plugin);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + plugin.getDescription());
            lore.add(" ");
            if (isRequired) {
                lore.add(ChatColor.GOLD + "Gehört zu " + event.getDefinition().getDisplayName());
            } else if (selected) {
                lore.add(ChatColor.GREEN + "Ausgewählt");
                lore.add(ChatColor.YELLOW + "Klicken zum Entfernen");
            } else {
                lore.add(ChatColor.RED + "Nicht ausgewählt");
                lore.add(ChatColor.YELLOW + "Klicken zum Hinzufügen");
            }
            inventory.setItem(i, new ItemApi(isRequired ? Material.GOLDEN_APPLE : (selected ? Material.LIME_DYE : Material.GRAY_DYE),
                    (selected ? ChatColor.GREEN : ChatColor.RED) + plugin.getDisplayName(), lore).build(),
                    new SimpleItemAction((clickEvent) -> {
                        Player clicker = (Player) clickEvent.getWhoClicked();
                        EventDraft current = EventDraft.of(clicker);
                        if (current == null || isRequired) return;
                        List<FileType.PLUGIN> plugins = new ArrayList<>(current.getEvent().getPlugins());
                        if (!plugins.remove(plugin)) plugins.add(plugin);
                        current.getEvent().setPlugins(plugins);
                        openPluginMenu(clicker);
                    }));
        }
        for (int i = SIZE - 9; i < SIZE; i++) inventory.setPlaceHolder(i);
        inventory.setItem(SIZE - 9, new ItemApi(Material.ARROW, ChatColor.GRAY + "Zurück").build(),
                new SimpleItemAction((clickEvent) -> openDraft((Player) clickEvent.getWhoClicked())));
        player.openInventory(inventory.getInventory());
    }

    private static void save(Player player) {
        EventDraft draft = EventDraft.of(player);
        if (draft == null) return;
        if (!draft.isComplete()) {
            player.sendMessage(ChatColor.RED + "Das Event braucht mindestens einen Tag.");
            return;
        }
        ScheduledEvent event = draft.getEvent();
        EventDraft.clear(player);
        player.closeInventory();
        PaperContext.async(() -> {
            try {
                EventApi.schedule(event);
            } catch (Exception e) {
                PaperContext.sync(() -> player.sendMessage(ChatColor.RED + "Konnte das Event nicht speichern: " + e.getMessage()));
                return;
            }
            PaperContext.sync(() -> player.sendMessage(ChatColor.GREEN + "'" + event.getName() + "' steht jetzt an "
                    + event.getDays().size() + " Tag(en) im Kalender."));
        });
    }

    /* --------------------------------------------------------------------------- helpers */

    private static org.bukkit.inventory.ItemStack eventItem(ScheduledEvent event, Player player) {
        EventDefinition definition = event.getDefinition();
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + definition.getDisplayName() + ChatColor.DARK_GRAY + " - " + event.getDescription());
        lore.add(" ");
        StringBuilder days = new StringBuilder();
        for (LocalDate day : event.getDays()) {
            if (days.length() > 0) days.append(", ");
            days.append(day.format(DAY_FORMAT));
        }
        lore.add(ChatColor.GRAY + "Tage: " + ChatColor.WHITE + days);
        lore.add(ChatColor.GRAY + "Teams: " + ChatColor.WHITE + event.getTeams().size()
                + ChatColor.GRAY + ", Spieler: " + ChatColor.WHITE + event.getParticipants().size());
        Ranking ranking = event.getRanking();
        if (ranking.isRanked()) {
            lore.add(ChatColor.GRAY + "Wertung: " + ChatColor.WHITE + ranking.getStrategy().getDisplayName());
            for (Ranking.Entry entry : ranking.getEntries()) {
                if (entry.getPlace() > 3) break;
                lore.add(ChatColor.DARK_GRAY + "  " + entry.getPlace() + ". " + entry.getTeam().getDisplayName()
                        + ChatColor.DARK_GRAY + " - " + entry.getScore());
            }
        } else {
            lore.add(ChatColor.GRAY + "Wertung: " + ChatColor.WHITE + "keine Rangliste");
        }
        EventTeam own = event.getTeamOf(player.getUniqueId());
        if (own != null) lore.add(ChatColor.GREEN + "Du spielst in " + own.getDisplayName());
        if (!event.isDefinitionKnown()) {
            lore.add(ChatColor.DARK_GRAY + "(Plugin hier nicht installiert)");
        }
        lore.add(" ");
        lore.add(ChatColor.YELLOW + "Klicken für Details");
        return new ItemApi(material(definition.getIconMaterial(), Material.PAPER),
                ChatColor.AQUA + event.getName(), lore).build();
    }

    private static Material material(String name, Material fallback) {
        Material material = Material.matchMaterial(name);
        return material == null ? fallback : material;
    }

    private static String shorten(String text) {
        return text.length() <= 20 ? text : text.substring(0, 20);
    }

    private static boolean isAdmin(Player player) {
        return player.isOp() || player.hasPermission("mcserver.events.manage");
    }

    private static boolean requireAdmin(Player player) {
        if (isAdmin(player)) return true;
        player.sendMessage(ChatColor.RED + "Nur Admins können Events planen.");
        return false;
    }

    /**
     * Writes the events of the next days into the chat, for players that just want a quick look.
     *
     * @param player the player
     */
    public static void printUpcoming(Player player) {
        List<ScheduledEvent> events = EventCalendar.getEvents();
        if (events.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "In den nächsten " + EventCalendar.DAYS + " Tagen ist nichts geplant.");
            return;
        }
        player.sendMessage(ChatColor.AQUA + "Events der nächsten " + EventCalendar.DAYS + " Tage:");
        for (ScheduledEvent event : events) {
            LocalDate next = event.getNextDay();
            if (next == null || !EventCalendar.isInWindow(next)) continue;
            player.sendMessage(ChatColor.GRAY + " - " + ChatColor.WHITE + next.format(DAY_FORMAT) + ChatColor.GRAY + ": "
                    + ChatColor.AQUA + event.getName() + ChatColor.DARK_GRAY + " (" + event.getDefinition().getDisplayName() + ")");
        }
    }

}

package de.hems.paper.servermanager;

import de.hems.api.ItemApi;
import de.hems.api.ServerApi;
import de.hems.paper.PaperContext;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.hems.types.server.CapacityData;
import de.hems.types.server.MemoryAdviceData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * What the machine has left, and what to do when it has nothing left.
 * <p>
 * This is the other half of refusing a start: telling somebody it did not work is easy, telling them why
 * and what would fix it is the part that gets the memory back. The numbers here are measured - the peak is
 * the most a server has actually held - so "take a gigabyte off survival" is a statement and not a hunch.
 */
public final class CapacityUi {

    private static final int SIZE = 9 * 5;
    /** Where the recommendations start. */
    private static final int ADVICE_START = 19;
    private static final SimpleDateFormat WHEN = new SimpleDateFormat("dd.MM. HH:mm");

    private CapacityUi() {
    }

    /**
     * Loads the numbers from the host and shows them.
     *
     * @param player the admin
     */
    public static void open(Player player) {
        player.sendMessage(ChatColor.GRAY + "Frage den Host nach dem Arbeitsspeicher ...");
        PaperContext.async(() -> {
            CapacityData capacity;
            try {
                capacity = ServerApi.capacity();
            } catch (Exception e) {
                PaperContext.sync(() -> player.sendMessage(ChatColor.RED + "Der Host antwortet gerade nicht."));
                return;
            }
            if (capacity == null) {
                PaperContext.sync(() -> player.sendMessage(ChatColor.RED + "Der Host antwortet gerade nicht."));
                return;
            }
            PaperContext.sync(() -> CustomInventory.show(player, build(capacity)));
        });
    }

    private static CustomInventory build(CapacityData capacity) {
        CustomInventory inventory = new CustomInventory(SIZE, "Arbeitsspeicher", null);
        inventory.fillPlaceHolder();
        inventory.setItem(4, overview(capacity), new SimpleItemAction(event -> {
        }));
        inventory.setItem(11, refusals(capacity), new SimpleItemAction(event -> {
        }));
        inventory.setItem(15, advice(capacity), new SimpleItemAction(event -> {
        }));

        List<MemoryAdviceData> entries = capacity.getAdvice();
        for (int i = 0; i < entries.size() && ADVICE_START + i < SIZE - 9; i++) {
            inventory.setItem(ADVICE_START + i, adviceIcon(entries.get(i)), new SimpleItemAction(event -> {
            }));
        }
        inventory.setItem(SIZE - 9, new ItemApi(Material.CLOCK, ChatColor.YELLOW + "Aktualisieren").build(),
                new SimpleItemAction(event -> open((Player) event.getWhoClicked())));
        inventory.setItem(SIZE - 1, new ItemApi(Material.BARRIER, ChatColor.GRAY + "Zurück").build(),
                new SimpleItemAction(event -> ServerManagerUi.openServerList((Player) event.getWhoClicked())));
        return inventory;
    }

    /**
     * The budget itself: what the machine has, what is promised away, what is left.
     */
    private static org.bukkit.inventory.ItemStack overview(CapacityData capacity) {
        List<String> lore = new ArrayList<>();
        if (capacity.getTotalMachineMB() > 0) {
            lore.add(ChatColor.GRAY + "Maschine: " + ChatColor.WHITE + capacity.getTotalMachineMB() + " MB");
            lore.add(ChatColor.GRAY + "Reserve fürs System: " + ChatColor.WHITE + capacity.getReserveMB() + " MB");
        } else {
            lore.add(ChatColor.DARK_GRAY + "Die Größe der Maschine ist nicht lesbar.");
        }
        if (capacity.getBudgetMB() > 0) {
            lore.add(ChatColor.GRAY + "Budget für Server: " + ChatColor.WHITE + capacity.getBudgetMB() + " MB");
            lore.add(ChatColor.GRAY + "Vergeben: " + ChatColor.WHITE + capacity.getAllocatedMB() + " MB");
            lore.add(ChatColor.GRAY + "Frei: " + free(capacity) + capacity.getFreeMB() + " MB");
            lore.add(" ");
            lore.add(bar(capacity));
        } else {
            lore.add(ChatColor.GRAY + "Vergeben: " + ChatColor.WHITE + capacity.getAllocatedMB() + " MB");
            lore.add(ChatColor.DARK_GRAY + "Ohne lesbare Maschinengröße gibt es kein Budget,");
            lore.add(ChatColor.DARK_GRAY + "also wird auch kein Start abgelehnt.");
        }
        return new ItemApi(Material.REDSTONE_BLOCK, ChatColor.AQUA + "Speicher im Netzwerk", lore).build();
    }

    /**
     * @return the colour the free memory is written in, red once nothing more fits
     */
    private static ChatColor free(CapacityData capacity) {
        int budget = Math.max(1, capacity.getBudgetMB());
        int percent = capacity.getFreeMB() * 100 / budget;
        if (percent < 10) return ChatColor.RED;
        if (percent < 25) return ChatColor.YELLOW;
        return ChatColor.GREEN;
    }

    /**
     * @return the budget drawn as a bar, because a number in megabytes says little at a glance
     */
    private static String bar(CapacityData capacity) {
        int width = 20;
        int budget = Math.max(1, capacity.getBudgetMB());
        int filled = Math.min(width, capacity.getAllocatedMB() * width / budget);
        StringBuilder bar = new StringBuilder(ChatColor.DARK_GRAY + "[" + ChatColor.RED);
        for (int i = 0; i < width; i++) {
            if (i == filled) bar.append(ChatColor.GREEN);
            bar.append('|');
        }
        return bar.append(ChatColor.DARK_GRAY).append(']').toString();
    }

    /**
     * How often a start did not happen because there was no memory for it.
     */
    private static org.bukkit.inventory.ItemStack refusals(CapacityData capacity) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Letzte 7 Tage: " + ChatColor.WHITE + capacity.getRefusedRecently());
        lore.add(ChatColor.GRAY + "Insgesamt: " + ChatColor.WHITE + capacity.getRefusedTotal());
        if (capacity.getLastRefusedAt() > 0) {
            lore.add(ChatColor.GRAY + "Zuletzt: " + ChatColor.WHITE
                    + WHEN.format(new Date(capacity.getLastRefusedAt())));
        }
        lore.add(" ");
        if (capacity.getRefusedRecently() == 0) {
            lore.add(ChatColor.DARK_GRAY + "Bisher musste kein Start abgelehnt werden.");
        } else {
            lore.add(ChatColor.YELLOW + "So oft wollte jemand eine Runde starten");
            lore.add(ChatColor.YELLOW + "und der Speicher hat nicht gereicht.");
        }
        Material icon = capacity.getRefusedRecently() == 0 ? Material.LIME_DYE : Material.RED_DYE;
        return new ItemApi(icon, ChatColor.AQUA + "Abgelehnte Starts", lore).build();
    }

    /**
     * The summary above the individual suggestions.
     */
    private static org.bukkit.inventory.ItemStack advice(CapacityData capacity) {
        List<String> lore = new ArrayList<>();
        if (capacity.getAdvice().isEmpty()) {
            lore.add(ChatColor.DARK_GRAY + "Kein Server hält deutlich weniger,");
            lore.add(ChatColor.DARK_GRAY + "als er bekommen hat.");
            lore.add(" ");
            lore.add(ChatColor.DARK_GRAY + "Gemessen wird alle 30 Sekunden und nur");
            lore.add(ChatColor.DARK_GRAY + "unter Linux. Nach einem Neustart des");
            lore.add(ChatColor.DARK_GRAY + "Launchers dauert es also einen Moment.");
        } else {
            lore.add(ChatColor.GRAY + "Frei zu machen: " + ChatColor.WHITE + capacity.getFreeableMB() + " MB");
            lore.add(" ");
            lore.add(ChatColor.YELLOW + "Die Server unten halten dauerhaft viel");
            lore.add(ChatColor.YELLOW + "weniger, als ihnen zugewiesen wurde.");
            lore.add(ChatColor.YELLOW + "Weniger zuweisen macht Platz für Runden.");
            lore.add(" ");
            lore.add(ChatColor.DARK_GRAY + "Ändern über die Einstellungen des Servers");
            lore.add(ChatColor.DARK_GRAY + "oder memory.max-memory-mb im Launcher.");
        }
        return new ItemApi(Material.PAPER, ChatColor.AQUA + "Empfehlung", lore).build();
    }

    /**
     * One server that is sitting on memory it never uses.
     */
    private static org.bukkit.inventory.ItemStack adviceIcon(MemoryAdviceData entry) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Zugewiesen: " + ChatColor.WHITE + entry.getAllocatedMB() + " MB");
        lore.add(ChatColor.GRAY + "Höchster gemessener Verbrauch: " + ChatColor.WHITE
                + entry.getPeakUsedMB() + " MB");
        lore.add(ChatColor.GRAY + "Vorschlag: " + ChatColor.GREEN + entry.getSuggestedMB() + " MB");
        lore.add(" ");
        lore.add(ChatColor.YELLOW + "Das macht " + entry.getFreedMB() + " MB frei"
                + (entry.getExtraRounds() > 0 ? " - Platz für " + entry.getExtraRounds()
                + (entry.getExtraRounds() == 1 ? " weitere Runde." : " weitere Runden.") : "."));
        return new ItemApi(Material.FURNACE, ChatColor.AQUA + entry.getServer(), lore).build();
    }
}

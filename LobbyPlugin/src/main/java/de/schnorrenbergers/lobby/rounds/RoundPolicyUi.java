package de.schnorrenbergers.lobby.rounds;

import de.hems.api.ItemApi;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.hems.paper.round.RoundService;
import de.hems.paper.round.RoundStarter;
import de.hems.paper.servermanager.CapacityUi;
import de.hems.types.round.RoundPolicy;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The rules an admin sets for rounds players start themselves.
 * <p>
 * Every click writes straight through to the launcher, so a switch flipped here means the same thing on
 * every server a second later. There is no save button on purpose: a half applied rule set is worse than
 * either of the two it sits between.
 */
public final class RoundPolicyUi {

    private static final int SIZE = 9 * 4;

    private RoundPolicyUi() {
    }

    /**
     * @param player an operator
     */
    public static void open(Player player) {
        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "Dafür bist du nicht berechtigt.");
            return;
        }
        CustomInventory.show(player, build());
    }

    private static CustomInventory build() {
        RoundPolicy policy = RoundService.getPolicy();
        CustomInventory inventory = new CustomInventory(SIZE, "Runden - Einstellungen", null);
        inventory.fillPlaceHolder();

        inventory.setItem(10, toggle(policy.isSelfStartEnabled(), "Selbst starten",
                        policy.isSelfStartEnabled()
                                ? "Jeder darf eine eigene Runde aufmachen."
                                : "Nur Admins dürfen Runden starten."),
                new SimpleItemAction(event -> {
                    RoundPolicy updated = policy.copy();
                    updated.setSelfStartEnabled(!policy.isSelfStartEnabled());
                    apply(event.getWhoClicked(), updated);
                }));

        inventory.setItem(11, number(Material.PLAYER_HEAD, "Runden pro Spieler", policy.getMaxPerPlayer(),
                        "Wie viele Runden einer gleichzeitig offen haben darf."),
                new SimpleItemAction(event -> {
                    RoundPolicy updated = policy.copy();
                    updated.setMaxPerPlayer(policy.getMaxPerPlayer() >= 5 ? 1 : policy.getMaxPerPlayer() + 1);
                    apply(event.getWhoClicked(), updated);
                }));

        inventory.setItem(12, number(Material.HOPPER, "Runden insgesamt", policy.getMaxRounds(),
                        policy.getMaxRounds() == 0 ? "Kein Limit - nur der Speicher entscheidet."
                                : "So viele selbst gestartete Runden gleichzeitig."),
                new SimpleItemAction(event -> {
                    RoundPolicy updated = policy.copy();
                    updated.setMaxRounds(policy.getMaxRounds() >= 16 ? 0 : policy.getMaxRounds() + 2);
                    apply(event.getWhoClicked(), updated);
                }));

        inventory.setItem(13, number(Material.CLOCK, "Wartezeit", policy.getCooldownSeconds(),
                        "Sekunden, bis derselbe Spieler wieder starten darf."),
                new SimpleItemAction(event -> {
                    RoundPolicy updated = policy.copy();
                    updated.setCooldownSeconds(policy.getCooldownSeconds() >= 900 ? 0
                            : policy.getCooldownSeconds() + 150);
                    apply(event.getWhoClicked(), updated);
                }));

        inventory.setItem(14, toggle(policy.isBlockWhileEventRunning(), "Während Events sperren",
                        policy.isBlockWhileEventRunning()
                                ? "Läuft ein Event, startet niemand privat."
                                : "Events blockieren nichts."),
                new SimpleItemAction(event -> {
                    RoundPolicy updated = policy.copy();
                    updated.setBlockWhileEventRunning(!policy.isBlockWhileEventRunning());
                    apply(event.getWhoClicked(), updated);
                }));

        inventory.setItem(15, number(Material.COMPASS, "Vorlauf vor Events",
                        policy.getBlockBeforeEventMinutes(),
                        policy.getBlockBeforeEventMinutes() == 0
                                ? "Kein Vorlauf." : "Minuten vor einem Event ist Schluss."),
                new SimpleItemAction(event -> {
                    RoundPolicy updated = policy.copy();
                    updated.setBlockBeforeEventMinutes(policy.getBlockBeforeEventMinutes() >= 60 ? 0
                            : policy.getBlockBeforeEventMinutes() + 15);
                    apply(event.getWhoClicked(), updated);
                }));

        inventory.setItem(16, memoryIcon(policy), new SimpleItemAction(event -> {
            RoundPolicy updated = policy.copy();
            int current = policy.getMemoryMB();
            updated.setMemoryMB(current == 0 ? 1024 : (current >= 4096 ? 0 : current + 512));
            apply(event.getWhoClicked(), updated);
        }));

        inventory.setItem(SIZE - 5, new ItemApi(Material.REDSTONE_BLOCK, ChatColor.AQUA + "Arbeitsspeicher",
                        List.of(ChatColor.GRAY + "Wie voll die Maschine ist und",
                                ChatColor.GRAY + "wie oft schon abgelehnt wurde")).build(),
                new SimpleItemAction(event -> CapacityUi.open((Player) event.getWhoClicked())));
        inventory.setItem(SIZE - 9, new ItemApi(Material.BARRIER, ChatColor.GRAY + "Zurück").build(),
                new SimpleItemAction(event -> RoundBrowserUi.open((Player) event.getWhoClicked())));
        return inventory;
    }

    private static void apply(org.bukkit.entity.HumanEntity clicker, RoundPolicy updated) {
        RoundService.savePolicyAsync(updated);
        if (clicker instanceof Player player) open(player);
    }

    private static ItemStack toggle(boolean on, String title, String explanation) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + explanation);
        lore.add(" ");
        lore.add(on ? ChatColor.GREEN + "An" : ChatColor.RED + "Aus");
        lore.add(ChatColor.YELLOW + "Klicken zum Umschalten");
        return new ItemApi(on ? Material.LIME_DYE : Material.RED_DYE,
                (on ? ChatColor.GREEN : ChatColor.GRAY) + title, lore).build();
    }

    private static ItemStack number(Material icon, String title, int value, String explanation) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + explanation);
        lore.add(" ");
        lore.add(ChatColor.WHITE + "Aktuell: " + value);
        lore.add(ChatColor.YELLOW + "Klicken für den nächsten Wert");
        return new ItemApi(icon, ChatColor.AQUA + title, lore).build();
    }

    private static ItemStack memoryIcon(RoundPolicy policy) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Wie viel Speicher eine Runde bekommt.");
        lore.add(" ");
        lore.add(ChatColor.WHITE + "Aktuell: " + RoundStarter.memoryOf(policy) + " MB"
                + (policy.getMemoryMB() == 0 ? ChatColor.DARK_GRAY + " (Vorlage)" : ""));
        lore.add(ChatColor.YELLOW + "Klicken für den nächsten Wert");
        return new ItemApi(Material.FURNACE, ChatColor.AQUA + "Speicher pro Runde", lore).build();
    }
}

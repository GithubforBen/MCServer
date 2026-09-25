package de.hems.paper.lotto;

import de.hems.communication.events.lotto.LottoRequestEvent;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.hems.paper.money.MoneyService;
import de.hems.types.lotto.LottoDraw;
import de.hems.types.lotto.LottoStatus;
import de.hems.types.lotto.LottoTicket;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The lotto slip: fifteen numbers to pick four from, the pot, and the buttons to buy.
 * <p>
 * Each number is a stack of that many items, so the number is readable at a glance without a texture.
 * What a player has picked is kept until they buy or clear it - closing the menu does not lose it.
 */
public final class LottoMenu {

    /** Where the numbers go: three rows of five in the middle. */
    private static final int[] NUMBER_SLOTS = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33};
    /** How many quick tips the button buys. */
    private static final int QUICK_TIPS = 5;

    private static final Map<UUID, TreeSet<Integer>> picked = new ConcurrentHashMap<>();
    private static final SecureRandom random = new SecureRandom();

    private LottoMenu() {
    }

    public static void open(Player player) {
        CustomInventory.show(player, build(player));
    }

    private static TreeSet<Integer> slip(Player player) {
        return picked.computeIfAbsent(player.getUniqueId(), id -> new TreeSet<>());
    }

    static CustomInventory build(Player player) {
        LottoStatus status = LottoClient.getStatus();
        TreeSet<Integer> slip = slip(player);
        CustomInventory menu = new CustomInventory(54, "Lotto · 4 aus 15", event -> {
        });
        menu.fillPlaceHolder();
        menu.setItem(4, info(player, status), SimpleItemAction.display());

        for (int number = 1; number <= LottoTicket.HIGHEST; number++) {
            int value = number;
            boolean chosen = slip.contains(number);
            ItemStack item = item(chosen ? Material.LIME_CONCRETE : Material.PAPER,
                    Component.text("Zahl " + number, chosen ? NamedTextColor.GREEN : NamedTextColor.WHITE),
                    List.of(Component.text(chosen ? "Klicken zum Abwählen" : "Klicken zum Wählen", NamedTextColor.GRAY)));
            item.setAmount(number);
            if (chosen) glint(item);
            menu.setItem(NUMBER_SLOTS[number - 1], item, SimpleItemAction.opens(event -> {
                if (!slip.remove(value)) {
                    if (slip.size() >= LottoTicket.NUMBERS) {
                        player.sendMessage(Component.text("Du hast schon " + LottoTicket.NUMBERS
                                + " Zahlen. Wähl erst eine ab.", NamedTextColor.RED));
                        return;
                    }
                    slip.add(value);
                }
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
            }, () -> build(player)));
        }

        int price = status.getPrice();
        boolean full = slip.size() == LottoTicket.NUMBERS;
        menu.setItem(45, item(Material.BARRIER, Component.text("Schein leeren", NamedTextColor.RED), List.of()),
                SimpleItemAction.opens(event -> slip.clear(), () -> build(player)));
        menu.setItem(47, item(Material.ENDER_EYE, Component.text("Zufällig ausfüllen", NamedTextColor.AQUA),
                        List.of(Component.text("Füllt den Schein mit " + LottoTicket.NUMBERS + " zufälligen Zahlen",
                                NamedTextColor.GRAY))),
                SimpleItemAction.opens(event -> {
                    slip.clear();
                    for (int number : LottoTicket.random(random)) slip.add(number);
                }, () -> build(player)));
        menu.setItem(49, item(full ? Material.EMERALD : Material.GRAY_DYE,
                        Component.text(full ? "Tippen: " + LottoTicket.format(toArray(slip)) : "Tippen",
                                full ? NamedTextColor.GREEN : NamedTextColor.GRAY),
                        List.of(Component.text(full ? "Kostet " + price + " Bits" : "Wähl erst "
                                + (LottoTicket.NUMBERS - slip.size()) + " Zahl(en)", NamedTextColor.GRAY))),
                new SimpleItemAction(event -> {
                    if (!full) return;
                    buy(player, List.of(toArray(slip)), true);
                }));
        menu.setItem(51, item(Material.FIREWORK_ROCKET,
                        Component.text(QUICK_TIPS + " Quicktipps", NamedTextColor.YELLOW),
                        List.of(Component.text(QUICK_TIPS + " zufällige Tipps für " + (QUICK_TIPS * price) + " Bits",
                                NamedTextColor.GRAY))),
                new SimpleItemAction(event -> {
                    List<int[]> tips = new ArrayList<>();
                    for (int i = 0; i < QUICK_TIPS; i++) tips.add(LottoTicket.random(random));
                    buy(player, tips, false);
                }));
        menu.setItem(53, item(Material.BOOK, Component.text("Meine Tipps", NamedTextColor.AQUA),
                        List.of(Component.text("Deine Tipps in dieser Runde", NamedTextColor.GRAY))),
                new SimpleItemAction(event -> showMine(player)));
        return menu;
    }

    private static ItemStack info(Player player, LottoStatus status) {
        List<Component> lore = new ArrayList<>();
        if (!status.isKnown()) {
            lore.add(Component.text("Wird geladen ...", NamedTextColor.GRAY));
        } else {
            lore.add(Component.text("Topf: " + status.getPot() + " Bits", NamedTextColor.GOLD));
            lore.add(Component.text("Ziehung in " + LottoStatus.span(status.getNextDrawAt() - System.currentTimeMillis())
                    + " (" + status.getSchedule().toLowerCase() + ")", NamedTextColor.GRAY));
            lore.add(Component.text("Ein Tipp: " + status.getPrice() + " Bits · " + status.getTickets()
                    + " Tipps in dieser Runde", NamedTextColor.GRAY));
            lore.add(Component.text("Dein Guthaben: " + MoneyService.get(player.getUniqueId()) + " Bits", NamedTextColor.GRAY));
            LottoDraw last = status.getLastDraw();
            if (last != null) {
                lore.add(Component.empty());
                lore.add(Component.text("Letzte Zahlen: " + LottoTicket.format(last.getNumbers()), NamedTextColor.YELLOW));
                lore.add(Component.text(last.getWinners().isEmpty() ? "Kein Gewinner"
                        : "Gewinner: " + String.join(", ", last.getWinners().stream().distinct().toList()), NamedTextColor.GRAY));
            }
            lore.add(Component.empty());
            lore.add(Component.text("Wer alle 4 Zahlen trifft, bekommt den Topf.", NamedTextColor.DARK_GRAY));
            lore.add(Component.text("Trifft niemand, wächst er weiter.", NamedTextColor.DARK_GRAY));
        }
        return item(Material.GOLD_BLOCK, Component.text("Lotto · Runde " + Math.max(1, status.getRound()),
                NamedTextColor.GOLD), lore);
    }

    private static void buy(Player player, List<int[]> tips, boolean clearSlip) {
        player.closeInventory();
        LottoClient.ask(LottoClient.request(player, LottoRequestEvent.Action.BUY).withTips(tips), answer -> {
            if (answer.error() != null) {
                player.sendMessage(Component.text(answer.error(), NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            if (clearSlip) slip(player).clear();
            for (int[] tip : tips) {
                player.sendMessage(Component.text("✓ Getippt: ", NamedTextColor.GREEN)
                        .append(Component.text(LottoTicket.format(LottoTicket.normalize(tip)), NamedTextColor.YELLOW)));
            }
            player.sendMessage(Component.text("Du hast " + answer.tickets().size() + " Tipp(s) in dieser Runde. Viel Glück!",
                    NamedTextColor.GRAY));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        });
    }

    /**
     * Opens the list of the player's tips of this round.
     */
    public static void showMine(Player player) {
        LottoClient.ask(LottoClient.request(player, LottoRequestEvent.Action.MINE), answer -> {
            if (answer.error() != null) {
                player.sendMessage(Component.text(answer.error(), NamedTextColor.RED));
                return;
            }
            List<LottoTicket> tickets = answer.tickets();
            CustomInventory menu = new CustomInventory(54, "Lotto · Meine Tipps", event -> {
            });
            for (int i = 0; i < Math.min(45, tickets.size()); i++) {
                menu.setItem(i, item(Material.PAPER, Component.text(LottoTicket.format(tickets.get(i).getNumbers()),
                        NamedTextColor.YELLOW), List.of()), SimpleItemAction.display());
            }
            for (int i = 45; i < 54; i++) menu.setPlaceHolder(i);
            menu.setItem(49, item(Material.ARROW, Component.text("Zurück", NamedTextColor.WHITE),
                    List.of(Component.text(tickets.size() + " Tipps in dieser Runde"
                            + (tickets.size() > 45 ? ", die ersten 45 stehen hier" : ""), NamedTextColor.GRAY))),
                    SimpleItemAction.opens(() -> build(player)));
            player.openInventory(menu.getInventory());
        });
    }

    private static int[] toArray(TreeSet<Integer> slip) {
        return slip.stream().mapToInt(Integer::intValue).toArray();
    }

    private static ItemStack item(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        List<Component> lines = new ArrayList<>();
        for (Component line : lore) lines.add(line.decoration(TextDecoration.ITALIC, false));
        meta.lore(lines);
        item.setItemMeta(meta);
        return item;
    }

    private static void glint(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
    }
}

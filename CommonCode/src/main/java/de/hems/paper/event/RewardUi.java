package de.hems.paper.event;

import de.hems.api.ItemApi;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.hems.types.event.EventData;
import de.hems.types.event.EventRewards;
import de.hems.types.event.PrizeData;
import de.hems.types.event.RewardRule;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The rewards of an event: a list of them, and one panel per reward.
 * <p>
 * A reward is made in the order somebody thinks about it. First it is created, then it is filled - money
 * and items, the items taken from the hand - and then comes the question of who gets it: one of the quick
 * choices ({@code #1}, {@code #2}, {@code #3}, the top ten, from place ten on, a number of kills,
 * everybody who took part) or any stretch of placings set by hand. Every reward that fits a player pays out.
 * <p>
 * Reached through the same button from the create panel, the event panel and the race panel.
 */
public final class RewardUi {

    /** The money steps a click walks through. */
    private static final int[] MONEY_STEPS = {0, 50, 100, 250, 500, 1000, 2500, 5000, 10000};
    /** The kill counts a click walks through. */
    private static final int[] KILL_STEPS = {1, 2, 3, 5, 7, 10, 15, 20};

    private RewardUi() {
    }

    /**
     * @param player who is editing
     * @param edit   the event and where changes go
     */
    public static void open(Player player, EventEdit edit) {
        player.openInventory(list(player, edit).getInventory());
    }

    /* ------------------------------------------------------------------------------------ the list */

    private static CustomInventory list(Player player, EventEdit edit) {
        EventData event = edit.current();
        List<RewardRule> rules = EventRewards.of(event);
        CustomInventory ui = new CustomInventory(9 * 6, ChatColor.GOLD + "Belohnungen", close -> {
        });
        ui.fillPlaceHolder();

        for (int i = 0; i < rules.size() && i < EventRewards.MAX_RULES; i++) {
            int index = i;
            RewardRule rule = rules.get(i);
            List<String> lore = new ArrayList<>();
            for (String line : rule.getPrize().describe()) lore.add(ChatColor.GRAY + line);
            lore.add("");
            lore.add(ChatColor.AQUA + "Klicken zum Bearbeiten");
            ui.setItem(i, new ItemApi(iconOf(rule), ChatColor.GOLD + rule.describeWho(), lore).build(),
                    new SimpleItemAction(click -> player.openInventory(detail(player, edit, index).getInventory())));
        }
        if (rules.isEmpty()) {
            ui.setItem(13, new ItemApi(Material.PAPER, ChatColor.GRAY + "Noch keine Belohnungen",
                    List.of(ChatColor.GRAY + "Unten rechts eine anlegen.")).build(), SimpleItemAction.display());
        }

        ui.setItem(40, howItWorks(event), SimpleItemAction.display());

        ui.setItem(45, new ItemApi(Material.ARROW, ChatColor.YELLOW + "Zurück",
                List.of(ChatColor.GRAY + (edit.isDraft()
                        ? "Übernommen wird beim Anlegen."
                        : "Jede Änderung ist schon gespeichert."))).build(),
                new SimpleItemAction(click -> edit.back(player)));

        if (rules.size() < EventRewards.MAX_RULES) {
            ui.setItem(53, new ItemApi(Material.LIME_DYE, ChatColor.GREEN + "Neue Belohnung",
                    List.of(ChatColor.GRAY + "Erst festlegen, was es gibt,",
                            ChatColor.GRAY + "dann, wer es bekommt.")).build(),
                    new SimpleItemAction(click -> {
                        int index = rules.size();
                        edit.change(player, changed -> {
                            List<RewardRule> current = EventRewards.of(changed);
                            current.add(RewardRule.place(nextFreePlace(current), new PrizeData()));
                            EventRewards.set(changed, current);
                        }, () -> player.openInventory(detail(player, edit, index).getInventory()));
                    }));
        }
        return ui;
    }

    private static ItemStack howItWorks(EventData event) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Jede Belohnung, die auf jemanden");
        lore.add(ChatColor.GRAY + "passt, wird ausgezahlt - wer Platz 1");
        lore.add(ChatColor.GRAY + "und 5 Kills hat, bekommt beides.");
        lore.add("");
        lore.add(ChatColor.GRAY + "Ausgezahlt wird, wenn das Event endet.");
        lore.add(ChatColor.GRAY + "Wer offline ist, bekommt es beim");
        lore.add(ChatColor.GRAY + "nächsten Join.");
        if (!event.getType().isRanked()) {
            lore.add("");
            lore.add(ChatColor.RED + event.getType().getTitle() + " wertet niemanden -");
            lore.add(ChatColor.RED + "hier wird nichts ausgezahlt.");
        }
        return new ItemApi(Material.BOOK, ChatColor.AQUA + "So funktioniert's", lore).build();
    }

    /**
     * @param rules the rewards there are
     * @return the first single placing nobody has a reward for yet, so a fresh reward starts somewhere useful
     */
    private static int nextFreePlace(List<RewardRule> rules) {
        int place = 1;
        boolean taken = true;
        while (taken) {
            taken = false;
            for (RewardRule rule : rules) {
                if (rule.getCondition() == RewardRule.Condition.PLACE
                        && rule.getFrom() == place && rule.getTo() == place) {
                    taken = true;
                    place++;
                    break;
                }
            }
        }
        return place;
    }

    /* ---------------------------------------------------------------------------------- one reward */

    private static CustomInventory detail(Player player, EventEdit edit, int index) {
        List<RewardRule> rules = EventRewards.of(edit.current());
        if (index < 0 || index >= rules.size()) return list(player, edit);
        RewardRule rule = rules.get(index);
        boolean kills = edit.current().getType().countsKills();

        CustomInventory ui = new CustomInventory(9 * 6, ChatColor.GOLD + "Belohnung: " + rule.describeWho(),
                close -> {
                });
        ui.fillPlaceHolder();

        List<String> preview = new ArrayList<>();
        preview.add(ChatColor.GRAY + "Wer: " + ChatColor.WHITE + rule.describeWho());
        preview.add(ChatColor.GRAY + "Was:");
        for (String line : rule.getPrize().describe()) preview.add(ChatColor.WHITE + "  " + line);
        ui.setItem(4, new ItemApi(iconOf(rule), ChatColor.GOLD + "Diese Belohnung", preview).build(),
                SimpleItemAction.display());

        Consumer<Consumer<RewardRule>> change = mutation -> edit.change(player, changed -> {
            List<RewardRule> current = EventRewards.of(changed);
            if (index >= current.size()) return;
            mutation.accept(current.get(index));
            EventRewards.set(changed, current);
        }, () -> player.openInventory(detail(player, edit, index).getInventory()));

        // --- what there is
        ui.setItem(9, new ItemApi(Material.OAK_SIGN, ChatColor.YELLOW + "1. Was gibt es?").build(),
                SimpleItemAction.display());
        ui.setItem(11, new ItemApi(Material.GOLD_NUGGET, ChatColor.GOLD + "Geld: " + rule.getPrize().getMoney()
                        + " Bits", List.of(ChatColor.GRAY + "Links: mehr · Rechts: weniger")).build(),
                new SimpleItemAction(click -> change.accept(edited ->
                        edited.getPrize().setMoney(step(MONEY_STEPS, edited.getPrize().getMoney(),
                                !click.isRightClick())))));
        ui.setItem(12, new ItemApi(Material.CHEST, ChatColor.GOLD + "Item aus der Hand",
                        List.of(ChatColor.GRAY + "Legt das Item in deiner Hand",
                                ChatColor.GRAY + "mit seiner Anzahl dazu.")).build(),
                new SimpleItemAction(click -> {
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    if (hand.getType().isAir()) {
                        player.sendMessage(ChatColor.RED + "Du hast nichts in der Hand.");
                        return;
                    }
                    String material = hand.getType().name();
                    int amount = hand.getAmount();
                    change.accept(edited -> edited.getPrize().withItem(material, amount));
                }));
        ui.setItem(13, new ItemApi(Material.LAVA_BUCKET, ChatColor.RED + "Items leeren",
                        List.of(ChatColor.GRAY + "Das Geld bleibt.")).build(),
                new SimpleItemAction(click -> change.accept(edited -> edited.getPrize().getItems().clear())));

        // --- who gets it
        ui.setItem(18, new ItemApi(Material.OAK_SIGN, ChatColor.YELLOW + "2. Wer bekommt sie?").build(),
                SimpleItemAction.display());
        preset(ui, 19, rule, "#1", Material.GOLD_INGOT, RewardRule.Condition.PLACE, 1, 1, change);
        preset(ui, 20, rule, "#2", Material.IRON_INGOT, RewardRule.Condition.PLACE, 2, 2, change);
        preset(ui, 21, rule, "#3", Material.COPPER_INGOT, RewardRule.Condition.PLACE, 3, 3, change);
        preset(ui, 22, rule, "Top 10 (Platz 1-10)", Material.EMERALD, RewardRule.Condition.PLACE, 1, 10, change);
        preset(ui, 23, rule, "ab Platz 10", Material.COAL, RewardRule.Condition.PLACE, 10,
                RewardRule.OPEN_END, change);
        if (kills) {
            ui.setItem(24, choice(Material.IRON_SWORD, "ab X Kills",
                            rule.getCondition() == RewardRule.Condition.KILLS,
                            "Wer mindestens so viele Kills hat.", "Die Zahl stellst du unten ein."),
                    new SimpleItemAction(click -> change.accept(edited -> {
                        int killCount = edited.getKills();
                        edited.setCondition(RewardRule.Condition.KILLS);
                        edited.setKills(killCount);
                    })));
        } else {
            ui.setItem(24, new ItemApi(Material.GRAY_DYE, ChatColor.DARK_GRAY + "ab X Kills",
                    List.of(ChatColor.DARK_GRAY + edit.current().getType().getTitle() + " zählt keine Kills.")).build(),
                    SimpleItemAction.display());
        }
        ui.setItem(25, choice(Material.PAPER, "Teilnahme",
                        rule.getCondition() == RewardRule.Condition.PARTICIPATION,
                        "Jeder, der mitgemacht hat."),
                new SimpleItemAction(click -> change.accept(edited ->
                        edited.setCondition(RewardRule.Condition.PARTICIPATION))));

        // --- by hand
        ui.setItem(27, new ItemApi(Material.OAK_SIGN, ChatColor.YELLOW + "Genau einstellen").build(),
                SimpleItemAction.display());
        if (rule.getCondition() == RewardRule.Condition.PLACE) {
            ui.setItem(29, new ItemApi(Material.LIME_CONCRETE, ChatColor.GOLD + "Von Platz " + rule.getFrom(),
                            List.of(ChatColor.GRAY + "Links: +1 · Rechts: -1",
                                    ChatColor.GRAY + "Mit Shift: ±5")).build(),
                    new SimpleItemAction(click -> change.accept(edited -> {
                        int by = click.isShiftClick() ? 5 : 1;
                        int from = edited.getFrom() + (click.isRightClick() ? -by : by);
                        edited.setRange(from, edited.getTo());
                    })));
            String to = rule.getTo() == RewardRule.OPEN_END ? "zum letzten" : "Platz " + rule.getTo();
            ui.setItem(31, new ItemApi(Material.RED_CONCRETE, ChatColor.GOLD + "Bis " + to,
                            List.of(ChatColor.GRAY + "Links: +1 · Rechts: -1",
                                    ChatColor.GRAY + "Mit Shift: ±5",
                                    ChatColor.GRAY + "Unter \"von\" heißt: bis zum letzten")).build(),
                    new SimpleItemAction(click -> change.accept(edited -> {
                        int by = click.isShiftClick() ? 5 : 1;
                        int current = edited.getTo() == RewardRule.OPEN_END ? edited.getFrom() - 1 : edited.getTo();
                        int next = current + (click.isRightClick() ? -by : by);
                        // stepping below the start is how "to the last one" is reached, and stepping up
                        // from there starts at the start again
                        edited.setRange(edited.getFrom(), next < edited.getFrom() ? RewardRule.OPEN_END : next);
                    })));
        } else if (rule.getCondition() == RewardRule.Condition.KILLS) {
            ui.setItem(30, new ItemApi(Material.IRON_SWORD, ChatColor.GOLD + "Ab " + rule.getKills() + " Kills",
                            List.of(ChatColor.GRAY + "Links: mehr · Rechts: weniger")).build(),
                    new SimpleItemAction(click -> change.accept(edited ->
                            edited.setKills(step(KILL_STEPS, edited.getKills(), !click.isRightClick())))));
        } else {
            ui.setItem(30, new ItemApi(Material.PAPER, ChatColor.GRAY + "Nichts einzustellen",
                    List.of(ChatColor.GRAY + "Jeder, der mitgemacht hat, bekommt sie.")).build(),
                    SimpleItemAction.display());
        }

        ui.setItem(45, new ItemApi(Material.ARROW, ChatColor.YELLOW + "Zur Liste").build(),
                new SimpleItemAction(click -> player.openInventory(list(player, edit).getInventory())));
        ui.setItem(53, new ItemApi(Material.BARRIER, ChatColor.RED + "Belohnung löschen").build(),
                new SimpleItemAction(click -> edit.change(player, changed -> {
                    List<RewardRule> current = EventRewards.of(changed);
                    if (index < current.size()) current.remove(index);
                    EventRewards.set(changed, current);
                }, () -> player.openInventory(list(player, edit).getInventory()))));
        return ui;
    }

    private static void preset(CustomInventory ui, int slot, RewardRule rule, String title, Material icon,
                               RewardRule.Condition condition, int from, int to,
                               Consumer<Consumer<RewardRule>> change) {
        boolean active = rule.getCondition() == condition && rule.getFrom() == from && rule.getTo() == to;
        ui.setItem(slot, choice(icon, title, active), new SimpleItemAction(click -> change.accept(edited -> {
            edited.setCondition(condition);
            edited.setRange(from, to);
        })));
    }

    private static ItemStack choice(Material icon, String title, boolean active, String... lines) {
        List<String> lore = new ArrayList<>();
        for (String line : lines) lore.add(ChatColor.GRAY + line);
        lore.add(active ? ChatColor.GREEN + "✔ ausgewählt" : ChatColor.GRAY + "Klicken zum Auswählen");
        return new ItemApi(active ? Material.LIME_STAINED_GLASS_PANE : icon,
                (active ? ChatColor.GREEN : ChatColor.GOLD) + title, lore).build();
    }

    private static Material iconOf(RewardRule rule) {
        return switch (rule.getCondition()) {
            case KILLS -> Material.IRON_SWORD;
            case PARTICIPATION -> Material.PAPER;
            case PLACE -> switch (rule.getFrom()) {
                case 1 -> rule.getTo() == 1 ? Material.GOLD_INGOT : Material.EMERALD;
                case 2 -> Material.IRON_INGOT;
                case 3 -> Material.COPPER_INGOT;
                default -> Material.COAL;
            };
        };
    }

    private static int step(int[] values, int current, boolean forward) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] != current) continue;
            int next = forward ? i + 1 : i - 1;
            if (next >= values.length) next = 0;
            if (next < 0) next = values.length - 1;
            return values[next];
        }
        int nearest = values[0];
        for (int value : values) {
            if (Math.abs(value - current) < Math.abs(nearest - current)) nearest = value;
        }
        return nearest;
    }

    /* ------------------------------------------------------------------------------- for other panels */

    /**
     * @param event the event
     * @return its rewards, one line each, for the lore of a button players see
     */
    public static List<String> describe(EventData event) {
        List<String> lore = new ArrayList<>();
        for (RewardRule rule : EventRewards.of(event)) {
            if (rule.getPrize().isEmpty()) continue;
            lore.add(ChatColor.GOLD + rule.describeWho() + ChatColor.GRAY + ": " + ChatColor.WHITE
                    + String.join(", ", rule.getPrize().describe()));
        }
        if (lore.isEmpty()) lore.add(ChatColor.GRAY + "Keine Belohnungen hinterlegt.");
        return lore;
    }

    /**
     * The button that leads here, the same on every panel it appears on.
     *
     * @param event the event
     * @param admin whether the viewer may change them
     * @return the button
     */
    public static ItemStack icon(EventData event, boolean admin) {
        List<String> lore = new ArrayList<>(describe(event));
        if (!event.getType().isRanked()) {
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + event.getType().getTitle() + " wertet niemanden.");
        } else if (admin) {
            lore.add("");
            lore.add(ChatColor.AQUA + "Klicken zum Bearbeiten");
        }
        return new ItemApi(Material.GOLD_INGOT, ChatColor.GOLD + "Belohnungen", lore).build();
    }
}

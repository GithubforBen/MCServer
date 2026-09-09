package de.hems.paper.event;

import de.hems.api.ItemApi;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.hems.types.event.EventData;
import de.hems.types.event.PokerEventSettings;
import de.hems.types.poker.PokerFormat;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The knobs of a poker night, as buttons.
 * <p>
 * There are more of them than fit next to the name and the start time, which is why they are their own
 * panel rather than four more slots in {@link EventCreateUi}. It edits a draft in place and hands it back,
 * so the same panel serves creating a night and correcting one that already exists.
 * <p>
 * Every number cycles through presets on a left click and goes back on a right click. Typing numbers into
 * chat is how a buy-in ends up at 100000 because somebody's finger slipped, and a buy-in is real money.
 */
public final class PokerSettingsUi {

    private static final int[] BUY_INS = {100, 250, 500, 1000, 2500, 5000, 10000};
    private static final int[] SMALL_BLINDS = {1, 5, 10, 25, 50, 100, 250};
    /** Zero is a table that takes nothing, which is a decision an admin is allowed to make. */
    private static final int[] RAKES_PERMILLE = {0, 5, 10, 20, 30, 50, 75, 100};
    private static final int[] RAKE_CAPS_BB = {0, 10, 25, 50, 100};
    private static final int[] BOT_FEES = {0, 50, 100, 250, 500, 1000};
    private static final int[] MIN_HANDS = {0, 5, 10, 20, 30, 50, 100};
    private static final int[] BLIND_UPS = {5, 8, 10, 12, 15, 20, 30};

    private PokerSettingsUi() {
    }

    /**
     * @param player who is editing
     * @param draft  the night being set up
     * @param back   what to open when the panel is left, given the draft as it now stands
     * @return the panel
     */
    public static CustomInventory build(Player player, EventData draft, Consumer<EventData> back) {
        PokerEventSettings settings = new PokerEventSettings(draft);
        settings.applyDefaults();

        CustomInventory ui = new CustomInventory(9 * 5, ChatColor.GOLD + "Pokernacht einstellen", close -> {
        });
        ui.fillPlaceHolder();

        Runnable reopen = () -> player.openInventory(build(player, draft, back).getInventory());

        PokerFormat format = settings.getFormat();
        ui.setItem(10, new ItemApi(format == PokerFormat.CASH ? Material.GOLD_INGOT : Material.GOLDEN_HELMET,
                        ChatColor.GOLD + "Format",
                        List.of(ChatColor.GRAY + "Aktuell: " + ChatColor.WHITE + format.getTitle(),
                                ChatColor.DARK_GRAY + format.getDescription(),
                                ChatColor.GRAY + "Klicken für: " + format.next().getTitle())).build(),
                new SimpleItemAction(click -> {
                    settings.setFormat(settings.getFormat().next());
                    reopen.run();
                }));

        ui.setItem(11, new ItemApi(Material.EMERALD, ChatColor.GOLD + "Buy-in",
                        List.of(ChatColor.GRAY + "Aktuell: " + ChatColor.WHITE + settings.getBuyIn() + " Bits",
                                ChatColor.DARK_GRAY + "So viele Chips bekommt man dafür - eins zu eins",
                                cycleHint())).build(),
                new SimpleItemAction(click -> {
                    settings.setBuyIn(step(BUY_INS, settings.getBuyIn(), forward(click.getClick())));
                    // the qualifying volume was set as a multiple of the buy-in, so it follows it along
                    // rather than quietly staying at a number that no longer means anything
                    settings.setMinVolume(PokerEventSettings.DEFAULT_MIN_VOLUME_BUY_INS * settings.getBuyIn());
                    reopen.run();
                }));

        ui.setItem(12, new ItemApi(Material.GOLD_NUGGET, ChatColor.GOLD + "Blinds",
                        List.of(ChatColor.GRAY + "Aktuell: " + ChatColor.WHITE
                                        + settings.getSmallBlind() + "/" + settings.getBigBlind(),
                                ChatColor.DARK_GRAY + "Ein Buy-in sind "
                                        + (settings.getBuyIn() / settings.getBigBlind()) + " Big Blinds",
                                cycleHint())).build(),
                new SimpleItemAction(click -> {
                    settings.setSmallBlind(step(SMALL_BLINDS, settings.getSmallBlind(), forward(click.getClick())));
                    reopen.run();
                }));

        ui.setItem(13, new ItemApi(Material.HOPPER, ChatColor.GOLD + "Hausanteil",
                        List.of(ChatColor.GRAY + "Aktuell: " + ChatColor.WHITE + settings.getRakeText()
                                        + " pro Pot",
                                ChatColor.DARK_GRAY + "Nur aus Pots, um die wirklich gespielt wurde",
                                cycleHint())).build(),
                new SimpleItemAction(click -> {
                    settings.setRakePermille(step(RAKES_PERMILLE, settings.getRakePermille(),
                            forward(click.getClick())));
                    reopen.run();
                }));

        int cap = settings.getRakeCapBigBlinds();
        ui.setItem(14, new ItemApi(Material.IRON_BARS, ChatColor.GOLD + "Deckel für den Hausanteil",
                        List.of(ChatColor.GRAY + "Aktuell: " + ChatColor.WHITE
                                        + (cap == 0 ? "kein Deckel" : cap + " Big Blinds ("
                                        + (cap * settings.getBigBlind()) + " Bits)"),
                                ChatColor.DARK_GRAY + "Ohne Deckel kostet ein einziger großer Pot mehr",
                                ChatColor.DARK_GRAY + "als der ganze restliche Abend",
                                cycleHint())).build(),
                new SimpleItemAction(click -> {
                    settings.setRakeCapBigBlinds(step(RAKE_CAPS_BB, cap, forward(click.getClick())));
                    reopen.run();
                }));

        ui.setItem(15, new ItemApi(Material.OAK_STAIRS, ChatColor.GOLD + "Plätze pro Tisch",
                        List.of(ChatColor.GRAY + "Aktuell: " + ChatColor.WHITE + settings.getSeats(),
                                cycleHint())).build(),
                new SimpleItemAction(click -> {
                    int seats = settings.getSeats() + (forward(click.getClick()) ? 1 : -1);
                    if (seats > PokerEventSettings.MAX_SEATS) seats = PokerEventSettings.MIN_SEATS;
                    if (seats < PokerEventSettings.MIN_SEATS) seats = PokerEventSettings.MAX_SEATS;
                    settings.setSeats(seats);
                    reopen.run();
                }));

        ui.setItem(16, new ItemApi(Material.OAK_PLANKS, ChatColor.GOLD + "Tische",
                        List.of(ChatColor.GRAY + "Aktuell: " + ChatColor.WHITE + settings.getTables(),
                                ChatColor.GRAY + "Platz für " + ChatColor.WHITE + settings.getCapacity()
                                        + ChatColor.GRAY + " Spieler",
                                cycleHint())).build(),
                new SimpleItemAction(click -> {
                    int tables = settings.getTables() + (forward(click.getClick()) ? 1 : -1);
                    if (tables > PokerEventSettings.MAX_TABLES) tables = 1;
                    if (tables < 1) tables = PokerEventSettings.MAX_TABLES;
                    settings.setTables(tables);
                    reopen.run();
                }));

        boolean bots = settings.isBotsAllowed();
        ui.setItem(19, new ItemApi(bots ? Material.ARMOR_STAND : Material.BARRIER,
                        ChatColor.GOLD + "Bots",
                        List.of(ChatColor.GRAY + "Aktuell: "
                                        + (bots ? ChatColor.GREEN + "erlaubt" : ChatColor.RED + "aus"),
                                ChatColor.DARK_GRAY + "Wer einen setzt, bezahlt seinen Stack",
                                ChatColor.DARK_GRAY + "und bekommt zurück, was davon übrig ist",
                                ChatColor.GRAY + "Klicken zum Umschalten")).build(),
                new SimpleItemAction(click -> {
                    settings.setBotsAllowed(!settings.isBotsAllowed());
                    reopen.run();
                }));

        ui.setItem(20, new ItemApi(Material.GOLDEN_CARROT, ChatColor.GOLD + "Gebühr pro Bot",
                        List.of(ChatColor.GRAY + "Aktuell: " + ChatColor.WHITE + settings.getBotFee() + " Bits",
                                ChatColor.DARK_GRAY + "Kommt nicht zurück - deshalb ist ein Tisch",
                                ChatColor.DARK_GRAY + "voller Bots eine Entscheidung und kein Hebel",
                                cycleHint())).build(),
                new SimpleItemAction(click -> {
                    settings.setBotFee(step(BOT_FEES, settings.getBotFee(), forward(click.getClick())));
                    reopen.run();
                }));

        ui.setItem(22, new ItemApi(Material.WRITABLE_BOOK, ChatColor.GOLD + "Wertung: Mindesthände",
                        List.of(ChatColor.GRAY + "Aktuell: " + ChatColor.WHITE + settings.getMinHands()
                                        + " Hände",
                                ChatColor.DARK_GRAY + "Ohne das gewinnt die Rangliste, wer einmal",
                                ChatColor.DARK_GRAY + "klein einsteigt, eine Hand trifft und aufhört",
                                cycleHint())).build(),
                new SimpleItemAction(click -> {
                    settings.setMinHands(step(MIN_HANDS, settings.getMinHands(), forward(click.getClick())));
                    reopen.run();
                }));

        ui.setItem(23, new ItemApi(Material.CHEST, ChatColor.GOLD + "Wertung: Mindesteinsatz",
                        List.of(ChatColor.GRAY + "Aktuell: " + ChatColor.WHITE + settings.getMinVolume()
                                        + " Bits insgesamt",
                                ChatColor.DARK_GRAY + "Das sind "
                                        + (settings.getMinVolume() / Math.max(1, settings.getBuyIn()))
                                        + " Buy-ins",
                                cycleHint())).build(),
                new SimpleItemAction(click -> {
                    int buyIn = settings.getBuyIn();
                    int steps = Math.max(1, settings.getMinVolume() / Math.max(1, buyIn));
                    steps += forward(click.getClick()) ? 1 : -1;
                    if (steps > 10) steps = 0;
                    if (steps < 0) steps = 10;
                    settings.setMinVolume(steps * buyIn);
                    reopen.run();
                }));

        if (settings.getFormat() == PokerFormat.TOURNAMENT) {
            ui.setItem(24, new ItemApi(Material.CLOCK, ChatColor.GOLD + "Blinds steigen",
                            List.of(ChatColor.GRAY + "Aktuell: " + ChatColor.WHITE
                                            + settings.getBlindUpMinutes() + " Minuten",
                                    ChatColor.DARK_GRAY + "Nur im Turnier",
                                    cycleHint())).build(),
                    new SimpleItemAction(click -> {
                        settings.setBlindUpMinutes(step(BLIND_UPS, settings.getBlindUpMinutes(),
                                forward(click.getClick())));
                        reopen.run();
                    }));
        }

        ui.setItem(31, summary(settings), SimpleItemAction.display());

        ui.setItem(36, new ItemApi(Material.ARROW, ChatColor.YELLOW + "Zurück").build(),
                new SimpleItemAction(click -> back.accept(draft)));
        return ui;
    }

    /**
     * @param settings the knobs
     * @return an item that says what this night will feel like, in one place
     */
    private static org.bukkit.inventory.ItemStack summary(PokerEventSettings settings) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Format: " + ChatColor.WHITE + settings.getFormat().getTitle());
        lore.add(ChatColor.GRAY + "Buy-in: " + ChatColor.WHITE + settings.getBuyIn() + " Bits");
        lore.add(ChatColor.GRAY + "Blinds: " + ChatColor.WHITE + settings.getSmallBlind()
                + "/" + settings.getBigBlind());
        lore.add(ChatColor.GRAY + "Haus: " + ChatColor.WHITE + settings.getRakeText()
                + (settings.getRakeCapBigBlinds() > 0
                ? " (max. " + (settings.getRakeCapBigBlinds() * settings.getBigBlind()) + " Bits)"
                : ""));
        lore.add(ChatColor.GRAY + "Tische: " + ChatColor.WHITE + settings.getTables()
                + " × " + settings.getSeats() + " Plätze");
        lore.add(ChatColor.GRAY + "Bots: " + ChatColor.WHITE
                + (settings.isBotsAllowed() ? settings.getBotFee() + " Bits Gebühr" : "aus"));
        lore.add(ChatColor.GRAY + "Wertung ab: " + ChatColor.WHITE + settings.getMinHands()
                + " Händen und " + settings.getMinVolume() + " Bits");
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "Gewertet wird, was rauskommt geteilt durch");
        lore.add(ChatColor.DARK_GRAY + "was reingeht. Höher ist besser.");
        return new ItemApi(Material.PLAYER_HEAD, ChatColor.GOLD + "So sieht der Abend aus", lore).build();
    }

    private static String cycleHint() {
        return ChatColor.DARK_GRAY + "Links: weiter · Rechts: zurück";
    }

    private static boolean forward(ClickType click) {
        return !click.isRightClick();
    }

    /**
     * @param values  the presets
     * @param current where we are now
     * @param forward whether to go up or down
     * @return the next preset, wrapping around, and the nearest one when the current value is not in the
     *         list at all - which happens to a night that was edited on the website
     */
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
}

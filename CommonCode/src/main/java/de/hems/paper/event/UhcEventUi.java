package de.hems.paper.event;

import de.hems.api.ItemApi;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.hems.types.event.EventData;
import de.hems.types.event.PrizeData;
import de.hems.types.event.RunData;
import de.hems.types.event.UhcObjective;
import de.hems.types.event.UhcSettings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The panel of a run event: what the rules are, who is waiting, and who was fastest.
 */
public final class UhcEventUi {

    /** How many places the leaderboard shows. */
    private static final int LEADERBOARD_SIZE = 21;

    private UhcEventUi() {
    }

    /**
     * @param player who is looking
     * @param event  the run event
     * @return the panel
     */
    public static CustomInventory build(Player player, EventData event) {
        UhcSettings settings = new UhcSettings(event);
        List<UhcObjective> objectives = UhcObjective.of(event.getType());

        CustomInventory ui = new CustomInventory(9 * 6, ChatColor.GOLD + event.getName(), close -> {
        });
        ui.fillPlaceHolder();

        ui.setItem(4, EventCalendarUi.icon(event), SimpleItemAction.display());

        List<String> goalLore = new ArrayList<>();
        for (UhcObjective objective : objectives) {
            goalLore.add(ChatColor.GRAY + "- " + ChatColor.WHITE + objective.getTitle());
        }
        ui.setItem(2, new ItemApi(Material.NETHER_STAR, ChatColor.AQUA + "Ziele", goalLore).build(),
                SimpleItemAction.display());

        ui.setItem(6, new ItemApi(Material.COMPARATOR, ChatColor.AQUA + "Regeln", List.of(
                ChatColor.GRAY + "Hardcore: " + ChatColor.WHITE + (settings.isHardcore() ? "an" : "aus"),
                ChatColor.GRAY + "Teamgröße: " + ChatColor.WHITE + settings.getTeamSize(),
                ChatColor.GRAY + "Versuche: " + ChatColor.WHITE
                        + (settings.getMaxRuns() == Integer.MAX_VALUE ? "unbegrenzt" : settings.getMaxRuns()),
                ChatColor.GRAY + "Unterbesetzt starten: " + ChatColor.WHITE
                        + (settings.isAllowUndermanned() ? "erlaubt" : "nein"))).build(),
                SimpleItemAction.display());

        drawQueue(ui, player, event, settings);
        drawLeaderboard(ui, event, objectives);

        ui.setItem(45, new ItemApi(Material.ARROW, ChatColor.YELLOW + "Zurück").build(),
                new SimpleItemAction(click -> player.openInventory(
                        EventCalendarUi.build(player, EventCalendarUi.Filter.ALL).getInventory())));

        List<String> prizeLore = new ArrayList<>();
        for (int place = 1; place <= PrizeData.PLACES; place++) {
            PrizeData prize = PrizeData.ofPlace(event, place);
            prizeLore.add(ChatColor.GOLD + "" + place + ". Platz: " + ChatColor.WHITE
                    + String.join(", ", prize.describe()));
        }
        prizeLore.add(ChatColor.GRAY + "Teilnahme: " + ChatColor.WHITE
                + String.join(", ", PrizeData.ofParticipation(event).describe()));
        ui.setItem(8, new ItemApi(Material.GOLD_INGOT, ChatColor.GOLD + "Preise", prizeLore).build(),
                player.isOp()
                        ? new SimpleItemAction(click ->
                                player.openInventory(PrizeUi.build(player, event).getInventory()))
                        : SimpleItemAction.display());

        if (player.isOp()) {
            ui.setItem(53, new ItemApi(Material.WRITABLE_BOOK, ChatColor.GREEN + "Einstellungen",
                    List.of(ChatColor.GRAY + "Regeln dieses Events ändern")).build(),
                    new SimpleItemAction(click ->
                            player.openInventory(UhcSettingsUi.build(player, event).getInventory())));
        }
        return ui;
    }

    /**
     * The queue: join, leave, and start once enough people are there.
     */
    private static void drawQueue(CustomInventory ui, Player player, EventData event, UhcSettings settings) {
        List<UUID> queue = RunQueue.getWaiting(event);
        boolean queued = RunQueue.isWaiting(event, player.getUniqueId());
        RunData active = RunService.getActiveRunOf(event.getId(), player.getUniqueId());

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Wartend: " + ChatColor.WHITE + queue.size() + "/" + settings.getTeamSize());
        for (UUID member : queue) lore.add(ChatColor.DARK_GRAY + "- " + nameOf(member));
        int used = RunService.countRunsOf(event.getId(), player.getUniqueId());
        lore.add("");
        lore.add(ChatColor.GRAY + "Deine Versuche: " + ChatColor.WHITE + used
                + (settings.getMaxRuns() == Integer.MAX_VALUE ? "" : "/" + settings.getMaxRuns()));

        if (active != null) {
            boolean paused = active.getState() == RunData.State.PAUSED;
            lore.add((paused ? ChatColor.YELLOW : ChatColor.GREEN) + "Zeit: "
                    + RunData.formatTicks(active.getElapsedTicks()));
            if (!paused) {
                ui.setItem(20, new ItemApi(Material.CLOCK, ChatColor.GREEN + "Dein Lauf läuft", lore).build(),
                        SimpleItemAction.display());
                return;
            }
            lore.add(ChatColor.GRAY + "Die Zeit steht still, solange niemand spielt.");
            lore.add("");
            lore.add(ChatColor.GREEN + "Klicken zum Weiterspielen");
            ui.setItem(20, new ItemApi(Material.CLOCK, ChatColor.YELLOW + "Lauf pausiert", lore).build(),
                    new SimpleItemAction(click -> {
                        player.sendMessage(ChatColor.AQUA + RunQueue.resume(event, player));
                        player.closeInventory();
                    }));
            return;
        }

        lore.add("");
        lore.add(queued ? ChatColor.YELLOW + "Klicken zum Verlassen" : ChatColor.GREEN + "Klicken zum Mitmachen");
        ui.setItem(20, new ItemApi(queued ? Material.RED_BED : Material.GREEN_BED,
                queued ? ChatColor.YELLOW + "In der Warteschlange" : ChatColor.GREEN + "Mitmachen", lore).build(),
                new SimpleItemAction(click -> {
                    player.sendMessage(ChatColor.AQUA + RunQueue.toggle(event, player));
                    player.openInventory(build(player, event).getInventory());
                }));

        if (queued && queue.size() > 0) {
            boolean full = queue.size() >= settings.getTeamSize();
            ui.setItem(24, new ItemApi(Material.LIME_DYE, ChatColor.GREEN + "Jetzt starten", List.of(
                    full ? ChatColor.GRAY + "Ihr seid vollzählig."
                            : ChatColor.YELLOW + "Ihr startet zu " + queue.size() + " statt zu "
                            + settings.getTeamSize(),
                    settings.isAllowUndermanned() || full
                            ? ChatColor.GRAY + "Klicken zum Starten"
                            : ChatColor.RED + "Unterbesetzt starten ist aus")).build(),
                    new SimpleItemAction(click -> {
                        player.sendMessage(ChatColor.AQUA + RunQueue.start(event, player));
                        player.closeInventory();
                    }));
        }
    }

    /**
     * The leaderboard: the finished runs, fastest first.
     */
    private static void drawLeaderboard(CustomInventory ui, EventData event, List<UhcObjective> objectives) {
        List<RunData> ranked = RunService.getLeaderboard(event.getId());
        if (ranked.isEmpty()) {
            ui.setItem(31, new ItemApi(Material.PAPER, ChatColor.GRAY + "Noch keine Zeiten",
                    List.of(ChatColor.GRAY + "Es hat noch niemand durchgespielt.")).build(),
                    SimpleItemAction.display());
            return;
        }
        for (int i = 0; i < ranked.size() && i < LEADERBOARD_SIZE; i++) {
            RunData run = ranked.get(i);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GOLD + "Zeit: " + ChatColor.WHITE + RunData.formatTicks(run.getElapsedTicks()));
            for (UUID member : run.getParticipants()) lore.add(ChatColor.GRAY + "- " + nameOf(member));
            if (run.isUndermanned()) {
                lore.add(ChatColor.YELLOW + "Unterbesetzt gelaufen (" + run.getParticipants().size()
                        + "/" + run.getIntendedTeamSize() + ")");
            }
            ui.setItem(27 + i, new ItemApi(medal(i), ChatColor.WHITE + "#" + (i + 1) + " "
                    + nameOf(run.getParticipants().stream().findFirst().orElse(null)), lore).build(),
                    SimpleItemAction.display());
        }
    }

    /**
     * @param place the zero based place
     * @return gold, silver, bronze, then plain
     */
    private static Material medal(int place) {
        return switch (place) {
            case 0 -> Material.GOLD_INGOT;
            case 1 -> Material.IRON_INGOT;
            case 2 -> Material.COPPER_INGOT;
            default -> Material.PAPER;
        };
    }

    /**
     * @param id a player
     * @return their name, or the id if the server has never seen them
     */
    static String nameOf(UUID id) {
        if (id == null) return "?";
        String name = Bukkit.getOfflinePlayer(id).getName();
        return name == null ? id.toString().substring(0, 8) : name;
    }
}

package de.schnorrenbergers.lobby.rounds;

import de.hems.api.ItemApi;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.hems.paper.round.RoundService;
import de.hems.paper.round.RoundStarter;
import de.hems.paper.warp.ServerStartup;
import de.hems.types.round.RoundData;
import de.hems.types.round.RoundMaps;
import de.hems.types.round.RoundPolicy;
import de.hems.types.round.RoundState;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The list of rounds that are open right now, and the way to put one up yourself.
 * <p>
 * Deliberately one screen: a round is either there to be joined or it is not, and the interesting question
 * - why can I not start one - is answered on the button itself rather than after clicking it. Somebody who
 * is told "in 3 minutes" does not press it four more times.
 */
public final class RoundBrowserUi {

    private static final int SIZE = 9 * 6;
    private static final int CONTENT_START = 9;
    private static final int CONTENT_SIZE = 36;

    private RoundBrowserUi() {
    }

    /**
     * @param player who is looking
     */
    public static void open(Player player) {
        CustomInventory.show(player, build(player));
    }

    private static CustomInventory build(Player player) {
        CustomInventory inventory = new CustomInventory(SIZE, "Runden", null);
        inventory.fillPlaceHolder();

        List<RoundData> rounds = joinable(player);
        for (int i = 0; i < rounds.size() && i < CONTENT_SIZE; i++) {
            RoundData round = rounds.get(i);
            inventory.setItem(CONTENT_START + i, icon(round, player), new SimpleItemAction(event -> {
                Player clicker = (Player) event.getWhoClicked();
                clicker.closeInventory();
                join(clicker, round);
            }));
        }
        if (rounds.isEmpty()) {
            inventory.setItem(22, new ItemApi(Material.GRAY_DYE, ChatColor.GRAY + "Gerade läuft keine Runde",
                    List.of(ChatColor.DARK_GRAY + "Mach die erste auf.")).build(),
                    new SimpleItemAction(event -> {
                    }));
        }

        inventory.setItem(SIZE - 9, new ItemApi(Material.CLOCK, ChatColor.YELLOW + "Aktualisieren").build(),
                new SimpleItemAction(event -> {
                    RoundService.refreshAsync();
                    open((Player) event.getWhoClicked());
                }));
        inventory.setItem(SIZE - 5, startButton(player), new SimpleItemAction(event -> {
            Player clicker = (Player) event.getWhoClicked();
            if (RoundStarter.precheck(clicker) != null) {
                // the reason is already on the button; clicking it again does not change it
                open(clicker);
                return;
            }
            RoundCreateUi.open(clicker);
        }));
        if (player.isOp()) {
            inventory.setItem(SIZE - 1, new ItemApi(Material.COMPARATOR, ChatColor.AQUA + "Einstellungen",
                            List.of(ChatColor.GRAY + "Wer darf selbst Runden starten,",
                                    ChatColor.GRAY + "wie viele und wie oft")).build(),
                    new SimpleItemAction(event -> RoundPolicyUi.open((Player) event.getWhoClicked())));
        }
        return inventory;
    }

    /**
     * @param player who is looking
     * @return the rounds they may go to
     */
    private static List<RoundData> joinable(Player player) {
        List<RoundData> joinable = new ArrayList<>();
        for (RoundData round : RoundService.getOpenRounds()) {
            if (round.getServerName() == null) continue;
            // a closed round is for its owner and the people they invited, so nobody else is shown it
            if (!round.isAllowed(player.getUniqueId()) && !player.isOp()) continue;
            joinable.add(round);
        }
        return joinable;
    }

    /**
     * One round in the list.
     */
    private static ItemStack icon(RoundData round, Player viewer) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Modus: " + ChatColor.WHITE + modeName(round));
        lore.add(ChatColor.GRAY + "Map: " + ChatColor.WHITE + RoundMaps.displayName(round.getMap()));
        lore.add(ChatColor.GRAY + "Status: " + ChatColor.WHITE + round.getState().getDescription());
        if (round.getPlayers() > 0) {
            lore.add(ChatColor.GRAY + "Spieler: " + ChatColor.WHITE + round.getPlayers());
        }
        if (!round.isOpen()) {
            lore.add(ChatColor.YELLOW + "Privat"
                    + (round.isOwner(viewer.getUniqueId()) && !round.getInvited().isEmpty()
                    ? ChatColor.GRAY + " (" + round.getInvited().size() + " eingeladen)" : ""));
        }
        if (round.isOwner(viewer.getUniqueId())) lore.add(ChatColor.GREEN + "Deine Runde");
        lore.add(" ");
        lore.add(ChatColor.YELLOW + "Klicken zum Beitreten");
        Material material = round.getState() == RoundState.PREPARING ? Material.YELLOW_WOOL : Material.LIME_WOOL;
        String title = ChatColor.AQUA + (round.getOwnerName() == null
                ? "Runde" : "Runde von " + round.getOwnerName());
        return new ItemApi(material, title, lore).build();
    }

    private static String modeName(RoundData round) {
        return switch (round.getTeamSize()) {
            case 1 -> "Solo";
            case 2 -> "Doubles (2er)";
            case 3 -> "Trio (3er)";
            default -> "Quad (4er)";
        };
    }

    /**
     * The button that starts a new round, or says why it cannot.
     */
    private static ItemStack startButton(Player player) {
        RoundStarter.Refusal refusal = RoundStarter.precheck(player);
        if (refusal == null) {
            RoundPolicy policy = RoundService.getPolicy();
            return new ItemApi(Material.NETHER_STAR, ChatColor.GREEN + "Eigene Runde starten",
                    List.of(ChatColor.GRAY + "Map, Modus und Addons wählen",
                            ChatColor.DARK_GRAY + "Du bist dann Rundenadmin.",
                            ChatColor.DARK_GRAY + "Speicher pro Runde: "
                                    + RoundStarter.memoryOf(policy) + " MB")).build();
        }
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.RED + refusal.message());
        if (refusal.capacity()) {
            lore.add(" ");
            lore.add(ChatColor.DARK_GRAY + "Ein Admin sieht im Server Manager,");
            lore.add(ChatColor.DARK_GRAY + "wo noch Speicher zu holen wäre.");
        }
        return new ItemApi(Material.BARRIER, ChatColor.GRAY + "Eigene Runde starten", lore).build();
    }

    /**
     * Sends a player to a round that is already up.
     */
    private static void join(Player player, RoundData round) {
        if (round.getServerName() == null) {
            player.sendMessage(ChatColor.RED + "Diese Runde steht noch nicht bereit.");
            return;
        }
        player.sendMessage(ChatColor.GRAY + "Du wirst verbunden ...");
        // never ensureAndWarp: a round whose server is gone is over, and starting it again would put up a
        // fresh empty round for one person who happened to click an old entry
        ServerStartup.warpWhenReady(player, round.getServerName());
    }
}

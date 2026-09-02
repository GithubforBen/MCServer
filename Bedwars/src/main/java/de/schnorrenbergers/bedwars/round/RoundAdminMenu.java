package de.schnorrenbergers.bedwars.round;

import de.hems.api.ItemApi;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.hems.types.round.RoundData;
import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.admin.AdminMenu;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.phase.IngamePhase;
import de.schnorrenbergers.bedwars.lobby.AddonMenu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * What the person who started this round can do with it.
 * <p>
 * Three things, and they are the three that come up while everybody is standing in the waiting lobby:
 * begin now rather than waiting out the countdown, decide whether strangers may walk in, and remove
 * somebody who is ruining it. The deeper switches - 1.8 combat, the addons - are the same menus a real
 * admin gets, reached from here rather than duplicated.
 */
public final class RoundAdminMenu {

    private static final int SIZE = 9 * 3;

    private RoundAdminMenu() {
    }

    /**
     * @param player the round admin
     */
    public static void open(Player player) {
        if (!RoundContext.mayAdminister(player)) {
            player.sendMessage(ChatColor.RED + "Das ist nicht deine Runde.");
            return;
        }
        CustomInventory.show(player, build(player));
    }

    private static CustomInventory build(Player player) {
        Game game = Bedwars.getInstance().getGame();
        RoundData round = RoundContext.get();
        CustomInventory menu = new CustomInventory(SIZE, "Runde verwalten", null);
        menu.fillPlaceHolder();

        menu.setItem(10, startIcon(game), new SimpleItemAction(event -> {
            Player clicker = (Player) event.getWhoClicked();
            clicker.closeInventory();
            forceStart(clicker, game);
        }));

        if (round != null) {
            menu.setItem(11, visibilityIcon(round), new SimpleItemAction(event -> {
                RoundContext.setOpen(!RoundContext.get().isOpen());
                open((Player) event.getWhoClicked());
            }));
        }

        menu.setItem(12, new ItemApi(Material.BARRIER, ChatColor.AQUA + "Spieler entfernen",
                        List.of(ChatColor.GRAY + "Wer stört, fliegt zurück in die Lobby.")).build(),
                new SimpleItemAction(event -> openKickList((Player) event.getWhoClicked())));

        menu.setItem(14, new ItemApi(Material.COMPARATOR, ChatColor.AQUA + "Einstellungen",
                        List.of(ChatColor.GRAY + "1.8-Kampf, Autostart und der Rest")).build(),
                new SimpleItemAction(event -> AdminMenu.open((Player) event.getWhoClicked())));

        menu.setItem(15, new ItemApi(Material.CHEST, ChatColor.AQUA + "Addons",
                        List.of(ChatColor.GRAY + "Was in dieser Runde an ist")).build(),
                new SimpleItemAction(event -> AddonMenu.open((Player) event.getWhoClicked())));

        if (round != null && round.getOwnerName() != null) {
            menu.setItem(SIZE - 1, new ItemApi(Material.PAPER, ChatColor.GRAY + "Rundenadmin: "
                    + ChatColor.WHITE + round.getOwnerName()).build(), new SimpleItemAction(event -> {
            }));
        }
        return menu;
    }

    private static ItemStack startIcon(Game game) {
        List<String> lore = new ArrayList<>();
        if (!game.isWaiting()) {
            lore.add(ChatColor.DARK_GRAY + "Die Runde läuft schon.");
            return new ItemApi(Material.GRAY_DYE, ChatColor.GRAY + "Jetzt starten", lore).build();
        }
        if (!game.canStart()) {
            lore.add(ChatColor.DARK_GRAY + "Es fehlt noch eine spielbare Map.");
            return new ItemApi(Material.GRAY_DYE, ChatColor.GRAY + "Jetzt starten", lore).build();
        }
        lore.add(ChatColor.GRAY + "Ohne auf den Countdown zu warten.");
        lore.add(ChatColor.GRAY + "Spieler: " + ChatColor.WHITE + game.getOnlineCount());
        return new ItemApi(Material.LIME_CONCRETE, ChatColor.GREEN + "Jetzt starten", lore).build();
    }

    private static void forceStart(Player player, Game game) {
        if (!game.isWaiting()) {
            player.sendMessage(ChatColor.RED + "Die Runde läuft schon.");
            return;
        }
        if (!game.canStart()) {
            player.sendMessage(ChatColor.RED + "Ohne spielbare Map geht das nicht.");
            return;
        }
        Bukkit.getServer().sendMessage(net.kyori.adventure.text.Component.text(
                player.getName() + " startet die Runde.", net.kyori.adventure.text.format.NamedTextColor.GREEN));
        game.setPhase(new IngamePhase(game));
    }

    private static ItemStack visibilityIcon(RoundData round) {
        List<String> lore = new ArrayList<>();
        lore.add(round.isOpen()
                ? ChatColor.GRAY + "Die Runde steht in der Lobbyliste."
                : ChatColor.GRAY + "Nur wer eingeladen wird, kommt rein.");
        lore.add(" ");
        lore.add(ChatColor.YELLOW + "Klicken zum Umschalten");
        return new ItemApi(round.isOpen() ? Material.LIME_DYE : Material.GRAY_DYE,
                ChatColor.AQUA + (round.isOpen() ? "Öffentlich" : "Privat"), lore).build();
    }

    /**
     * Everybody who is here, one head each, click to remove.
     */
    private static void openKickList(Player admin) {
        Game game = Bedwars.getInstance().getGame();
        CustomInventory menu = new CustomInventory(9 * 4, "Spieler entfernen", null);
        menu.fillPlaceHolder();
        int slot = 0;
        for (GamePlayer participant : game.getOnlinePlayers()) {
            Player player = Bukkit.getPlayer(participant.getUuid());
            if (player == null || player.equals(admin)) continue;
            if (slot >= 9 * 3) break;
            menu.setItem(slot++, new ItemApi(Material.PLAYER_HEAD, ChatColor.AQUA + player.getName(),
                            List.of(ChatColor.GRAY + "Klicken schickt " + player.getName(),
                                    ChatColor.GRAY + "zurück in die Lobby.")).build(),
                    new SimpleItemAction(event -> {
                        Player clicker = (Player) event.getWhoClicked();
                        clicker.closeInventory();
                        if (!player.isOnline()) return;
                        RoundContext.kick(player);
                        clicker.sendMessage(ChatColor.GRAY + player.getName() + " wurde entfernt.");
                    }));
        }
        if (slot == 0) {
            menu.setItem(13, new ItemApi(Material.GRAY_DYE, ChatColor.GRAY + "Niemand sonst da").build(),
                    new SimpleItemAction(event -> {
                    }));
        }
        menu.setItem(9 * 4 - 1, new ItemApi(Material.ARROW, ChatColor.GRAY + "Zurück").build(),
                new SimpleItemAction(event -> open((Player) event.getWhoClicked())));
        CustomInventory.show(admin, menu);
    }
}

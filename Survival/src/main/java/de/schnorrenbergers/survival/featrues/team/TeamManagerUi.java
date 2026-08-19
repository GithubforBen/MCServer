package de.schnorrenbergers.survival.featrues.team;

import de.hems.api.ItemApi;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.hems.paper.util.ChatPrompt;
import de.hems.types.team.TeamData;
import de.hems.types.team.TeamSettings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The team manager players see in game.
 * <p>
 * Everything a team can be adjusted with is reachable here: members, the settings the team decides for
 * itself, its name, tag, colour, home and who leads it. The settings screen is built from
 * {@link TeamSettings.Key}, so a new setting shows up as a new button without this class being touched.
 */
public final class TeamManagerUi {

    private TeamManagerUi() {
    }

    /**
     * Opens the manager for a player.
     *
     * @param player the player, who has to be in a team
     */
    public static void open(Player player) {
        TeamManager manager = TeamManager.of(player);
        if (manager == null) {
            player.sendMessage(ChatColor.RED + "❌ Du bist in keinem Team.");
            return;
        }
        player.openInventory(main(manager, player).getInventory());
    }

    /* ------------------------------------------------------------------ main screen */

    /**
     * @param manager the team being managed
     * @param source  the player looking at it
     * @return the main screen: who is in the team, and what can be changed
     */
    private static CustomInventory main(TeamManager manager, Player source) {
        TeamData team = manager.getData();
        boolean leader = team.isLeader(source.getUniqueId());
        CustomInventory ui = new CustomInventory(54,
                ChatColor.GREEN + "Team " + team.getName(), close -> {
        });

        int slot = 0;
        for (UUID member : team.getMembers()) {
            if (slot > 26) break;
            OfflinePlayer offline = Bukkit.getOfflinePlayer(member);
            String name = offline.getName() == null ? member.toString().substring(0, 8) : offline.getName();
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + (team.isLeader(member) ? "Anführer" : "Mitglied"));
            lore.add(ChatColor.DARK_GRAY + (offline.isOnline() ? "online" : "offline"));
            if (leader && !team.isLeader(member)) lore.add(ChatColor.YELLOW + "Klicken zum Verwalten");
            // the skull constructor fills a different meta, so the lore is layered on the built head
            ItemStack head = new ItemApi(new ItemApi(name, ChatColor.WHITE + name).buildSkull(), lore).build();
            UUID target = member;
            ui.setItem(slot++, head, new SimpleItemAction(event -> {
                if (!leader || team.isLeader(target)) return;
                source.openInventory(member(manager, source, target).getInventory());
            }));
        }
        for (int free = slot; free <= 26; free++) {
            ui.setItem(free, new ItemApi(Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                    ChatColor.DARK_GRAY + "Freier Platz").build(), SimpleItemAction.display());
        }
        for (int filler = 27; filler < 45; filler++) ui.setPlaceHolder(filler);

        ui.setItem(45, info(team, manager), SimpleItemAction.display());

        if (leader) {
            ui.setItem(47, new ItemApi(Material.COMPARATOR, ChatColor.AQUA + "Einstellungen",
                            List.of(ChatColor.GRAY + "Was dein Team selbst festlegt")).build(),
                    new SimpleItemAction(event -> source.openInventory(settings(manager, source).getInventory())));
            ui.setItem(48, new ItemApi(Material.WHITE_BANNER, ChatColor.BLUE + "Farbe",
                            List.of(ChatColor.GRAY + "Aktuell: " + team.getColor())).build(),
                    new SimpleItemAction(event -> source.openInventory(colors(manager, source).getInventory())));
            ui.setItem(49, new ItemApi(Material.NAME_TAG, ChatColor.GOLD + "Name & Tag",
                            List.of(ChatColor.GRAY + "Name: " + team.getName(),
                                    ChatColor.GRAY + "Tag: " + team.getTag())).build(),
                    new SimpleItemAction(event -> source.openInventory(naming(manager, source).getInventory())));
            ui.setItem(50, new ItemApi(Material.RED_BED, ChatColor.LIGHT_PURPLE + "Team-Home",
                            List.of(ChatColor.GRAY + (team.getHome() == null ? "Nicht gesetzt" : team.getHome()),
                                    ChatColor.YELLOW + "Klicken: hier setzen")).build(),
                    new SimpleItemAction(event -> {
                        source.closeInventory();
                        manager.setHome(source);
                    }));
            ui.setItem(53, new ItemApi(Material.BARRIER, ChatColor.DARK_RED + "Team auflösen",
                            List.of(ChatColor.GRAY + "Zweimal klicken zum Bestätigen")).build(),
                    new SimpleItemAction(event -> source.openInventory(confirmDisband(manager, source).getInventory())));
        } else {
            ui.setItem(53, new ItemApi(Material.OAK_DOOR, ChatColor.RED + "Team verlassen",
                            List.of(ChatColor.GRAY + "Du verlässt dein Team")).build(),
                    new SimpleItemAction(event -> {
                        source.closeInventory();
                        manager.removePlayer(source);
                    }));
        }
        return ui;
    }

    /**
     * @param team    the team
     * @param manager the manager it belongs to
     * @return the item summarising the team
     */
    private static ItemStack info(TeamData team, TeamManager manager) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Mitglieder: " + ChatColor.WHITE
                + team.getMembers().size() + " / " + manager.getMaxPlayerAmount());
        lore.add(ChatColor.GRAY + "Chunks: " + ChatColor.WHITE + team.getClaims().size());
        lore.add(ChatColor.GRAY + "Nächster Chunk: " + ChatColor.WHITE + manager.getChunkCost());
        lore.add(ChatColor.GRAY + "Tag: " + ChatColor.WHITE + team.getTag());
        lore.add(ChatColor.GRAY + "Home: " + ChatColor.WHITE
                + (team.getHome() == null ? "keins" : team.getHome()));
        return new ItemApi(Material.BOOK, ChatColor.GREEN + team.getName(), lore).build();
    }

    /* ------------------------------------------------------------------ settings */

    /**
     * One button per setting a team has. Built from the enum, so adding a setting adds a button.
     *
     * @param manager the team being managed
     * @param source  the leader
     * @return the settings screen
     */
    private static CustomInventory settings(TeamManager manager, Player source) {
        TeamData team = manager.getData();
        CustomInventory ui = new CustomInventory(27, ChatColor.AQUA + "Team-Einstellungen", close -> {
        });
        ui.fillPlaceHolder();
        ui.setItem(0, back(), new SimpleItemAction(event ->
                source.openInventory(main(manager, source).getInventory())));

        int slot = 10;
        for (TeamSettings.Key key : TeamSettings.Key.values()) {
            if (slot > 25) break;
            if (key.getType() == TeamSettings.Key.Type.FLAG) {
                boolean on = team.getSettings().getFlag(key);
                ItemStack item = new ItemApi(on ? Material.LIME_DYE : Material.GRAY_DYE,
                        (on ? ChatColor.GREEN : ChatColor.GRAY) + key.getLabel(),
                        List.of(ChatColor.GRAY + "Status: " + (on ? ChatColor.GREEN + "an" : ChatColor.RED + "aus"),
                                ChatColor.YELLOW + "Klicken zum Umschalten")).build();
                ui.setItem(slot++, item, new SimpleItemAction(event -> {
                    manager.toggleSetting(source, key);
                    source.closeInventory();
                }));
            } else {
                int value = team.getSettings().getNumber(key);
                ItemStack item = new ItemApi(Material.COMPARATOR,
                        ChatColor.AQUA + key.getLabel(),
                        List.of(ChatColor.GRAY + "Aktuell: " + ChatColor.WHITE + value,
                                ChatColor.YELLOW + "Linksklick: +1",
                                ChatColor.YELLOW + "Rechtsklick: -1")).build();
                ui.setItem(slot++, item, new SimpleItemAction(event -> {
                    int next = event.isRightClick() ? value - 1 : value + 1;
                    manager.setNumber(source, key, next);
                    source.closeInventory();
                }));
            }
        }
        return ui;
    }

    /* ------------------------------------------------------------------ colours */

    private static CustomInventory colors(TeamManager manager, Player source) {
        CustomInventory ui = new CustomInventory(27, ChatColor.DARK_AQUA + "Teamfarbe", close -> {
        });
        ui.fillPlaceHolder();
        ui.setItem(0, back(), new SimpleItemAction(event ->
                source.openInventory(main(manager, source).getInventory())));
        int slot = 9;
        for (TeamColor color : TeamColor.values()) {
            if (slot > 26) break;
            ItemStack item = new ItemApi(Material.WHITE_WOOL,
                    color.getColor() + color.getReadableName()).build();
            ui.setItem(slot++, item, new SimpleItemAction(event -> {
                manager.setTeamColor(color, source);
                source.closeInventory();
            }));
        }
        return ui;
    }

    /* ------------------------------------------------------------------ name and tag */

    private static CustomInventory naming(TeamManager manager, Player source) {
        CustomInventory ui = new CustomInventory(27, ChatColor.GOLD + "Name & Tag", close -> {
        });
        ui.fillPlaceHolder();
        ui.setItem(0, back(), new SimpleItemAction(event ->
                source.openInventory(main(manager, source).getInventory())));
        ui.setItem(12, new ItemApi(Material.NAME_TAG, ChatColor.GOLD + "Team umbenennen",
                        List.of(ChatColor.GRAY + "Aktuell: " + manager.getName())).build(),
                new SimpleItemAction(event -> {
                    source.closeInventory();
                    ChatPrompt.ask(source, ChatColor.GOLD + "Wie soll das Team heißen? ('abbrechen' bricht ab)",
                            answer -> manager.rename(source, answer));
                }));
        ui.setItem(14, new ItemApi(Material.OAK_SIGN, ChatColor.GOLD + "Tag ändern",
                        List.of(ChatColor.GRAY + "Aktuell: " + manager.getTag())).build(),
                new SimpleItemAction(event -> {
                    source.closeInventory();
                    ChatPrompt.ask(source, ChatColor.GOLD + "Welchen Tag soll das Team haben?",
                            answer -> manager.setTag(source, answer));
                }));
        return ui;
    }

    /* ------------------------------------------------------------------ one member */

    private static CustomInventory member(TeamManager manager, Player source, UUID target) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(target);
        String name = offline.getName() == null ? target.toString() : offline.getName();
        CustomInventory ui = new CustomInventory(27, ChatColor.GREEN + name, close -> {
        });
        ui.fillPlaceHolder();
        ui.setItem(0, back(), new SimpleItemAction(event ->
                source.openInventory(main(manager, source).getInventory())));
        ui.setItem(12, new ItemApi(Material.BARRIER, ChatColor.RED + "Aus dem Team werfen").build(),
                new SimpleItemAction(event -> {
                    source.closeInventory();
                    manager.kickPlayer(source, offline);
                }));
        ui.setItem(14, new ItemApi(Material.GOLDEN_HELMET, ChatColor.GOLD + "Zum Anführer machen",
                        List.of(ChatColor.GRAY + "Du wirst normales Mitglied",
                                ChatColor.YELLOW + "Das lässt sich nicht rückgängig machen")).build(),
                new SimpleItemAction(event -> {
                    source.closeInventory();
                    manager.transferLeadership(source, target);
                }));
        return ui;
    }

    /* ------------------------------------------------------------------ disband */

    private static CustomInventory confirmDisband(TeamManager manager, Player source) {
        CustomInventory ui = new CustomInventory(27, ChatColor.DARK_RED + "Wirklich auflösen?", close -> {
        });
        ui.fillPlaceHolder();
        ui.setItem(11, new ItemApi(Material.LIME_WOOL, ChatColor.GREEN + "Abbrechen").build(),
                new SimpleItemAction(event -> source.openInventory(main(manager, source).getInventory())));
        ui.setItem(15, new ItemApi(Material.RED_WOOL, ChatColor.DARK_RED + "Ja, auflösen",
                        List.of(ChatColor.GRAY + "Claims und Rucksack gehen verloren")).build(),
                new SimpleItemAction(event -> {
                    source.closeInventory();
                    manager.disband(source);
                }));
        return ui;
    }

    private static ItemStack back() {
        return new ItemApi(Material.ARROW, ChatColor.GRAY + "Zurück").build();
    }
}

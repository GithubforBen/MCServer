package de.schnorrenbergers.lobby.rounds;

import de.hems.api.ItemApi;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.hems.paper.round.RoundService;
import de.hems.paper.round.RoundStarter;
import de.hems.types.round.RoundAddon;
import de.hems.types.round.RoundData;
import de.hems.types.round.RoundMaps;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where the person starting a round decides what it is.
 * <p>
 * Kept per player, because two people can be putting a round together at the same time and neither of them
 * should see the other's choices. Nothing here is written down until the start button is pressed - a draft
 * that was abandoned is not a round.
 */
public final class RoundCreateUi {

    private static final int SIZE = 9 * 5;
    /** Where the addon toggles start. */
    private static final int ADDON_START = 27;

    private static final Map<UUID, RoundData> drafts = new ConcurrentHashMap<>();

    private RoundCreateUi() {
    }

    /**
     * @param player who is putting a round together
     */
    public static void open(Player player) {
        drafts.computeIfAbsent(player.getUniqueId(), key -> fresh());
        CustomInventory.show(player, build(player));
    }

    /**
     * @return a round with the settings most people want
     */
    private static RoundData fresh() {
        RoundData round = new RoundData();
        round.setTeamSize(2);
        round.setAddons(RoundAddon.defaults());
        round.setOpen(true);
        List<String> maps = RoundService.getMaps();
        round.setMap(maps.isEmpty() ? null : maps.get(0));
        return round;
    }

    private static CustomInventory build(Player player) {
        RoundData draft = drafts.get(player.getUniqueId());
        CustomInventory inventory = new CustomInventory(SIZE, "Runde einrichten", null);
        inventory.fillPlaceHolder();

        inventory.setItem(10, mapIcon(draft), new SimpleItemAction(event -> {
            nextMap(draft);
            open((Player) event.getWhoClicked());
        }));
        inventory.setItem(12, modeIcon(draft), new SimpleItemAction(event -> {
            draft.setTeamSize(draft.getTeamSize() >= 4 ? 1 : draft.getTeamSize() + 1);
            open((Player) event.getWhoClicked());
        }));
        inventory.setItem(14, visibilityIcon(draft), new SimpleItemAction(event -> {
            draft.setOpen(!draft.isOpen());
            open((Player) event.getWhoClicked());
        }));
        inventory.setItem(16, new ItemApi(Material.PAPER, ChatColor.AQUA + "Rundenadmin",
                        List.of(ChatColor.GRAY + "Wer startet, ist Rundenadmin.",
                                ChatColor.GRAY + "Du kannst in der Wartelobby",
                                ChatColor.GRAY + "Spieler kicken, die Runde vorzeitig",
                                ChatColor.GRAY + "starten und sie privat schalten.")).build(),
                new SimpleItemAction(event -> {
                }));

        RoundAddon[] addons = RoundAddon.values();
        for (int i = 0; i < addons.length && ADDON_START + i < SIZE - 9; i++) {
            RoundAddon addon = addons[i];
            inventory.setItem(ADDON_START + i, addonIcon(addon, draft), new SimpleItemAction(event -> {
                toggle(draft, addon);
                open((Player) event.getWhoClicked());
            }));
        }

        inventory.setItem(SIZE - 9, new ItemApi(Material.BARRIER, ChatColor.GRAY + "Zurück").build(),
                new SimpleItemAction(event -> RoundBrowserUi.open((Player) event.getWhoClicked())));
        inventory.setItem(SIZE - 1, new ItemApi(Material.LIME_CONCRETE, ChatColor.GREEN + "Runde starten",
                        List.of(ChatColor.GRAY + "Der Server wird hochgefahren",
                                ChatColor.GRAY + "und du kommst direkt hin.")).build(),
                new SimpleItemAction(event -> {
                    Player clicker = (Player) event.getWhoClicked();
                    clicker.closeInventory();
                    drafts.remove(clicker.getUniqueId());
                    RoundStarter.start(clicker, draft);
                }));
        return inventory;
    }

    /**
     * Steps to the next map, wrapping around at the end.
     */
    private static void nextMap(RoundData draft) {
        List<String> maps = RoundService.getMaps();
        if (maps.isEmpty()) return;
        int index = maps.indexOf(draft.getMap());
        draft.setMap(maps.get((index + 1) % maps.size()));
    }

    private static ItemStack mapIcon(RoundData draft) {
        List<String> lore = new ArrayList<>();
        List<String> maps = RoundService.getMaps();
        for (String map : maps) {
            boolean chosen = map.equals(draft.getMap());
            lore.add((chosen ? ChatColor.GREEN + "» " : ChatColor.DARK_GRAY + "  ")
                    + RoundMaps.displayName(map));
        }
        if (maps.size() <= 1) {
            lore.add(" ");
            lore.add(ChatColor.DARK_GRAY + "Mehr Maps: Weltordner nach");
            lore.add(ChatColor.DARK_GRAY + "./bedwars-maps beim Launcher legen.");
        } else {
            lore.add(" ");
            lore.add(ChatColor.YELLOW + "Klicken für die nächste Map");
        }
        return new ItemApi(Material.FILLED_MAP, ChatColor.AQUA + "Map: "
                + RoundMaps.displayName(draft.getMap()), lore).build();
    }

    private static ItemStack modeIcon(RoundData draft) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Spieler pro Team: " + ChatColor.WHITE + draft.getTeamSize());
        lore.add(" ");
        lore.add(ChatColor.YELLOW + "Klicken für den nächsten Modus");
        return new ItemApi(Material.IRON_SWORD, ChatColor.AQUA + "Modus: " + modeName(draft), lore).build();
    }

    private static String modeName(RoundData draft) {
        return switch (draft.getTeamSize()) {
            case 1 -> "Solo";
            case 2 -> "Doubles";
            case 3 -> "Trio";
            default -> "Quad";
        };
    }

    private static ItemStack visibilityIcon(RoundData draft) {
        List<String> lore = new ArrayList<>();
        lore.add(draft.isOpen()
                ? ChatColor.GRAY + "Jeder sieht die Runde in der Liste."
                : ChatColor.GRAY + "Nur du siehst sie - nimm deine Leute mit.");
        lore.add(" ");
        lore.add(ChatColor.YELLOW + "Klicken zum Umschalten");
        return new ItemApi(draft.isOpen() ? Material.LIME_DYE : Material.GRAY_DYE,
                ChatColor.AQUA + (draft.isOpen() ? "Öffentlich" : "Privat"), lore).build();
    }

    private static ItemStack addonIcon(RoundAddon addon, RoundData draft) {
        boolean on = draft.getAddons().contains(addon.getId());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + addon.getDescription());
        lore.add(" ");
        lore.add(on ? ChatColor.GREEN + "An" : ChatColor.DARK_GRAY + "Aus");
        lore.add(ChatColor.YELLOW + "Klicken zum Umschalten");
        ItemStack icon = new ItemApi(on ? addon.getIcon() : Material.GRAY_DYE,
                (on ? ChatColor.GREEN : ChatColor.GRAY) + addon.getDisplayName(), lore).build();
        return icon;
    }

    private static void toggle(RoundData draft, RoundAddon addon) {
        Set<String> addons = new LinkedHashSet<>(draft.getAddons());
        if (!addons.remove(addon.getId())) addons.add(addon.getId());
        draft.setAddons(addons);
    }
}

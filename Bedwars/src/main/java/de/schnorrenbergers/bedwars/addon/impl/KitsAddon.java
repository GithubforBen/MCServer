package de.schnorrenbergers.bedwars.addon.impl;

import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.addon.AddonConfig;
import de.schnorrenbergers.bedwars.addon.AddonSettings;
import de.schnorrenbergers.bedwars.addon.ListeningAddon;
import de.schnorrenbergers.bedwars.api.BedwarsGameStateChangeEvent;
import de.schnorrenbergers.bedwars.api.BedwarsPlayerRespawnEvent;
import de.schnorrenbergers.bedwars.game.Equipment;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.phase.PhaseType;
import de.schnorrenbergers.bedwars.util.ConfigFile;
import de.schnorrenbergers.bedwars.util.Messages;
import de.schnorrenbergers.bedwars.util.Registries;
import de.schnorrenbergers.bedwars.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Kits: a little extra to start with, picked in the waiting lobby.
 * <p>
 * A kit is deliberately small - a handful of items and one passive effect - because a bedwars round is
 * decided by the shop, and a kit that could decide it instead would turn the choice in the lobby into the
 * whole game. What it does change is how the first two minutes feel, which is exactly where a round is
 * otherwise identical every time.
 * <p>
 * The pick survives death: it is handed out again at every respawn, or a kit would be worth nothing to
 * whoever dies first.
 */
public final class KitsAddon extends ListeningAddon {

    public static final String ID = "kits";

    /** Marks the lobby item that opens the kit menu. */
    private static final NamespacedKey ITEM_KEY = new NamespacedKey("bedwars", "kit-item");

    /**
     * One kit out of {@code kits.yml}.
     *
     * @param id          how it is referred to
     * @param displayName what it is called, MiniMessage
     * @param icon        what it is drawn as
     * @param lore        what the menu says about it
     * @param items       what it hands out
     * @param perk        the effect its owner carries all round, or {@code null} for none
     * @param amplifier   how strong that effect is
     */
    public record Kit(String id, String displayName, Material icon, List<String> lore,
                      List<ItemStack> items, @Nullable PotionEffectType perk, int amplifier) {
    }

    private final AddonConfig config;
    private final Map<String, Kit> kits = new LinkedHashMap<>();
    /** Who picked what. By uuid, so a reconnect keeps the choice. */
    private final Map<UUID, String> chosen = new HashMap<>();

    private ConfigFile file;
    private int menuSlot;
    private String defaultKit;

    public KitsAddon(Plugin plugin, AddonSettings settings) {
        super(plugin);
        this.config = new AddonConfig(settings, ID);
        menuSlot = Math.max(0, Math.min(8, config.get("menu-slot", 1,
                "Where the kit chooser sits in the lobby hotbar. The team wool is slot 0.")));
        defaultKit = config.get("default", "",
                "Which kit somebody who never picked one plays. Empty means: no kit at all.");
        config.save();
        load();
    }

    /**
     * Reads {@code kits.yml}, writing four kits into it the first time.
     */
    public void load() {
        file = new ConfigFile("kits.yml");
        kits.clear();
        file.section("kits",
                "One block per kit. 'items' are written as MATERIAL:AMOUNT, optionally followed by",
                "enchantments as ENCHANT/LEVEL separated by commas, e.g. IRON_SWORD:1:sharpness/1.",
                "'perk' is a potion effect its owner carries for the whole round - keep it small, the",
                "shop is what should decide a round.");
        define("warrior", "<red>Warrior", Material.STONE_SWORD,
                "<gray>A better blade, and a thicker skin.",
                List.of("STONE_SWORD:1"), "resistance", 0);
        define("runner", "<aqua>Runner", Material.FEATHER,
                "<gray>Always a step ahead.",
                List.of(), "speed", 0);
        define("builder", "<yellow>Builder", Material.OAK_PLANKS,
                "<gray>Wood to start with, and hands that work faster.",
                List.of("OAK_PLANKS:16", "SHEARS:1"), "haste", 0);
        define("archer", "<green>Archer", Material.BOW,
                "<gray>A bow, and enough to make it count.",
                List.of("BOW:1:power/1", "ARROW:8"), "", 0);
        read();
        file.save();
    }

    private void define(String id, String displayName, Material icon, String lore,
                        List<String> items, String perk, int amplifier) {
        String path = "kits." + id;
        file.get(path + ".display-name", displayName);
        file.get(path + ".icon", icon.name());
        if (!file.contains(path + ".lore")) file.set(path + ".lore", List.of(lore));
        if (!file.contains(path + ".items")) file.set(path + ".items", items);
        file.get(path + ".perk", perk);
        file.get(path + ".perk-amplifier", amplifier);
    }

    private void read() {
        for (String id : file.keys("kits")) {
            String path = "kits." + id;
            Material icon = Material.matchMaterial(file.read(path + ".icon", Material.CHEST.name()));
            List<ItemStack> items = new ArrayList<>();
            for (String line : file.raw().getStringList(path + ".items")) {
                ItemStack stack = item(line, id);
                if (stack != null) items.add(stack);
            }
            kits.put(id, new Kit(id,
                    file.read(path + ".display-name", id),
                    icon == null ? Material.CHEST : icon,
                    List.copyOf(file.raw().getStringList(path + ".lore")),
                    List.copyOf(items),
                    Registries.effect(file.read(path + ".perk", "")),
                    Math.max(0, file.read(path + ".perk-amplifier", 0))));
        }
    }

    /**
     * @param line one entry of a kit's item list
     * @param kit  which kit, for the warning
     * @return the item, or {@code null} when the line names something this server does not have
     */
    private static @Nullable ItemStack item(String line, String kit) {
        String[] parts = line.split(":");
        Material material = Material.matchMaterial(parts[0].trim().toUpperCase(Locale.ROOT));
        if (material == null) {
            Bukkit.getLogger().warning("[Bedwars] kits.yml: the kit '" + kit + "' asks for '" + parts[0]
                    + "', which is not a material. It is skipped.");
            return null;
        }
        ItemStack stack = new ItemStack(material, parts.length > 1 ? number(parts[1], 1) : 1);
        if (parts.length > 2) enchant(stack, parts[2], kit);
        return stack;
    }

    private static void enchant(ItemStack stack, String written, String kit) {
        for (String pair : written.split(",")) {
            String[] parts = pair.split("/");
            Enchantment enchantment = Registries.enchantment(parts[0]);
            if (enchantment == null) {
                Bukkit.getLogger().warning("[Bedwars] kits.yml: the kit '" + kit + "' asks for the"
                        + " enchantment '" + parts[0] + "', which does not exist.");
                continue;
            }
            stack.addUnsafeEnchantment(enchantment, parts.length > 1 ? number(parts[1], 1) : 1);
        }
    }

    private static int number(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public void reload() {
        load();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDescription() {
        return "A small starting kit with one passive perk, picked in the waiting lobby";
    }

    @Override
    protected void onEnable(Game game) {
        if (!game.isWaiting()) return;
        for (Player player : Bukkit.getOnlinePlayers()) giveChooser(player);
    }

    @Override
    protected void onDisable(Game game) {
        chosen.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack held = player.getInventory().getItem(menuSlot);
            if (isChooser(held)) player.getInventory().setItem(menuSlot, null);
        }
    }

    // ------------------------------------------------------------------ choosing

    /**
     * Puts the chooser into the lobby hotbar. Runs after the lobby has laid the inventory out, or it would
     * be the inventory that is cleared a moment later.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Game game = Bedwars.getInstance().getGame();
        if (game == null || !game.isWaiting() || game.isSetupMode()) return;
        Bukkit.getScheduler().runTask(getPlugin(), () -> {
            if (event.getPlayer().isOnline()) giveChooser(event.getPlayer());
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!isChooser(event.getItem())) return;
        event.setCancelled(true);
        Game game = Bedwars.getInstance().getGame();
        if (game != null && game.isWaiting()) openMenu(event.getPlayer());
    }

    private void giveChooser(Player player) {
        if (kits.isEmpty()) return;
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Messages.get("kit.item").decoration(
                    net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            meta.getPersistentDataContainer().set(ITEM_KEY, PersistentDataType.STRING, ID);
            item.setItemMeta(meta);
        }
        player.getInventory().setItem(menuSlot, item);
    }

    private static boolean isChooser(@Nullable ItemStack stack) {
        if (stack == null || stack.getItemMeta() == null) return false;
        return ID.equals(stack.getItemMeta().getPersistentDataContainer()
                .get(ITEM_KEY, PersistentDataType.STRING));
    }

    /**
     * Opens the picker: one icon per kit, the one that is picked marked as such.
     */
    private void openMenu(Player player) {
        CustomInventory menu = new CustomInventory(9 * ((kits.size() - 1) / 9 + 1),
                Text.legacy(Messages.get("kit.title")), null);
        menu.fillPlaceHolder();
        int slot = 0;
        for (Kit kit : kits.values()) {
            menu.setItem(slot++, icon(kit, kit.id().equals(chosen.get(player.getUniqueId()))),
                    new SimpleItemAction(event -> choose(player, kit)));
        }
        CustomInventory.show(player, menu);
    }

    private ItemStack icon(Kit kit, boolean picked) {
        ItemStack stack = new ItemStack(kit.icon());
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        meta.displayName(Text.item(kit.displayName()));
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        kit.lore().forEach(line -> lore.add(Text.item(line)));
        if (kit.perk() != null) {
            lore.add(Messages.get("kit.perk",
                    "perk", Text.niceName(kit.perk().getKey().getKey()),
                    "level", Text.roman(kit.amplifier() + 1)));
        }
        lore.add(Messages.get(picked ? "kit.picked" : "kit.click"));
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private void choose(Player player, Kit kit) {
        chosen.put(player.getUniqueId(), kit.id());
        Messages.send(player, "kit.chosen", "kit", Text.plain(kit.displayName()));
        player.playSound(player, Sound.UI_BUTTON_CLICK, 1.0f, 1.4f);
        // the menu stays open, so the tick has to move to the kit that was just picked
        openMenu(player);
    }

    // ------------------------------------------------------------------- handing out

    /**
     * Hands every kit out when the round begins.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onStateChange(BedwarsGameStateChangeEvent event) {
        if (event.getTo() != PhaseType.RUNNING) return;
        Game game = event.getGame();
        // a tick later: the running phase is in the middle of giving everybody their starting kit, and a
        // kit handed out before that is a kit that gets cleared again
        Bukkit.getScheduler().runTask(getPlugin(), () -> {
            for (GamePlayer participant : game.getPlayers()) {
                if (participant.isPlaying()) apply(participant);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(BedwarsPlayerRespawnEvent event) {
        GamePlayer participant = event.getPlayer();
        Bukkit.getScheduler().runTask(getPlugin(), () -> apply(participant));
    }

    /**
     * Gives one player their kit, or nothing when they never picked one and there is no default.
     */
    private void apply(GamePlayer participant) {
        Player player = participant.getPlayer();
        if (player == null || !participant.isAlive()) return;
        Kit kit = kitOf(participant);
        if (kit == null) return;
        for (ItemStack stack : kit.items()) {
            player.getInventory().addItem(stack.clone()).values()
                    .forEach(rest -> player.getWorld().dropItem(player.getLocation(), rest));
        }
        // a kit that hands out a sword hands out a better one than the starting kit did a tick earlier,
        // and both of them stayed: the warrior walked into the round carrying two
        Equipment.dropWoodenSword(player);
        if (kit.perk() != null) {
            player.addPotionEffect(new PotionEffect(kit.perk(), PotionEffect.INFINITE_DURATION,
                    kit.amplifier(), false, false, true));
        }
    }

    /**
     * @param participant somebody in the round
     * @return the kit they play, or {@code null} when they have none
     */
    public @Nullable Kit kitOf(GamePlayer participant) {
        String id = chosen.getOrDefault(participant.getUuid(), defaultKit);
        return id == null || id.isBlank() ? null : kits.get(id);
    }

    public Map<String, Kit> getKits() {
        return Map.copyOf(kits);
    }
}

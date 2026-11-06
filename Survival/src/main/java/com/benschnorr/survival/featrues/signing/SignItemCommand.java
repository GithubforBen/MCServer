package com.benschnorr.survival.featrues.signing;

import com.benschnorr.paper.cosmetic.CosmeticSafetyListener;
import com.benschnorr.paper.cosmetic.GadgetItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * {@code /signItem}: signs the whole stack in the main hand with the player's UUID and one lore line.
 * No timestamp, so signings by one player are identical and stack. The rules live in {@link SigningRules}.
 */
public final class SignItemCommand implements CommandExecutor {

    /** The signer's UUID in the item's data. */
    public static final NamespacedKey SIGNED_BY = new NamespacedKey("survival", "signed_by");

    /** Menu items mark themselves with this key in {@code CustomInventory}, which keeps it private. */
    private static final NamespacedKey MENU_ITEM = new NamespacedKey("survival", "id");

    private static final ChatColor ERROR = ChatColor.RED;
    private static final ChatColor TEXT = ChatColor.GRAY;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ERROR + "Nur Spieler können Items signieren.");
            return true;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        boolean empty = hand.getType().isAir();
        UUID signer = empty ? null : signerOf(hand);
        SigningRules.Outcome outcome = SigningRules.decide(empty, empty || signable(hand), signer, player.getUniqueId());
        switch (outcome) {
            case EMPTY_HAND -> player.sendMessage(ERROR + "Halte das Item, das du signieren willst, in der Hand.");
            case NOT_SIGNABLE -> player.sendMessage(ERROR + "Dieses Item gehört zum Netzwerk und kann nicht signiert werden.");
            case ALREADY_YOURS -> player.sendMessage(TEXT + "Dieses Item hast du schon signiert.");
            case SIGNED_BY_OTHER -> player.sendMessage(ERROR + "Dieses Item hat schon " + nameOf(signer)
                    + " signiert. Eine Signatur ist endgültig.");
            case SIGN -> {
                sign(hand, player);
                player.getInventory().setItemInMainHand(hand);
                player.sendMessage(TEXT + "Du hast " + hand.getAmount() + " Item" + (hand.getAmount() == 1 ? "" : "s")
                        + " signiert.");
            }
        }
        return true;
    }

    /** Writes the signature and the lore line; the same call for the same player gives the same item. */
    static void sign(ItemStack item, Player player) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(SIGNED_BY, PersistentDataType.STRING, player.getUniqueId().toString());
        List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        String line = ChatColor.GRAY + SigningRules.loreLine(player.getName());
        if (!lore.contains(line)) lore.add(line);
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    /** @return who signed the item, or null if nobody did */
    public static @Nullable UUID signerOf(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        String raw = meta.getPersistentDataContainer().get(SIGNED_BY, PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Items that carry a function of the network: gadgets, cosmetic items and menu items. */
    static boolean signable(ItemStack item) {
        if (GadgetItems.isAny(item)) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return true;
        PersistentDataContainer data = meta.getPersistentDataContainer();
        return !data.has(CosmeticSafetyListener.COSMETIC_KEY) && !data.has(MENU_ITEM);
    }

    private static String nameOf(UUID id) {
        OfflinePlayer other = Bukkit.getOfflinePlayer(id);
        return other.getName() != null ? other.getName() : id.toString();
    }
}

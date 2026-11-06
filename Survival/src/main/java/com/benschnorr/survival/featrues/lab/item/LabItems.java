package com.benschnorr.survival.featrues.lab.item;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds and reads lab items. Name, description, purity, effects and kind are written in the item, the look
 * comes from the {@code item_model} component, so no resource pack is needed.
 */
public final class LabItems {

    private static final NamespacedKey KIND = new NamespacedKey("survival", "lab_kind");
    private static final NamespacedKey PURITY = new NamespacedKey("survival", "lab_purity");
    private static final NamespacedKey EFFECTS = new NamespacedKey("survival", "lab_effects");
    private static final NamespacedKey STRETCHED = new NamespacedKey("survival", "lab_stretched");

    private LabItems() {
    }

    public static ItemStack create(LabData data) {
        return create(data, 1);
    }

    public static ItemStack create(LabData data, int amount) {
        ItemStack item = new ItemStack(LabMaterials.of(data.kind()), amount);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plain(LabText.title(data), colorOf(data)));
        meta.lore(LabText.lore(data).stream().map(line -> plain(line, NamedTextColor.GRAY)).toList());
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KIND, PersistentDataType.STRING, data.kind().name());
        if (data.kind().hasPurity()) pdc.set(PURITY, PersistentDataType.INTEGER, data.purity());
        if (!data.effects().isEmpty()) {
            pdc.set(EFFECTS, PersistentDataType.STRING,
                    data.effects().stream().map(Enum::name).collect(Collectors.joining(",")));
        }
        if (data.stretched()) pdc.set(STRETCHED, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        item.setData(DataComponentTypes.ITEM_MODEL, Key.key(LabMaterials.modelOf(data.kind())));
        return item;
    }

    /** The content of a lab item, or empty for anything else. */
    public static Optional<LabData> read(ItemStack item) {
        if (item == null || item.isEmpty() || !item.hasItemMeta()) return Optional.empty();
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String kindName = pdc.get(KIND, PersistentDataType.STRING);
        if (kindName == null) return Optional.empty();
        try {
            LabKind kind = LabKind.valueOf(kindName);
            if (item.getType() != LabMaterials.of(kind)) return Optional.empty();
            int purity = pdc.getOrDefault(PURITY, PersistentDataType.INTEGER, 0);
            Set<LabEffect> effects = EnumSet.noneOf(LabEffect.class);
            String stored = pdc.get(EFFECTS, PersistentDataType.STRING);
            if (stored != null && !stored.isEmpty()) {
                for (String name : stored.split(",")) effects.add(LabEffect.valueOf(name));
            }
            boolean stretched = pdc.has(STRETCHED, PersistentDataType.BYTE);
            return Optional.of(new LabData(kind, purity, effects, stretched));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static boolean isKind(ItemStack item, LabKind kind) {
        return read(item).map(data -> data.kind() == kind).orElse(false);
    }

    private static Component plain(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private static NamedTextColor colorOf(LabData data) {
        if (data.kind() == LabKind.PFLANZENABFALL) return NamedTextColor.DARK_GRAY;
        if (!data.kind().hasPurity()) return NamedTextColor.AQUA;
        return switch (data.purity()) {
            case 1 -> NamedTextColor.GRAY;
            case 2 -> NamedTextColor.WHITE;
            case 3 -> NamedTextColor.YELLOW;
            case 4 -> NamedTextColor.GREEN;
            default -> NamedTextColor.LIGHT_PURPLE;
        };
    }
}

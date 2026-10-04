package de.hems.paper.item;

import de.hems.types.item.ItemCatalog;
import de.hems.types.item.ItemSpec;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Turns {@link ItemSpec}s into real items and back, on a game server.
 * <p>
 * Everything bukkit specific about hand made items lives here, so an event prize and a slot edited on the
 * website end up as exactly the same item. Names the server does not know - an enchantment of a newer
 * version, a typo that got past the launcher - are left out with a warning rather than failing the whole
 * item, because a prize without one enchantment is still better than no prize.
 */
public final class ItemSpecs {

    /** Where the modifiers made here keep their keys, so they never collide with another plugin's. */
    private static final String NAMESPACE = "hems";
    /** {@code &6} and {@code &#ff8800} both, the way the item editor writes colours. */
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&').hexColors().build();

    private ItemSpecs() {
    }

    /* ------------------------------------------------------------------------------------ building */

    /**
     * @param spec the item
     * @return it as one stack, holding at most what one stack can, or {@code null} if the material is unknown
     */
    public static ItemStack build(ItemSpec spec) {
        Material material = material(spec);
        if (material == null) return null;
        ItemStack stack = new ItemStack(material, Math.max(1, Math.min(material.getMaxStackSize(), spec.getAmount())));
        return apply(stack, spec);
    }

    /**
     * @param spec the item, possibly more than fits one stack
     * @return as many stacks as it takes, empty if the material is unknown
     */
    public static List<ItemStack> stacks(ItemSpec spec) {
        List<ItemStack> stacks = new ArrayList<>();
        Material material = material(spec);
        if (material == null) return stacks;
        ItemStack one = apply(new ItemStack(material, 1), spec);
        int left = spec.getAmount();
        while (left > 0) {
            int amount = Math.min(left, one.getMaxStackSize());
            ItemStack stack = one.clone();
            stack.setAmount(amount);
            stacks.add(stack);
            left -= amount;
        }
        return stacks;
    }

    /**
     * Gives an existing item the name, lore, enchantments, modifiers and wear an item description asks for.
     * Everything the description does not cover - plugin data, a banner pattern, a potion - stays as it was.
     * So does every part the description describes exactly as the item already has it: a name with a hover
     * text reads back as plain colours, and writing that back would lose the hover for nothing.
     * The amount is the caller's business.
     *
     * @param stack the item to change, it is changed in place
     * @param spec  what it should be like
     * @return the same stack
     */
    public static ItemStack apply(ItemStack stack, ItemSpec spec) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        ItemSpec current = read(stack);

        if (!Objects.equals(current.getName(), spec.getName())) {
            meta.displayName(spec.getName() == null ? null : text(spec.getName()));
        }
        if (!current.getLore().equals(spec.getLore())) {
            if (spec.getLore().isEmpty()) {
                meta.lore(null);
            } else {
                List<Component> lore = new ArrayList<>();
                for (String line : spec.getLore()) lore.add(text(line == null ? "" : line));
                meta.lore(lore);
            }
        }
        if (!current.getEnchantments().equals(spec.getEnchantments())) enchant(meta, spec);
        if (!current.getAttributes().equals(spec.getAttributes())) modify(meta, spec);

        meta.setUnbreakable(spec.isUnbreakable());
        if (meta instanceof Damageable damageable && stack.getType().getMaxDurability() > 0) {
            int damage = Math.min(spec.getDamage(), stack.getType().getMaxDurability() - 1);
            if (damage > 0) {
                damageable.setDamage(damage);
            } else {
                damageable.resetDamage();
            }
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private static void enchant(ItemMeta meta, ItemSpec spec) {
        // a book keeps its enchantments to be put on something else, everything else wears them itself
        if (meta instanceof EnchantmentStorageMeta book) {
            for (Enchantment existing : new ArrayList<>(book.getStoredEnchants().keySet())) {
                book.removeStoredEnchant(existing);
            }
        }
        meta.removeEnchantments();
        for (Map.Entry<String, Integer> entry : spec.getEnchantments().entrySet()) {
            Enchantment enchantment = enchantment(entry.getKey());
            if (enchantment == null) {
                Bukkit.getLogger().warning("Unknown enchantment on a hand made item: " + entry.getKey());
                continue;
            }
            if (meta instanceof EnchantmentStorageMeta book) {
                book.addStoredEnchant(enchantment, entry.getValue(), true);
            } else {
                meta.addEnchant(enchantment, entry.getValue(), true);
            }
        }
    }

    private static void modify(ItemMeta meta, ItemSpec spec) {
        meta.setAttributeModifiers(null);
        int index = 0;
        for (ItemSpec.Modifier modifier : spec.getAttributes()) {
            Attribute attribute = attribute(modifier.getAttribute());
            EquipmentSlotGroup slot = EquipmentSlotGroup.getByName(modifier.getSlot());
            if (attribute == null || slot == null) {
                Bukkit.getLogger().warning("Unknown attribute or slot on a hand made item: "
                        + modifier.getAttribute() + " / " + modifier.getSlot());
                continue;
            }
            NamespacedKey key = new NamespacedKey(NAMESPACE, "item_" + index++ + "_"
                    + attribute.getKey().getKey().replace('.', '_'));
            meta.addAttributeModifier(attribute, new AttributeModifier(key, modifier.getAmount(),
                    operation(modifier.getOperation()), slot));
        }
    }

    /* ------------------------------------------------------------------------------------- reading */

    /**
     * @param stack a real item
     * @return what it is, in the form the website edits - {@code null} for nothing
     */
    public static ItemSpec read(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return null;
        ItemSpec spec = new ItemSpec(stack.getType().name(), stack.getAmount());
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return spec;
        if (meta.hasCustomName() && meta.customName() != null) spec.setName(LEGACY.serialize(meta.customName()));
        List<Component> lore = meta.lore();
        if (lore != null) {
            List<String> lines = new ArrayList<>();
            for (Component line : lore) lines.add(LEGACY.serialize(line));
            spec.setLore(lines);
        }
        Map<Enchantment, Integer> enchantments = meta instanceof EnchantmentStorageMeta book
                ? book.getStoredEnchants() : meta.getEnchants();
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            spec.withEnchantment(entry.getKey().getKey().toString(), entry.getValue());
        }
        if (meta.hasAttributeModifiers() && meta.getAttributeModifiers() != null) {
            for (Map.Entry<Attribute, AttributeModifier> entry : meta.getAttributeModifiers().entries()) {
                AttributeModifier modifier = entry.getValue();
                spec.withAttribute(new ItemSpec.Modifier(entry.getKey().getKey().toString(), modifier.getAmount(),
                        ItemSpec.Operation.byName(modifier.getOperation().name()),
                        String.valueOf(modifier.getSlotGroup()).toLowerCase(Locale.ROOT)));
            }
        }
        spec.setUnbreakable(meta.isUnbreakable());
        if (meta instanceof Damageable damageable && damageable.hasDamage()) spec.setDamage(damageable.getDamage());
        return spec;
    }

    /* ------------------------------------------------------------------------------------- catalog */

    /**
     * @return everything an item can be made of on this server
     */
    public static ItemCatalog catalog() {
        List<ItemCatalog.Material> materials = new ArrayList<>();
        List<Material> items = new ArrayList<>();
        for (Material material : Material.values()) {
            if (material.isLegacy() || !material.isItem() || material.isAir()) continue;
            items.add(material);
            materials.add(new ItemCatalog.Material(material.name(), material.isBlock(), material.getMaxStackSize(),
                    material.getMaxDurability()));
        }
        materials.sort((a, b) -> a.name().compareTo(b.name()));

        List<ItemCatalog.Enchantment> enchantments = new ArrayList<>();
        Registry.ENCHANTMENT.stream().forEach(enchantment -> {
            List<String> on = new ArrayList<>();
            for (Material material : items) {
                try {
                    if (enchantment.canEnchantItem(new ItemStack(material))) on.add(material.name());
                } catch (RuntimeException ignored) {
                    // an item that cannot even be asked is not one to suggest it for
                }
            }
            enchantments.add(new ItemCatalog.Enchantment(enchantment.getKey().toString(),
                    enchantment.getMaxLevel(), on));
        });
        enchantments.sort((a, b) -> a.key().compareTo(b.key()));

        List<String> attributes = new ArrayList<>();
        Registry.ATTRIBUTE.stream().forEach(attribute -> attributes.add(attribute.getKey().toString()));
        attributes.sort(String::compareTo);
        return new ItemCatalog(materials, enchantments, attributes, System.currentTimeMillis());
    }

    /* -------------------------------------------------------------------------------------- lookup */

    private static Material material(ItemSpec spec) {
        if (spec == null || spec.getMaterial() == null) return null;
        Material material = Material.matchMaterial(spec.getMaterial());
        if (material == null || material.isAir() || !material.isItem()) {
            Bukkit.getLogger().warning("Unknown item material: " + spec.getMaterial());
            return null;
        }
        return material;
    }

    private static Enchantment enchantment(String key) {
        NamespacedKey namespaced = NamespacedKey.fromString(key);
        return namespaced == null ? null : Registry.ENCHANTMENT.get(namespaced);
    }

    private static Attribute attribute(String key) {
        NamespacedKey namespaced = NamespacedKey.fromString(key);
        return namespaced == null ? null : Registry.ATTRIBUTE.get(namespaced);
    }

    private static AttributeModifier.Operation operation(ItemSpec.Operation operation) {
        return switch (operation) {
            case ADD_NUMBER -> AttributeModifier.Operation.ADD_NUMBER;
            case ADD_SCALAR -> AttributeModifier.Operation.ADD_SCALAR;
            case MULTIPLY_SCALAR_1 -> AttributeModifier.Operation.MULTIPLY_SCALAR_1;
        };
    }

    /**
     * @param legacy text with {@code &} colour codes
     * @return it as a component that is not italic unless it says so - minecraft slants every custom name
     *         and lore line otherwise, which nobody typing a name expects
     */
    private static Component text(String legacy) {
        return LEGACY.deserialize(legacy).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /**
     * @param specs items
     * @return them as stacks, split where they are larger than one stack
     */
    public static List<ItemStack> stacks(Collection<ItemSpec> specs) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemSpec spec : specs) stacks.addAll(stacks(spec));
        return stacks;
    }
}

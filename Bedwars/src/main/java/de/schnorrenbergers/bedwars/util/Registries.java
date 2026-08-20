package de.schnorrenbergers.bedwars.util;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Looking things up by the name a config wrote.
 * <p>
 * Enchantments and effects stopped being enum constants and became registry entries, so every config that
 * names one has to go through the registry. That lookup is the same three lines everywhere, and having it
 * in one place is what keeps the next one from being written slightly differently.
 */
public final class Registries {

    private Registries() {
    }

    /**
     * @param name an enchantment as a config wrote it, e.g. {@code sharpness}
     * @return it, or {@code null} when this server has none by that name
     */
    public static @Nullable Enchantment enchantment(String name) {
        if (name == null || name.isBlank()) return null;
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(key(name));
    }

    /**
     * @param name a potion effect as a config wrote it, e.g. {@code speed}
     * @return it, or {@code null} when this server has none by that name
     */
    public static @Nullable PotionEffectType effect(String name) {
        if (name == null || name.isBlank()) return null;
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.MOB_EFFECT).get(key(name));
    }

    private static NamespacedKey key(String name) {
        return NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT).trim());
    }
}

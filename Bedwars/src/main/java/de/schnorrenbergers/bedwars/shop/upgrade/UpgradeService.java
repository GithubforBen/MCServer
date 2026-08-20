package de.schnorrenbergers.bedwars.shop.upgrade;

import de.schnorrenbergers.bedwars.api.BedwarsUpgradeEvent;
import de.schnorrenbergers.bedwars.config.UpgradeSettings;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.generator.GeneratorManager;
import de.schnorrenbergers.bedwars.map.TeamSpot;
import de.schnorrenbergers.bedwars.util.Messages;
import de.schnorrenbergers.bedwars.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

/**
 * The team upgrades: buying them, and keeping them true afterwards.
 * <p>
 * An upgrade is a number on the team, not an item in an inventory - which means it has to be put back onto
 * the players whenever their inventory changes: after a purchase, after a respawn, and for anybody who
 * joins the team late. {@link #applyTo(Player, GameTeam)} is that one place.
 */
public class UpgradeService {

    /** How long the heal pool effect is handed out for; refreshed while a player stands in their base. */
    private static final int HEAL_TICKS = 60;

    private final UpgradeSettings settings;

    public UpgradeService(UpgradeSettings settings) {
        this.settings = settings;
    }

    public UpgradeSettings getSettings() {
        return settings;
    }

    // -------------------------------------------------------------------- buying

    /**
     * Buys the next level of an upgrade for the buyer's team.
     *
     * @param game    the round
     * @param buyer   who pays
     * @param upgrade what they picked
     * @return whether it worked
     */
    public boolean buy(Game game, GamePlayer buyer, Upgrade upgrade) {
        Player player = buyer.getPlayer();
        GameTeam team = buyer.getTeam();
        if (player == null || team == null || !buyer.isAlive()) return false;

        int level = team.getUpgradeLevel(upgrade.id()) + 1;
        if (level > upgrade.maxLevel()) {
            Messages.send(player, "upgrade.maxed", "upgrade", Text.plain(upgrade.displayName()));
            return false;
        }
        BedwarsUpgradeEvent event = new BedwarsUpgradeEvent(game, team, buyer, upgrade.id(), level);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        int price = upgrade.priceFor(level);
        if (!upgrade.currency().take(player, price)) {
            Messages.send(player, "shop.cannot-afford",
                    "amount", String.valueOf(price - upgrade.currency().count(player)),
                    "currency", upgrade.currency().getDisplayName());
            player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }
        team.setUpgradeLevel(upgrade.id(), level);
        applyTeam(game, team);
        for (GamePlayer member : team.getMembers()) {
            Player online = member.getPlayer();
            if (online == null) continue;
            applyTo(online, team);
            Messages.send(online, "upgrade.bought",
                    "player", buyer.getName(),
                    "upgrade", Text.plain(upgrade.displayName()),
                    "level", Text.roman(level));
            online.playSound(online, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.4f);
        }
        return true;
    }

    // ------------------------------------------------------------------ applying

    /**
     * Puts everything the team has bought onto one player: their swords, their armour and their hands.
     *
     * @param player who to bring up to date
     * @param team   their team
     */
    public void applyTo(Player player, GameTeam team) {
        int sharpness = levelOf(team, Upgrade.Effect.SHARPNESS);
        int protection = levelOf(team, Upgrade.Effect.PROTECTION);
        if (sharpness > 0) {
            // written back slot by slot rather than enchanting what the array holds: whether that array is
            // the inventory itself or a copy of it depends on the inventory, and a silent copy would be an
            // upgrade that is paid for and never appears
            ItemStack[] contents = player.getInventory().getStorageContents();
            for (int slot = 0; slot < contents.length; slot++) {
                if (!isBlade(contents[slot])) continue;
                contents[slot].addUnsafeEnchantment(Enchantment.SHARPNESS, sharpness);
                player.getInventory().setItem(slot, contents[slot]);
            }
        }
        if (protection > 0) {
            ItemStack[] armor = player.getInventory().getArmorContents();
            for (ItemStack piece : armor) {
                if (piece != null && !piece.getType().isAir()) {
                    piece.addUnsafeEnchantment(Enchantment.PROTECTION, protection);
                }
            }
            player.getInventory().setArmorContents(armor);
        }
        int haste = levelOf(team, Upgrade.Effect.HASTE);
        if (haste > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, PotionEffect.INFINITE_DURATION,
                    haste - 1, false, false, true));
        }
    }

    /**
     * Puts everything that belongs to the team as a whole into effect - which today is the forge.
     *
     * @param game the round
     * @param team whose upgrades
     */
    public void applyTeam(Game game, GameTeam team) {
        GeneratorManager generators = game.getGenerators();
        int forge = levelOf(team, Upgrade.Effect.FORGE);
        // level one is the first upgrade, and the first upgrade is the second tier of the generator
        if (generators != null && forge > 0) generators.setOwnedTier(team, forge + 1);
    }

    /**
     * Hands out the heal pool. Once a second is enough: the effect is given for three seconds and simply
     * refreshed, so it stops a moment after somebody leaves their base rather than the instant they do.
     *
     * @param game  the round
     * @param ticks where the loop stands
     */
    public void tick(Game game, long ticks) {
        if (ticks % 20L != 0L || game.getArena() == null || game.getWorld() == null) return;
        for (GameTeam team : game.getTeams()) {
            if (!team.isAlive() || levelOf(team, Upgrade.Effect.HEAL_POOL) <= 0) continue;
            TeamSpot spot = game.getArena().getTeam(team.getColor());
            if (spot == null || spot.getSpawn() == null) continue;
            Location base = spot.getSpawn().toLocation(game.getWorld());
            double radius = Math.max(1, spot.getProtection());
            for (GamePlayer member : team.getAliveMembers()) {
                Player player = member.getPlayer();
                if (player == null || !player.getWorld().equals(base.getWorld())) continue;
                if (player.getLocation().distanceSquared(base) > radius * radius) continue;
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, HEAL_TICKS, 0,
                        true, false, true));
            }
        }
    }

    /**
     * @param team   whose upgrades
     * @param effect what to look for
     * @return the highest level the team has of any upgrade with that effect, 0 for none
     */
    public int levelOf(GameTeam team, Upgrade.Effect effect) {
        int level = 0;
        for (Upgrade upgrade : settings.getUpgrades()) {
            if (upgrade.effect() != effect) continue;
            level = Math.max(level, team.getUpgradeLevel(upgrade.id()));
        }
        return level;
    }

    /**
     * @param stack an item
     * @return whether a sharpened swords upgrade applies to it
     */
    private static boolean isBlade(@Nullable ItemStack stack) {
        if (stack == null) return false;
        String name = stack.getType().name();
        return name.endsWith("_SWORD") || name.endsWith("_AXE");
    }

}

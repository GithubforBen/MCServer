package de.schnorrenbergers.bedwars.addon.impl;

import de.schnorrenbergers.bedwars.addon.AddonConfig;
import de.schnorrenbergers.bedwars.addon.AddonSettings;
import de.schnorrenbergers.bedwars.addon.ListeningAddon;
import de.schnorrenbergers.bedwars.api.BedwarsPlayerKillEvent;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.shop.Currency;
import de.schnorrenbergers.bedwars.util.Messages;
import de.schnorrenbergers.bedwars.util.Registries;
import de.schnorrenbergers.bedwars.util.Text;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Killstreaks and the price on somebody's head.
 * <p>
 * The two belong together and are one addon on purpose: a streak that only ever helps the player on it
 * makes a good player unstoppable, and the bounty is what the rest of the server gets for stopping them.
 * The reward for a kill is therefore both - a buff for the killer, and a bigger target on their back.
 */
public final class KillstreaksAddon extends ListeningAddon {

    public static final String ID = "killstreaks";

    /**
     * One step of the ladder.
     *
     * @param kills     how many in a row it takes
     * @param effect    what it hands out
     * @param amplifier how strong
     * @param seconds   how long
     */
    private record Reward(int kills, PotionEffectType effect, int amplifier, int seconds) {
    }

    private final AddonConfig config;
    private final List<Reward> rewards = new ArrayList<>();
    /** Who is on how long a streak. */
    private final Map<UUID, Integer> streaks = new HashMap<>();

    private int bountyFrom;
    private int bountyPerKill;
    private int bountyMaximum;
    private Currency bountyCurrency;

    public KillstreaksAddon(Plugin plugin, AddonSettings settings) {
        super(plugin);
        this.config = new AddonConfig(settings, ID);
        read();
    }

    private void read() {
        rewards.clear();
        List<String> written = config.strings("rewards",
                List.of("3:speed:0:30", "5:strength:0:30", "10:regeneration:0:20"),
                "One line per step, written as KILLS:EFFECT:AMPLIFIER:SECONDS.",
                "Kept short and weak on purpose: a streak is meant to be noticed, not to decide a round.");
        for (String line : written) {
            String[] parts = line.split(":");
            int kills = number(parts[0], 0);
            PotionEffectType effect = Registries.effect(parts.length > 1 ? parts[1] : "");
            if (kills <= 0 || effect == null) {
                Bukkit.getLogger().warning("[Bedwars] addons.yml: the killstreak '" + line
                        + "' is not readable and is skipped.");
                continue;
            }
            rewards.add(new Reward(kills, effect,
                    parts.length > 2 ? number(parts[2], 0) : 0,
                    parts.length > 3 ? number(parts[3], 30) : 30));
        }
        bountyFrom = Math.max(1, config.get("bounty.from", 3,
                "From how many kills in a row somebody carries a bounty."));
        bountyPerKill = Math.max(0, config.get("bounty.per-kill", 1,
                "How much the bounty grows with every further kill."));
        bountyMaximum = Math.max(0, config.get("bounty.maximum", 5,
                "How large it can get. 0 turns the bounty off and leaves the streaks."));
        bountyCurrency = config.currency("bounty.currency", Currency.DIAMOND,
                "What a bounty is paid in.");
        config.save();
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
        read();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDescription() {
        return "Buffs for a killing streak, and a bounty on whoever is on one";
    }

    @Override
    protected void onDisable(Game game) {
        streaks.clear();
    }

    // -------------------------------------------------------------------- killing

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(BedwarsPlayerKillEvent event) {
        GamePlayer victim = event.getVictim();
        GamePlayer killer = event.getKiller();
        int hunted = streaks.getOrDefault(victim.getUuid(), 0);
        streaks.remove(victim.getUuid());

        if (killer == null || killer.equals(victim)) return;
        payBounty(killer, victim, hunted);

        int streak = streaks.merge(killer.getUuid(), 1, Integer::sum);
        reward(killer, streak);
        if (streak == bountyFrom && bountyMaximum > 0) {
            Messages.broadcast("killstreak.bounty.set",
                    "player", killer.getName(),
                    "amount", String.valueOf(bounty(streak)),
                    "currency", bountyCurrency.getDisplayName());
        }
    }

    /**
     * Hands out the step of the ladder that was just reached, if it is one.
     */
    private void reward(GamePlayer killer, int streak) {
        Player player = killer.getPlayer();
        if (player == null) return;
        for (Reward reward : rewards) {
            if (reward.kills() != streak) continue;
            player.addPotionEffect(new PotionEffect(reward.effect(), reward.seconds() * 20,
                    reward.amplifier(), false, true, true));
            player.showTitle(Title.title(
                    Messages.get("killstreak.title", "streak", String.valueOf(streak)),
                    Messages.get("killstreak.subtitle",
                            "effect", Text.niceName(reward.effect().getKey().getKey()),
                            "level", Text.roman(reward.amplifier() + 1)),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1400), Duration.ofMillis(400))));
            Messages.broadcast("killstreak.reached",
                    "player", killer.getName(),
                    "streak", String.valueOf(streak));
            player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.6f);
        }
    }

    /**
     * Pays out what the victim was worth.
     *
     * @param killer who gets it
     * @param victim who carried it
     * @param streak how long the victim's streak was
     */
    private void payBounty(GamePlayer killer, GamePlayer victim, int streak) {
        int amount = bounty(streak);
        if (amount <= 0) return;
        Player player = killer.getPlayer();
        if (player == null) return;
        ItemStack payout = new ItemStack(bountyCurrency.getMaterial(), amount);
        player.getInventory().addItem(payout).values()
                .forEach(rest -> player.getWorld().dropItem(player.getLocation(), rest));
        Messages.broadcast("killstreak.bounty.claimed",
                "player", killer.getName(),
                "victim", victim.getName(),
                "amount", String.valueOf(amount),
                "currency", bountyCurrency.getDisplayName());
    }

    /**
     * @param streak how long a streak
     * @return what a head on that streak is worth
     */
    private int bounty(int streak) {
        if (bountyMaximum <= 0 || streak < bountyFrom) return 0;
        return Math.min(bountyMaximum, 1 + (streak - bountyFrom) * bountyPerKill);
    }

    /**
     * @param player somebody in the round
     * @return how many kills in a row they have
     */
    public int streakOf(GamePlayer player) {
        return streaks.getOrDefault(player.getUuid(), 0);
    }
}

package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.Cosmetics;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A thunderstorm over the winners.
 * <p>
 * Every bolt is {@code strikeLightningEffect}, which is the picture and the thunder without the weather:
 * no fire, no damage, no charged creepers. The round is over, but "over" is not the same as "the map may
 * now be set alight", and somebody watching from spectator would still be standing in it.
 */
public class StormWinEffect implements WinEffect {

    /** How long the storm lasts, in ticks. */
    private static final int DEFAULT_DURATION = 20 * 8;
    /** How many ticks between two bolts per winner. */
    private static final int INTERVAL = 10;
    /** How far from a winner a bolt may come down, in blocks. */
    private static final double SPREAD = 4.0d;

    @Override
    public String getId() {
        return Cosmetics.WIN_STORM;
    }

    @Override
    public void play(WinContext context) {
        World world = context.world();
        if (world == null) return;
        List<Player> winners = context.winners();
        if (winners.isEmpty()) return;
        int duration = Math.max(INTERVAL, context.setting(Cosmetics.SETTING_DURATION_TICKS, DEFAULT_DURATION));

        new BukkitRunnable() {
            int elapsed;

            @Override
            public void run() {
                if (elapsed >= duration) {
                    cancel();
                    return;
                }
                elapsed += INTERVAL;
                for (Player winner : winners) {
                    if (!winner.isOnline() || winner.getWorld() != world) continue;
                    ThreadLocalRandom random = ThreadLocalRandom.current();
                    Location at = winner.getLocation().add(
                            random.nextDouble(-SPREAD, SPREAD), 0.0d, random.nextDouble(-SPREAD, SPREAD));
                    world.strikeLightningEffect(at);
                    world.playSound(at, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.AMBIENT,
                            0.6f, (float) random.nextDouble(0.8d, 1.3d));
                }
            }
        }.runTaskTimer(context.plugin(), 0L, INTERVAL);
    }
}

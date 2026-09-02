package de.hems.paper.cosmetic;

import de.hems.types.cosmetic.Cosmetics;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Fireworks over whoever won.
 * <p>
 * The one everybody has. Deliberately the plainest thing in the list: it is what a round ends with when
 * nobody has bought anything, so it has to look finished rather than look like a placeholder.
 */
public class RocketWinEffect implements WinEffect {

    /** How long the fireworks keep coming, in ticks. */
    private static final String SETTING_DURATION = "duration-ticks";
    private static final int DEFAULT_DURATION = 20 * 8;
    /** How many ticks between two rockets per winner. */
    private static final int INTERVAL = 12;

    private static final Color[] COLOURS = {
            Color.RED, Color.LIME, Color.AQUA, Color.YELLOW, Color.FUCHSIA, Color.ORANGE, Color.WHITE};

    @Override
    public String getId() {
        return Cosmetics.WIN_ROCKETS;
    }

    @Override
    public void play(WinContext context) {
        World world = context.world();
        if (world == null) return;
        int duration = Math.max(INTERVAL, context.setting(SETTING_DURATION, DEFAULT_DURATION));
        List<Player> winners = context.winners();
        if (winners.isEmpty()) return;

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
                    launch(winner.getLocation().add(spread(), 1, spread()));
                }
            }
        }.runTaskTimer(context.plugin(), 0L, INTERVAL);
    }

    /**
     * @return a small random offset, so two rockets never go up through the same block
     */
    private static double spread() {
        return ThreadLocalRandom.current().nextDouble(-1.5, 1.5);
    }

    /**
     * Sends one rocket up.
     *
     * @param where from where
     */
    private static void launch(Location where) {
        World world = where.getWorld();
        if (world == null) return;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Firework firework = world.spawn(where, Firework.class, spawned -> {
            FireworkMeta meta = spawned.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder()
                    .with(random.nextBoolean() ? FireworkEffect.Type.BALL_LARGE : FireworkEffect.Type.STAR)
                    .withColor(COLOURS[random.nextInt(COLOURS.length)])
                    .withFade(COLOURS[random.nextInt(COLOURS.length)])
                    .flicker(random.nextBoolean())
                    .trail(true)
                    .build());
            meta.setPower(1);
            spawned.setFireworkMeta(meta);
            // marked before it can ever explode, so the winner is not blown off their own celebration
            CosmeticSafetyListener.mark(spawned);
        });
        firework.setShotAtAngle(false);
    }
}

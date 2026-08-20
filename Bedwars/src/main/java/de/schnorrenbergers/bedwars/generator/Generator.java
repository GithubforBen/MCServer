package de.schnorrenbergers.bedwars.generator;

import de.schnorrenbergers.bedwars.api.BedwarsResourceSpawnEvent;
import de.schnorrenbergers.bedwars.config.GeneratorSettings;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.util.Messages;
import de.schnorrenbergers.bedwars.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * One generator, running.
 * <p>
 * The clock is counted in ticks rather than against the wall clock, so a laggy server drops slower instead
 * of dumping a handful of diamonds the moment it catches up.
 */
public class Generator {

    /** How high above the block the floating text sits. */
    private static final double HOLOGRAM_HEIGHT = 1.6d;

    private final GeneratorSettings.Type type;
    private final Location location;
    /** The team it belongs to, or {@code null} when it stands out in the middle. */
    private final GameTeam owner;

    private int tier = 1;
    private double ticksLeft;
    private TextDisplay hologram;
    /** Whose turn it is to get the drop, for a generator that splits. */
    private int splitTurn;

    public Generator(GeneratorSettings.Type type, Location location, @Nullable GameTeam owner) {
        this.type = type;
        this.location = location;
        this.owner = owner;
        this.ticksLeft = interval();
    }

    /**
     * @return how many ticks lie between two drops at the current level
     */
    private double interval() {
        return type.secondsAt(tier) * 20.0d;
    }

    /**
     * One tick of the clock.
     *
     * @param game the round
     */
    public void tick(Game game) {
        ticksLeft -= 1.0d;
        if (ticksLeft <= 0.0d) {
            ticksLeft += interval();
            drop(game);
        }
    }

    /**
     * Refreshes the floating text. Called once a second, not every tick - a countdown in whole seconds
     * needs no more, and the display is a packet to everybody in sight.
     */
    public void updateHologram() {
        if (!type.hologram()) return;
        if (hologram == null || !hologram.isValid()) hologram = spawnHologram();
        if (hologram == null) return;
        hologram.text(Messages.get("generator.hologram",
                "type", type.displayName(),
                "tier", Text.roman(tier),
                "seconds", String.valueOf(Math.max(0, (int) Math.ceil(ticksLeft / 20.0d)))));
    }

    private @Nullable TextDisplay spawnHologram() {
        if (location.getWorld() == null) return null;
        Location at = location.clone().add(0.0d, HOLOGRAM_HEIGHT, 0.0d);
        return location.getWorld().spawn(at, TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(false);
            display.setPersistent(false);
        });
    }

    /**
     * Drops what this generator makes, unless too much of it is already lying around.
     *
     * @param game the round
     */
    private void drop(Game game) {
        if (location.getWorld() == null) return;
        if (owner != null && !owner.isAlive()) return;
        if (isGroundFull()) return;

        ItemStack stack = new ItemStack(type.material(), type.amount());
        BedwarsResourceSpawnEvent event = new BedwarsResourceSpawnEvent(game, type.id(), location, stack);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        if (type.splitInBase() && owner != null && giveToTeam(event.getDrop())) return;
        Item dropped = location.getWorld().dropItem(location, event.getDrop());
        // straight down, so resources stay on the platform they belong to instead of rolling off it
        dropped.setVelocity(new Vector(0, 0, 0));
    }

    /**
     * @return whether enough of this resource is already lying around the generator
     */
    private boolean isGroundFull() {
        if (type.groundCap() <= 0) return false;
        double range = Math.max(4.0d, type.splitRadius());
        int lying = 0;
        for (Entity entity : location.getWorld().getNearbyEntities(location, range, range, range)) {
            if (!(entity instanceof Item item)) continue;
            if (item.getItemStack().getType() != type.material()) continue;
            lying += item.getItemStack().getAmount();
            if (lying >= type.groundCap()) return true;
        }
        return false;
    }

    /**
     * Hands the drop to somebody of the team standing at the base.
     * <p>
     * Taking turns rather than giving it to whoever is closest: the player standing directly on the
     * generator would otherwise get everything while their team mate a step away gets nothing.
     *
     * @param stack what was made
     * @return whether somebody took it
     */
    private boolean giveToTeam(ItemStack stack) {
        List<GamePlayer> nearby = new ArrayList<>();
        for (GamePlayer member : owner.getAliveMembers()) {
            var player = member.getPlayer();
            if (player == null || !player.getWorld().equals(location.getWorld())) continue;
            if (player.getLocation().distanceSquared(location) <= type.splitRadius() * type.splitRadius()) {
                nearby.add(member);
            }
        }
        if (nearby.isEmpty()) return false;
        GamePlayer receiver = nearby.get(Math.floorMod(splitTurn++, nearby.size()));
        var player = receiver.getPlayer();
        if (player == null) return false;
        var leftover = player.getInventory().addItem(stack);
        // a full inventory gets the rest on the floor rather than losing it
        leftover.values().forEach(rest -> location.getWorld().dropItem(location, rest));
        return true;
    }

    /**
     * @param tier the level this generator now runs at
     */
    public void setTier(int tier) {
        int clamped = Math.max(1, Math.min(type.maximumTier(), tier));
        if (clamped == this.tier) return;
        this.tier = clamped;
        // the new speed applies from the next drop on, so an upgrade never swallows a drop that was due
        ticksLeft = Math.min(ticksLeft, interval());
    }

    public int getTier() {
        return tier;
    }

    public GeneratorSettings.Type getType() {
        return type;
    }

    public Location getLocation() {
        return location;
    }

    public @Nullable GameTeam getOwner() {
        return owner;
    }

    /**
     * Takes the floating text away. The generator itself is only a number and disappears with the round.
     */
    public void remove() {
        if (hologram != null && hologram.isValid()) hologram.remove();
        hologram = null;
    }

    /**
     * @return the text a hologram would show, for tests and debugging
     */
    public Component describe() {
        return Messages.get("generator.hologram",
                "type", type.displayName(),
                "tier", Text.roman(tier),
                "seconds", String.valueOf(Math.max(0, (int) Math.ceil(ticksLeft / 20.0d))));
    }
}

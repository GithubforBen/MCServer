package de.schnorrenbergers.bedwars.listener;

import de.schnorrenbergers.bedwars.Bedwars;
import de.schnorrenbergers.bedwars.api.BedwarsPlayerKillEvent;
import de.schnorrenbergers.bedwars.config.GeneratorSettings;
import de.schnorrenbergers.bedwars.game.Game;
import de.schnorrenbergers.bedwars.game.GamePlayer;
import de.schnorrenbergers.bedwars.game.GameTeam;
import de.schnorrenbergers.bedwars.map.MapPoint;
import de.schnorrenbergers.bedwars.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Dying, and coming back.
 * <p>
 * A death is either the end of a life or the end of a round for somebody, and which of the two it is
 * depends on one thing only: whether their bed is still standing. Everything else here follows from that.
 * <p>
 * The last attacker is remembered for a few seconds, because the most common death in this game mode is
 * the void - and a kill that the void gets credit for is a kill somebody worked for.
 */
public class CombatListener implements Listener {

    /** How long after a hit a death still counts as that attacker's kill. */
    private static final long CREDIT_TICKS = 200L;

    private final Map<UUID, Attacker> lastAttacker = new HashMap<>();

    /**
     * @param who  the attacker
     * @param tick when they hit
     */
    private record Attacker(UUID who, long tick) {
    }

    public CombatListener(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ------------------------------------------------------------ who hit whom

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = attackerOf(event);
        if (attacker == null || attacker.equals(victim)) return;
        lastAttacker.put(victim.getUniqueId(),
                new Attacker(attacker.getUniqueId(), victim.getWorld().getFullTime()));
    }

    /**
     * Keeps team mates from hurting each other.
     * <p>
     * Runs before the bookkeeping above, so a hit that never happens is not remembered as one either -
     * otherwise a team mate's arrow would take the kill off whoever actually did the work.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFriendlyFire(EntityDamageByEntityEvent event) {
        Game game = game();
        if (game == null || !game.isRunning()) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = attackerOf(event);
        if (attacker == null || attacker.equals(victim)) return;
        GamePlayer hurt = game.get(victim);
        GamePlayer hitting = game.get(attacker);
        if (hurt == null || hitting == null || hurt.getTeam() == null) return;
        if (hurt.getTeam().equals(hitting.getTeam())) event.setCancelled(true);
    }

    /**
     * @param event a hit
     * @return the player behind it, whether they threw something, swung something or lit it
     */
    private static @Nullable Player attackerOf(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) return player;
        // tnt is not a projectile, and without this the most explosive kill in the game belongs to nobody
        if (event.getDamager() instanceof TNTPrimed tnt && tnt.getSource() instanceof Player source) {
            return source;
        }
        if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        if (event.getDamager() instanceof Arrow arrow && arrow.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    // ------------------------------------------------------------------- dying

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Game game = game();
        if (game == null || !game.isRunning()) return;

        Player player = event.getPlayer();
        GamePlayer victim = game.get(player);
        event.deathMessage(null);
        event.setDroppedExp(0);
        event.setKeepLevel(true);
        if (victim == null || !victim.isPlaying()) {
            event.getDrops().clear();
            return;
        }

        GamePlayer killer = findKiller(game, player);
        boolean finalKill = victim.getTeam() != null && !victim.getTeam().isBedAlive();

        victim.addDeath();
        // one level off every tool chain: a death has to cost something without starting the round over
        victim.getLoadout().onDeath();
        if (killer != null) killer.addKill(finalKill);
        handOverResources(game, event, killer);
        event.getDrops().clear();

        Bukkit.getPluginManager().callEvent(new BedwarsPlayerKillEvent(game, victim, killer, finalKill));
        announce(victim, killer, finalKill);

        if (finalKill) {
            victim.setState(GamePlayer.State.SPECTATOR);
        } else {
            victim.setState(GamePlayer.State.RESPAWNING);
            victim.setRespawnTicks(game.getSettings().getRespawnSeconds() * 20);
        }
        lastAttacker.remove(player.getUniqueId());
    }

    /**
     * @return who gets the kill: whoever minecraft credits, or whoever hit them last a moment ago
     */
    private @Nullable GamePlayer findKiller(Game game, Player player) {
        if (player.getKiller() != null) return game.get(player.getKiller());
        Attacker attacker = lastAttacker.get(player.getUniqueId());
        if (attacker == null) return null;
        if (player.getWorld().getFullTime() - attacker.tick() > CREDIT_TICKS) return null;
        GamePlayer credited = game.get(attacker.who());
        return credited == null || !credited.isPlaying() ? null : credited;
    }

    /**
     * Gives the resources somebody was carrying to whoever killed them.
     * <p>
     * Without this, chasing a player who is running home with eight diamonds is worth nothing, and the
     * safest thing anybody can do with a resource is stand still.
     */
    private void handOverResources(Game game, PlayerDeathEvent event, @Nullable GamePlayer killer) {
        if (!game.getSettings().isResourcesToKiller() || killer == null) return;
        Player receiver = killer.getPlayer();
        if (receiver == null) return;
        Set<Material> resources = resourceMaterials();
        Map<Material, Integer> taken = new HashMap<>();
        for (ItemStack stack : event.getDrops()) {
            if (stack == null || !resources.contains(stack.getType())) continue;
            taken.merge(stack.getType(), stack.getAmount(), Integer::sum);
        }
        if (taken.isEmpty()) return;
        for (Map.Entry<Material, Integer> entry : taken.entrySet()) {
            var leftover = receiver.getInventory().addItem(new ItemStack(entry.getKey(), entry.getValue()));
            leftover.values().forEach(rest -> receiver.getWorld().dropItem(receiver.getLocation(), rest));
        }
        Messages.send(receiver, "death.collected",
                "player", event.getPlayer().getName(),
                "what", taken.entrySet().stream()
                        .map(entry -> entry.getValue() + "x " + niceName(entry.getKey()))
                        .collect(Collectors.joining(", ")));
    }

    /**
     * @return every material a generator makes, which is what counts as a resource
     */
    private static Set<Material> resourceMaterials() {
        GeneratorSettings settings = Bedwars.getInstance().getGeneratorSettings();
        return settings.all().values().stream().map(GeneratorSettings.Type::material).collect(Collectors.toSet());
    }

    private static String niceName(Material material) {
        String name = material.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * Says what happened, in one line that also says whether it was the last time.
     */
    private void announce(GamePlayer victim, @Nullable GamePlayer killer, boolean finalKill) {
        String team = victim.getTeam() == null ? "" : victim.getTeam().getColor().getDisplayName();
        if (killer == null) {
            Messages.broadcast(finalKill ? "death.alone.final" : "death.alone",
                    "player", victim.getName(), "team", team);
            return;
        }
        Messages.broadcast(finalKill ? "death.killed.final" : "death.killed",
                "player", victim.getName(),
                "team", team,
                "killer", killer.getName(),
                "killer-team", killer.getTeam() == null ? "" : killer.getTeam().getColor().getDisplayName());
    }

    /**
     * Somebody left in the middle of a round.
     * <p>
     * Logging out is a way of not dying, so it is treated as dying: the death counts, and whoever was
     * hitting them a moment ago gets the kill. What it is not is a way of leaving the round behind - as
     * long as their bed stands they keep their place and walk back in where they left off, because the
     * most common reason for this is a connection and not a decision.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onQuit(PlayerQuitEvent event) {
        Game game = game();
        if (game == null || !game.isRunning()) return;
        Player player = event.getPlayer();
        GamePlayer participant = game.get(player);
        if (participant == null || !participant.isAlive()) return;

        GameTeam team = participant.getTeam();
        boolean finalKill = team != null && !team.isBedAlive();
        GamePlayer killer = findKiller(game, player);

        participant.addDeath();
        participant.getLoadout().onDeath();
        if (killer != null) killer.addKill(finalKill);
        Bukkit.getPluginManager().callEvent(
                new BedwarsPlayerKillEvent(game, participant, killer, finalKill));

        boolean keepsPlace = !finalKill && game.getSettings().isKeepPlayingWhenOffline();
        if (keepsPlace) {
            participant.setState(GamePlayer.State.RESPAWNING);
            // no waiting time left: whenever they come back, the round puts them straight into it
            participant.setRespawnTicks(0);
        } else {
            participant.setState(GamePlayer.State.SPECTATOR);
        }
        Messages.broadcast(finalKill ? "death.left.final" : "death.left",
                "player", participant.getName(),
                "team", team == null ? Messages.raw("chat.no-team") : team.getColor().getDisplayName(),
                "killer", killer == null ? "" : killer.getName());
        lastAttacker.remove(player.getUniqueId());
    }

    // ---------------------------------------------------------------- coming back

    /**
     * Puts a dead player somewhere harmless. Whether and when they come back is the round's business, not
     * minecraft's, so they land as a spectator and the phase counts them down.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        Game game = game();
        if (game == null || !game.isRunning() || game.getWorld() == null) return;
        GamePlayer player = game.get(event.getPlayer());
        if (player == null) return;

        Location waiting = watchpoint(game);
        if (waiting != null) event.setRespawnLocation(waiting);
        Bukkit.getScheduler().runTask(Bedwars.getInstance(), () -> {
            if (!event.getPlayer().isOnline()) return;
            event.getPlayer().setGameMode(GameMode.SPECTATOR);
        });
    }

    /**
     * @return where the dead wait, which is the spectator spot of the map
     */
    private static @Nullable Location watchpoint(Game game) {
        if (game.getArena() == null || game.getWorld() == null) return null;
        MapPoint spot = game.getArena().getSpectator();
        return spot == null ? game.getWorld().getSpawnLocation() : spot.toLocation(game.getWorld());
    }

    private static Game game() {
        Bedwars plugin = Bedwars.getInstance();
        return plugin == null ? null : plugin.getGame();
    }
}

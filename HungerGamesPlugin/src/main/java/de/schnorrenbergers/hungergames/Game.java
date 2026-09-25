package de.schnorrenbergers.hungergames;

import de.hems.communication.ListenerAdapter;
import de.hems.paper.event.EventResultService;
import de.hems.paper.warp.ServerConnector;
import de.hems.types.event.EventData;
import de.hems.types.event.EventResultData;
import de.hems.types.event.EventState;
import de.hems.types.event.HungerGamesSettings;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * One game of hunger games, from the waiting room to the last one standing.
 * <p>
 * <b>Waiting.</b> Everybody who arrives stands in the middle, unhurt and unable to touch anything. The game
 * starts once the event's time has come and enough people are there - or when an admin says so.
 * <p>
 * <b>Countdown.</b> Thirty seconds. For the last ten everybody stands frozen on their start place in a ring
 * around the cornucopia, the way the start of a hunger games looks.
 * <p>
 * <b>Running.</b> A grace period first, in which nobody can hurt anybody else. Then open play on the full
 * map, with supply packages coming down every few minutes. Then the border closes in until what is left is
 * the arena for the showdown, and whoever still stands there glows.
 * <p>
 * <b>Placings</b> come from the order people fall in: the first one out is last, the last one standing
 * wins. Every change goes to the launcher straight away, so the result survives this server being switched
 * off, which it is shortly after the game.
 */
public final class Game {

    /** Where the game stands. */
    public enum State {
        WAITING, COUNTDOWN, RUNNING, ENDED
    }

    /** Where a running game stands. */
    public enum Phase {
        GRACE("Schutzzeit"), OPEN("Freies Spiel"), SHRINKING("Grenze schrumpft"), SHOWDOWN("Showdown");

        private final String title;

        Phase(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }
    }

    private static final int COUNTDOWN_SECONDS = 30;
    /** When, in the countdown, everybody is put onto their start place and frozen. */
    private static final int FREEZE_AT = 10;
    /** How long the winner is celebrated before everybody goes back to the lobby. */
    private static final int LOBBY_AFTER_SECONDS = 20;

    private final HungerGamesPlugin plugin;
    private final ArenaMap map;
    private final HungerGamesSettings settings;
    private final SupplyDrops drops;
    private final BossBar bar = BossBar.bossBar(Component.text("Hunger Games"), 1f,
            BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);

    private State state = State.WAITING;
    private boolean frozen;
    private boolean forced;
    private int countdown;
    private long startedAt;
    private long lastDropAt;
    private boolean reportedFinished;
    private boolean graceOver;
    private boolean graceAnnounced;
    private boolean shrinking;
    private boolean showdown;
    private int endedFor;

    /** Everybody who got a start place, in the order they got it. */
    private final Map<UUID, EventResultData> lines = new LinkedHashMap<>();
    private final Set<UUID> alive = new LinkedHashSet<>();
    /** Chests that have had their loot, so opening one twice does not refill it. */
    private final Set<Location> looted = new HashSet<>();
    private BukkitTask task;

    public Game(HungerGamesPlugin plugin, ArenaMap map, HungerGamesSettings settings) {
        this.plugin = plugin;
        this.map = map;
        this.settings = settings;
        this.drops = new SupplyDrops(plugin, this, map);
    }

    public void start() {
        World world = map.getWorld();
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.KEEP_INVENTORY, false);
        world.setGameRule(GameRule.SPAWN_RADIUS, 0);
        world.setSpawnLocation(map.getCenter());
        // until the start the whole world is open, so the waiting room is not hemmed in by a border that is
        // only set when the game begins
        world.getWorldBorder().reset();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void shutdown() {
        if (task != null) task.cancel();
        for (Player player : Bukkit.getOnlinePlayers()) player.hideBossBar(bar);
    }

    /* ------------------------------------------------------------------------------------ the clock */

    private void tick() {
        switch (state) {
            case WAITING -> tickWaiting();
            case COUNTDOWN -> tickCountdown();
            case RUNNING -> tickRunning();
            case ENDED -> tickEnded();
        }
    }

    private void tickWaiting() {
        int online = Bukkit.getOnlinePlayers().size();
        EventData event = ArenaContext.getEvent();
        boolean due = forced || (event != null && event.getState() == EventState.RUNNING);
        if (event != null && (event.getState() == EventState.FINISHED || event.getState() == EventState.CANCELLED)) {
            bar.name(Component.text(ArenaContext.getTitle() + " findet nicht mehr statt", NamedTextColor.RED));
            // no game was ever played, so there is nothing the launcher has to wait for
            reportFinished();
            return;
        }
        int needed = forced ? 1 : settings.getMinPlayers();
        if (due && online >= needed) {
            state = State.COUNTDOWN;
            countdown = COUNTDOWN_SECONDS;
            broadcast(Component.text("Die Spiele beginnen in " + COUNTDOWN_SECONDS + " Sekunden!",
                    NamedTextColor.GOLD));
            return;
        }
        if (!due) {
            bar.name(Component.text(event == null
                    ? "Testarena - wartet auf /hg start"
                    : "Startet in " + EventData.format(event.getTimeUntilStart()), NamedTextColor.YELLOW));
        } else {
            bar.name(Component.text("Warte auf Spieler: " + online + "/" + needed, NamedTextColor.YELLOW));
        }
        bar.progress(Math.min(1f, online / (float) Math.max(1, needed)));
    }

    private void tickCountdown() {
        int online = Bukkit.getOnlinePlayers().size();
        if (!frozen && !forced && online < settings.getMinPlayers()) {
            state = State.WAITING;
            broadcast(Component.text("Zu wenige Spieler - der Start wartet.", NamedTextColor.RED));
            return;
        }
        countdown--;
        bar.name(Component.text("Start in " + countdown + " Sekunden", NamedTextColor.GOLD));
        bar.progress(Math.max(0f, countdown / (float) COUNTDOWN_SECONDS));
        if (countdown == FREEZE_AT) placeEverybody();
        if (countdown <= 5 && countdown > 0) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.showTitle(Title.title(Component.text(String.valueOf(countdown), NamedTextColor.GOLD),
                        Component.empty(), Title.Times.times(Duration.ZERO, Duration.ofMillis(900), Duration.ZERO)));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
            }
        }
        if (countdown <= 0) go();
    }

    private void tickRunning() {
        // everybody left between the freeze and the start, or all fell in the same tick - nobody to wait for
        if (alive.isEmpty()) {
            finish();
            return;
        }
        long elapsed = System.currentTimeMillis() - startedAt;
        long graceEnd = settings.getGraceMinutes() * 60_000L;
        long shrinkAt = settings.getShrinkAfterMinutes() * 60_000L;
        long showdownAt = shrinkAt + settings.getShrinkMinutes() * 60_000L;

        if (!graceOver && elapsed >= graceEnd) {
            graceOver = true;
        }
        if (graceOver && !graceAnnounced && graceEnd > 0) {
            graceAnnounced = true;
            broadcast(Component.text("Die Schutzzeit ist vorbei - ab jetzt zählt jeder Treffer!",
                    NamedTextColor.RED));
            playToAll(Sound.ENTITY_ENDER_DRAGON_GROWL);
        }
        if (!shrinking && elapsed >= shrinkAt) startShrinking();
        if (!showdown && elapsed >= showdownAt) startShowdown();

        if (settings.getDropMinutes() > 0 && System.currentTimeMillis() - lastDropAt >= settings.getDropMinutes() * 60_000L) {
            lastDropAt = System.currentTimeMillis();
            drops.drop();
        }

        Phase phase = getPhase();
        long nextAt = switch (phase) {
            case GRACE -> graceEnd;
            case OPEN -> shrinkAt;
            case SHRINKING -> showdownAt;
            case SHOWDOWN -> -1L;
        };
        String left = nextAt < 0 ? "" : " - noch " + EventData.format(Duration.ofMillis(nextAt - elapsed));
        bar.name(Component.text(phase.getTitle() + left + " · " + alive.size() + " übrig",
                phase == Phase.SHOWDOWN ? NamedTextColor.RED : NamedTextColor.YELLOW));
        bar.progress(nextAt <= 0 ? 1f : Math.max(0f, Math.min(1f, 1f - elapsed / (float) nextAt)));
        bar.color(phase == Phase.SHOWDOWN || phase == Phase.SHRINKING ? BossBar.Color.RED : BossBar.Color.YELLOW);

        for (Player player : Bukkit.getOnlinePlayers()) {
            EventResultData line = lines.get(player.getUniqueId());
            if (line == null || !alive.contains(player.getUniqueId())) continue;
            player.sendActionBar(Component.text("Kills: " + line.getKills(), NamedTextColor.GRAY));
        }

        // the event's clock is not what ends a game: it runs until one is left, and the launcher waits
    }

    private void tickEnded() {
        endedFor++;
        bar.name(Component.text("Vorbei - zurück in die Lobby in " + Math.max(0, LOBBY_AFTER_SECONDS - endedFor)
                + " s", NamedTextColor.GRAY));
        if (endedFor == LOBBY_AFTER_SECONDS) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                ServerConnector.connect(player, ListenerAdapter.ServerName.LOBBY);
            }
        }
    }

    /* ---------------------------------------------------------------------------------- the start */

    /**
     * Puts everybody who is here onto a start place and freezes them there.
     */
    private void placeEverybody() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        List<Location> spawns = map.spawns(players.size());
        UUID eventId = ArenaContext.hasEvent() ? ArenaContext.getEvent().getId() : null;
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            lines.put(player.getUniqueId(), new EventResultData(eventId, player.getUniqueId(), player.getName()));
            alive.add(player.getUniqueId());
            reset(player, GameMode.ADVENTURE);
            player.teleport(spawns.get(i));
        }
        frozen = true;
    }

    private void go() {
        frozen = false;
        state = State.RUNNING;
        startedAt = System.currentTimeMillis();
        lastDropAt = startedAt;

        World world = map.getWorld();
        world.setTime(1000L);
        world.setStorm(false);
        world.setThundering(false);
        WorldBorder border = world.getWorldBorder();
        border.setCenter(map.getCenter());
        border.setSize(settings.getBorderStart());
        border.setDamageBuffer(1.0);
        border.setDamageAmount(1.0);
        border.setWarningDistance(10);

        for (Location location : map.getCornucopiaChests()) {
            BlockState state = location.getBlock().getState();
            if (!(state instanceof Chest chest)) continue;
            Loot.fill(chest.getBlockInventory(), Loot.Tier.CORNUCOPIA);
            looted.add(location);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (alive.contains(player.getUniqueId())) {
                player.setGameMode(GameMode.SURVIVAL);
                player.showTitle(Title.title(Component.text("LOS!", NamedTextColor.GREEN),
                        Component.text("Möge das Glück stets mit euch sein", NamedTextColor.GRAY)));
            } else {
                spectate(player);
            }
        }
        playToAll(Sound.ENTITY_GENERIC_EXPLODE);
        if (settings.getGraceMinutes() > 0) {
            broadcast(Component.text(settings.getGraceMinutes() + " Minute" + (settings.getGraceMinutes() == 1 ? "" : "n")
                    + " Schutzzeit - niemand kann jemandem schaden.", NamedTextColor.AQUA));
        }
        report(lines.values());
    }

    /**
     * Starts the game now, whatever the event's clock says. With a single player it runs until it is
     * stopped, which is what testing an arena alone needs.
     *
     * @return what to tell the admin
     */
    public String forceStart() {
        if (state != State.WAITING) return "Das Spiel läuft schon.";
        forced = true;
        return "Das Spiel startet.";
    }

    /* ------------------------------------------------------------------------------------ the border */

    private void startShrinking() {
        shrinking = true;
        WorldBorder border = map.getWorld().getWorldBorder();
        long ticks = settings.getShrinkMinutes() * 60L * 20L;
        border.changeSize(settings.getBorderEnd(), ticks);
        broadcast(Component.text("Die Grenze schrumpft! In " + settings.getShrinkMinutes()
                + " Minuten ist die Welt nur noch " + settings.getBorderEnd() + " Blöcke breit.", NamedTextColor.RED));
        playToAll(Sound.BLOCK_BEACON_DEACTIVATE);
    }

    private void startShowdown() {
        showdown = true;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(Title.title(Component.text("SHOWDOWN", NamedTextColor.RED),
                    Component.text("Nur einer kommt hier raus", NamedTextColor.GRAY)));
            if (settings.isShowdownGlow() && alive.contains(player.getUniqueId())) player.setGlowing(true);
        }
        playToAll(Sound.ENTITY_WITHER_SPAWN);
    }

    public Phase getPhase() {
        long elapsed = System.currentTimeMillis() - startedAt;
        if (elapsed < settings.getGraceMinutes() * 60_000L) return Phase.GRACE;
        if (showdown) return Phase.SHOWDOWN;
        if (shrinking) return Phase.SHRINKING;
        return Phase.OPEN;
    }

    /* ------------------------------------------------------------------------------ who is still in */

    /**
     * Takes somebody out of the game.
     *
     * @param player who fell
     * @param killer who got them, or {@code null} for the world, the border or a logout
     * @param how    what happened, for the message, or {@code null} for the usual one
     */
    public void eliminate(Player player, @Nullable Player killer, @Nullable String how) {
        UUID id = player.getUniqueId();
        if (state != State.RUNNING || !alive.remove(id)) return;
        EventResultData line = lines.get(id);
        List<EventResultData> changed = new ArrayList<>();
        // the one who falls now takes the worst place that is still free
        line.setPlace(alive.size() + 1);
        changed.add(line);
        EventResultData killerLine = killer == null ? null : lines.get(killer.getUniqueId());
        if (killerLine != null && !killer.getUniqueId().equals(id)) {
            killerLine.setKills(killerLine.getKills() + 1);
            changed.add(killerLine);
        }
        player.setGlowing(false);
        map.getWorld().strikeLightningEffect(player.getLocation());

        String text = how != null ? player.getName() + " " + how
                : killer != null && killerLine != null ? player.getName() + " wurde von " + killer.getName() + " getötet"
                : player.getName() + " ist gestorben";
        broadcast(Component.text("☠ " + text + " - Platz " + line.getPlace() + ". Noch " + alive.size() + " übrig.",
                NamedTextColor.RED));
        report(changed);

        // alone in a test arena is a test, not a win
        if (alive.size() <= 1 && lines.size() > 1) finish();
    }

    private void finish() {
        state = State.ENDED;
        UUID winnerId = alive.isEmpty() ? null : alive.iterator().next();
        if (winnerId != null) {
            EventResultData line = lines.get(winnerId);
            line.setPlace(1);
            alive.clear();
            report(List.of(line));
        }
        Player winner = winnerId == null ? null : Bukkit.getPlayer(winnerId);
        String name = winner != null ? winner.getName() : winnerId != null ? lines.get(winnerId).getPlayerName() : null;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGlowing(false);
            player.showTitle(Title.title(
                    Component.text(name == null ? "Keiner überlebt" : name + " gewinnt!", NamedTextColor.GOLD),
                    Component.text(ArenaContext.getTitle(), NamedTextColor.GRAY)));
        }
        if (winner != null) celebrate(winner);
        playToAll(Sound.UI_TOAST_CHALLENGE_COMPLETE);
        map.getWorld().getWorldBorder().setSize(map.getWorld().getWorldBorder().getSize());
        reportFinished();
    }

    /**
     * Stops the game without a winner. The ones still standing stay without a placing.
     *
     * @return what to tell the admin
     */
    public String stop() {
        if (state == State.ENDED) return "Das Spiel ist schon vorbei.";
        state = State.ENDED;
        alive.clear();
        broadcast(Component.text("Das Spiel wurde von einem Admin beendet.", NamedTextColor.RED));
        reportFinished();
        return "Beendet.";
    }

    /* ---------------------------------------------------------------------------------- the players */

    /**
     * Brings somebody who just arrived into whatever is going on.
     *
     * @param player who joined
     */
    public void join(Player player) {
        player.showBossBar(bar);
        player.setGlowing(false);
        if (state == State.WAITING || (state == State.COUNTDOWN && !frozen)) {
            reset(player, GameMode.ADVENTURE);
            player.teleport(waitingSpot());
            player.sendMessage(Component.text("Willkommen bei " + ArenaContext.getTitle() + "!", NamedTextColor.GOLD));
            return;
        }
        spectate(player);
        player.sendMessage(Component.text("Das Spiel läuft schon - du schaust zu.", NamedTextColor.GRAY));
    }

    /**
     * Somebody left. During the game that is the same as falling - a logout cannot be a hiding place.
     *
     * @param player who left
     */
    public void quit(Player player) {
        player.hideBossBar(bar);
        if (state == State.RUNNING && alive.contains(player.getUniqueId())) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && !item.getType().isAir()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            }
            player.getInventory().clear();
            eliminate(player, null, "hat das Spiel verlassen");
        }
        if (state == State.COUNTDOWN && frozen && alive.remove(player.getUniqueId())) {
            lines.remove(player.getUniqueId());
        }
    }

    private Location waitingSpot() {
        Location center = map.getCenter();
        Location spot = ArenaMap.surface(map.getWorld(), center.getBlockX(), center.getBlockZ() - 4);
        spot.setDirection(center.toVector().subtract(spot.toVector()));
        spot.setPitch(0);
        return spot;
    }

    private void spectate(Player player) {
        player.setGameMode(GameMode.SPECTATOR);
        player.setGlowing(false);
        Location center = map.getCenter().add(0, 10, 0);
        player.teleport(center);
    }

    private static void reset(Player player, GameMode mode) {
        player.setGameMode(mode);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        for (PotionEffect effect : player.getActivePotionEffects()) player.removePotionEffect(effect.getType());
        var health = player.getAttribute(Attribute.MAX_HEALTH);
        player.setHealth(health == null ? 20.0 : health.getValue());
        player.setFoodLevel(20);
        player.setSaturation(5f);
        player.setExp(0f);
        player.setLevel(0);
        player.setFireTicks(0);
    }

    private void celebrate(Player winner) {
        for (int i = 0; i < 5; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!winner.isOnline()) return;
                Firework firework = winner.getWorld().spawn(winner.getLocation(), Firework.class);
                FireworkMeta meta = firework.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE)
                        .withColor(org.bukkit.Color.YELLOW, org.bukkit.Color.ORANGE).withFlicker().build());
                meta.setPower(1);
                firework.setFireworkMeta(meta);
            }, i * 15L);
        }
    }

    /**
     * Tells the launcher the game is over, once, so it settles the event now rather than waiting for this
     * server to go away.
     */
    private void reportFinished() {
        if (reportedFinished || !ArenaContext.hasEvent()) return;
        reportedFinished = true;
        EventResultService.finish(ArenaContext.getEvent().getId(), new ArrayList<>(lines.values()));
    }

    private void report(Collection<EventResultData> changed) {
        if (!ArenaContext.hasEvent() || changed.isEmpty()) return;
        EventResultService.report(new ArrayList<>(changed));
    }

    private static void broadcast(Component message) {
        Bukkit.getServer().sendMessage(message);
    }

    private static void playToAll(Sound sound) {
        for (Player player : Bukkit.getOnlinePlayers()) player.playSound(player.getLocation(), sound, 1f, 1f);
    }

    /* ---------------------------------------------------------------------------- for the listener */

    public State getState() {
        return state;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public boolean isAlive(UUID player) {
        return alive.contains(player);
    }

    /**
     * @return whether players can hurt each other right now
     */
    public boolean isPvp() {
        return state == State.RUNNING && getPhase() != Phase.GRACE;
    }

    /**
     * Marks a chest as looted.
     *
     * @param location the chest
     * @return whether it had not been looted before
     */
    public boolean loot(Location location) {
        return looted.add(location);
    }

    public ArenaMap getMap() {
        return map;
    }

    public String describe() {
        return "Status: " + state + (state == State.RUNNING ? " (" + getPhase().getTitle() + ")" : "")
                + ", " + alive.size() + " übrig, " + lines.size() + " gestartet";
    }
}

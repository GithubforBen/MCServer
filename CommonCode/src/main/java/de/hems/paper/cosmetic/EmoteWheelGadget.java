package de.hems.paper.cosmetic;

import de.hems.api.ItemApi;
import de.hems.paper.PaperContext;
import de.hems.paper.customInventory.CustomInventory;
import de.hems.paper.customInventory.types.SimpleItemAction;
import de.hems.types.cosmetic.CosmeticData;
import de.hems.types.cosmetic.Cosmetics;
import de.hems.types.cosmetic.GadgetSlot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A handful of gestures, out of a menu - and the player's own body is what plays them.
 * <p>
 * A server cannot bend an elbow: which way a limb points is decided by the client, from what its player
 * is doing. What a server can do is make the player do things, and every one of those comes with an
 * animation of its own - swinging an arm, ducking, jumping, spinning the way a riptide trident spins,
 * lying down flat. So an emote here is a short choreography of exactly those: a timeline that says at
 * which tick the body does what. It is the real player with their real skin moving, seen by everybody who
 * can see them, and it needs neither a resource pack nor a stand-in.
 * <p>
 * Two things follow from the body being real. An emote may not be a way out of a fight - lying down makes
 * somebody a great deal harder to hit - so it does not start within a few seconds of a hit and a hit ends
 * it. And whatever an emote leaves on a player, a duck or a pose, is taken off again whichever way it
 * ends: played out, interrupted, replaced by the next one, or by its owner logging off.
 */
public class EmoteWheelGadget implements Gadget, Listener {

    /** How long after an emote before the next one, in ticks. */
    private static final int DEFAULT_COOLDOWN_TICKS = 40;
    /** How long after a hit, given or taken, no emote starts. */
    private static final long COMBAT_MILLIS = 10_000L;
    /** What a jump throws its jumper up with, which is what a hop is. */
    private static final double HOP = 0.42d;

    /** One thing a body does at one moment of an emote. */
    @FunctionalInterface
    private interface Move {
        void play(Player player);
    }

    /**
     * One emote: what it is called, what the menu says it looks like, and the choreography.
     */
    private static final class Emote {

        private final String name;
        private final Material icon;
        private final String looks;
        private final int length;
        private final Map<Integer, List<Move>> timeline = new HashMap<>();

        /**
         * @param length how long it runs, in ticks
         */
        Emote(String name, Material icon, String looks, int length) {
            this.name = name;
            this.icon = icon;
            this.looks = looks;
            this.length = length;
        }

        /**
         * @param tick  when, counted from the start
         * @param moves what the body does then
         */
        Emote at(int tick, Move... moves) {
            timeline.computeIfAbsent(tick, key -> new ArrayList<>()).addAll(List.of(moves));
            return this;
        }

        /**
         * @param from  the first tick
         * @param until the tick it stops before
         * @param step  how many ticks lie between two
         * @param moves what the body does each time
         */
        Emote every(int from, int until, int step, Move... moves) {
            for (int tick = from; tick < until; tick += step) at(tick, moves);
            return this;
        }
    }

    // ------------------------------------------------------------------ moves

    private static final Move SWING = Player::swingMainHand;
    private static final Move SWING_OTHER = Player::swingOffHand;
    private static final Move DUCK = player -> player.setSneaking(true);
    private static final Move RISE = player -> player.setSneaking(false);
    /** Flat on the ground. Held in place, or the server would stand the player up again a tick later. */
    private static final Move LIE_DOWN = player -> player.setPose(Pose.SWIMMING, true);
    private static final Move GET_UP = player -> player.setPose(Pose.STANDING, false);

    private static final Move HOP_UP = player -> {
        // only off the ground: a hop in the air is a second jump, and that is another gadget
        if (!player.isOnGround()) return;
        Vector moving = player.getVelocity();
        player.setVelocity(new Vector(moving.getX(), HOP, moving.getZ()));
    };

    /**
     * @param ticks how long the body spins
     * @return the spin of a riptide trident, without the trident and without what it does to whoever is
     *         in the way: no item behind it means no damage and no enchantment either
     */
    private static Move spin(int ticks) {
        return player -> player.startRiptideAttack(ticks, 0.0f, null);
    }

    private static Move sound(Sound sound, float volume, float pitch) {
        return player -> player.getWorld().playSound(player.getLocation(), sound, volume, pitch);
    }

    /**
     * @param height how far above the feet
     * @param spread how far around that
     */
    private static Move particles(Particle particle, int count, double height, double spread) {
        return player -> player.getWorld().spawnParticle(particle,
                player.getLocation().add(0.0d, height, 0.0d), count, spread, spread, spread, 0.02d);
    }

    // ----------------------------------------------------------------- emotes

    private static final List<Emote> EMOTES = List.of(
            new Emote("Winken", Material.FEATHER, "Du winkst mit dem Arm", 48)
                    .at(0, sound(Sound.ENTITY_VILLAGER_YES, 0.7f, 1.2f))
                    .every(0, 44, 7, SWING),

            new Emote("Klatschen", Material.PAPER, "Du klatschst in die Hände", 40)
                    .every(0, 36, 6, SWING, sound(Sound.BLOCK_WOOD_HIT, 0.6f, 1.8f),
                            particles(Particle.CRIT, 2, 1.2d, 0.2d))
                    .every(3, 36, 6, SWING_OTHER, sound(Sound.BLOCK_WOOD_HIT, 0.6f, 1.6f)),

            new Emote("Jubeln", Material.FIREWORK_ROCKET, "Du springst dreimal und reißt die Arme hoch", 46)
                    .at(0, sound(Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.3f))
                    .every(0, 30, 14, HOP_UP, SWING, SWING_OTHER)
                    .every(6, 36, 14, SWING, SWING_OTHER)
                    .at(34, particles(Particle.FIREWORK, 30, 2.4d, 0.5d),
                            sound(Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.7f, 1.0f)),

            new Emote("Verbeugen", Material.LEATHER_HELMET, "Du verbeugst dich zweimal", 56)
                    .at(0, DUCK, sound(Sound.BLOCK_NOTE_BLOCK_HARP, 0.7f, 0.8f))
                    .at(18, RISE)
                    .at(28, DUCK, sound(Sound.BLOCK_NOTE_BLOCK_HARP, 0.7f, 1.0f))
                    .at(46, RISE),

            new Emote("Lachen", Material.PUFFERFISH, "Du schüttelst dich vor Lachen", 40)
                    .every(0, 36, 4, DUCK)
                    .every(2, 38, 4, RISE)
                    .every(0, 36, 18, sound(Sound.ENTITY_VILLAGER_CELEBRATE, 0.7f, 1.3f))
                    .every(0, 36, 12, particles(Particle.HAPPY_VILLAGER, 6, 2.0d, 0.3d)),

            dance(),

            new Emote("Pirouette", Material.ENDER_EYE, "Du springst ab und drehst dich um dich selbst", 26)
                    .at(0, HOP_UP, spin(18), sound(Sound.ITEM_TRIDENT_RIPTIDE_1, 0.4f, 1.4f))
                    .every(0, 18, 6, particles(Particle.ENCHANT, 20, 1.0d, 0.5d)),

            new Emote("Hinlegen", Material.RED_BED, "Du legst dich flach auf den Boden", 70)
                    .at(0, LIE_DOWN)
                    .every(6, 60, 20, sound(Sound.ENTITY_FOX_SLEEP, 0.6f, 1.0f),
                            particles(Particle.CLOUD, 2, 0.7d, 0.1d))
                    .at(64, GET_UP));

    /**
     * Four seconds to a beat of five ticks: ducking on the beat, an arm on every one of them in turn, a
     * hop at the end of each bar and a spin half way through.
     */
    private static Emote dance() {
        // a tune in the steps of a note block, one note a beat
        float[] tune = {1.0f, 1.26f, 1.5f, 1.26f, 1.0f, 1.26f, 1.5f, 2.0f,
                1.78f, 1.5f, 1.26f, 1.5f, 1.78f, 1.5f, 1.26f, 1.0f};
        int beat = 5;
        Emote dance = new Emote("Tanzen", Material.JUKEBOX, "Du tanzt vier Takte lang", tune.length * beat + 4);
        for (int i = 0; i < tune.length; i++) {
            int tick = i * beat;
            dance.at(tick, sound(Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, tune[i]),
                    particles(Particle.NOTE, 1, 2.2d, 0.3d),
                    i % 2 == 0 ? SWING : SWING_OTHER);
            boolean lastOfBar = i % 4 == 3;
            // up for the hop that ends a bar, down and up in turn for everything before it
            dance.at(tick, lastOfBar || i % 2 == 1 ? RISE : DUCK);
            if (lastOfBar) dance.at(tick, HOP_UP);
        }
        return dance.at(8 * beat, spin(10)).at(tune.length * beat, RISE);
    }

    // ------------------------------------------------------------------ state

    /** Whose emote is playing, and the loop that plays it. */
    private final Map<UUID, BukkitTask> playing = new HashMap<>();
    /** When somebody last hit or was hit, in wall clock milliseconds. */
    private final Map<UUID, Long> lastFight = new HashMap<>();

    @Override
    public String getId() {
        return Cosmetics.GADGET_EMOTES;
    }

    @Override
    public Set<GadgetSlot> slots() {
        return Set.of(GadgetSlot.LOBBY, GadgetSlot.SURVIVAL);
    }

    @Override
    public ItemStack item(CosmeticData cosmetic) {
        return GadgetItems.of(Material.NAME_TAG, getId(), "Emotes", "Rechtsklick: Menü");
    }

    @Override
    public @Nullable String hint() {
        return "Emotes: Rechtsklick öffnet das Menü. Du bewegst dich wirklich - alle in der Nähe sehen es.";
    }

    // not ignoreCancelled: a right click into the air arrives already cancelled, because there is no block
    // to use - only whether the item may be used says if somebody else forbade it
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.useItemInHand() == Event.Result.DENY) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!GadgetItems.is(event.getItem(), getId())) return;
        event.setCancelled(true);

        Player player = event.getPlayer();
        if (Gadgets.settingsFor(player, getId()) == null) return;
        CustomInventory.show(player, menu());
    }

    private CustomInventory menu() {
        CustomInventory menu = new CustomInventory(18, "Emotes", null);
        menu.fillPlaceHolder();
        // every other slot of two rows, so the eight read as a choice rather than as an inventory
        int[] places = {1, 3, 5, 7, 10, 12, 14, 16};
        for (int i = 0; i < EMOTES.size() && i < places.length; i++) {
            Emote emote = EMOTES.get(i);
            menu.setItem(places[i], new ItemApi(emote.icon, ChatColor.LIGHT_PURPLE + emote.name,
                            List.of(ChatColor.GRAY + emote.looks,
                                    ChatColor.DARK_GRAY + "Alle in der Nähe sehen es")).build(),
                    new SimpleItemAction(event -> play((Player) event.getWhoClicked(), emote)));
        }
        return menu;
    }

    // ---------------------------------------------------------------- playing

    private void play(Player player, Emote emote) {
        player.closeInventory();
        CosmeticData gadget = Gadgets.settingsFor(player, getId());
        Plugin plugin = PaperContext.getPlugin();
        if (gadget == null || plugin == null) return;
        if (player.hasCooldown(Material.NAME_TAG)) return;
        String refused = refusal(player);
        if (refused != null) {
            player.sendActionBar(Component.text(refused, NamedTextColor.RED));
            return;
        }
        // the cooldown starts when the emote is over, not when it begins
        player.setCooldown(Material.NAME_TAG, emote.length + Math.max(1,
                gadget.getNumber(Cosmetics.SETTING_COOLDOWN_TICKS, DEFAULT_COOLDOWN_TICKS)));

        stop(player);
        UUID id = player.getUniqueId();
        int[] tick = {0};
        playing.put(id, Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player body = Bukkit.getPlayer(id);
            if (body == null || tick[0] > emote.length || !mayMove(body)) {
                if (body != null) stop(body);
                else forget(id);
                return;
            }
            for (Move move : emote.timeline.getOrDefault(tick[0], List.of())) move.play(body);
            tick[0]++;
        }, 0L, 1L));
        // half of this the player cannot see on themselves without turning the camera round, so they
        // are told that it is running
        player.sendActionBar(Component.text("Emote: " + emote.name, NamedTextColor.LIGHT_PURPLE));
    }

    /**
     * @return why somebody cannot start an emote right now, or {@code null} when they can
     */
    private @Nullable String refusal(Player player) {
        Long fought = lastFight.get(player.getUniqueId());
        if (fought != null && System.currentTimeMillis() - fought < COMBAT_MILLIS) {
            return "Nicht mitten im Kampf.";
        }
        return mayMove(player) ? null : "Das geht gerade nicht.";
    }

    /**
     * @return whether a body is free to be moved about: on its own feet and in the game
     */
    private static boolean mayMove(Player player) {
        return !player.isDead()
                && player.getGameMode() != GameMode.SPECTATOR
                && !player.isInsideVehicle()
                && !player.isGliding()
                && !player.isSleeping();
    }

    /**
     * Ends whatever somebody is playing and takes off what it left on them.
     */
    private void stop(Player player) {
        if (!forget(player.getUniqueId())) return;
        player.setSneaking(false);
        if (player.hasFixedPose()) player.setPose(Pose.STANDING, false);
    }

    /**
     * @return whether there was an emote to end
     */
    private boolean forget(UUID id) {
        BukkitTask task = playing.remove(id);
        if (task == null) return false;
        task.cancel();
        return true;
    }

    // ------------------------------------------------------------- interrupts

    /**
     * A hit ends an emote and keeps the next one from starting, for whoever took it and whoever dealt it.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        long now = System.currentTimeMillis();
        if (event.getEntity() instanceof Player victim) {
            lastFight.put(victim.getUniqueId(), now);
            stop(victim);
        }
        if (event instanceof EntityDamageByEntityEvent hit && hit.getDamager() instanceof Player attacker) {
            lastFight.put(attacker.getUniqueId(), now);
            stop(attacker);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        stop(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        stop(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        stop(event.getPlayer());
        lastFight.remove(event.getPlayer().getUniqueId());
    }
}

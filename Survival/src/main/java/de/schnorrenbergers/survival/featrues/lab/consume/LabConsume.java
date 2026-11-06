package de.schnorrenbergers.survival.featrues.lab.consume;

import de.schnorrenbergers.survival.featrues.lab.LabSettings;
import de.schnorrenbergers.survival.featrues.lab.consume.LabConsumeRules.Crash;
import de.schnorrenbergers.survival.featrues.lab.consume.LabConsumeRules.Grant;
import de.schnorrenbergers.survival.featrues.lab.item.LabData;
import de.schnorrenbergers.survival.featrues.lab.item.LabEffect;
import de.schnorrenbergers.survival.featrues.lab.item.LabItems;
import de.schnorrenbergers.survival.featrues.lab.item.LabKind;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Optional;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Consuming Kokain: a right click uses up one item, gives the effects of its purity and starts a crash after
 * them. The crash is kept on the player as a due time, so it comes on the next join if it fell due while they
 * were offline, and it is gone when they die. Register once with {@link #init(Plugin, LabSettings)}.
 *
 * <p>Out of combat the effects are those of the table; in combat Speed and Haste are capped (see
 * {@link LabCombat}).
 */
public final class LabConsume implements Listener {

    /** How far a running effect's end may be from the recorded one and still be ours. */
    static final long TOLERANCE_MILLIS = 2000;

    private final Plugin plugin;
    private final LabSettings settings;
    private final LabConsumeState state;
    private final Predicate<Player> inCombat;
    private final Map<UUID, Integer> handledTick = new HashMap<>();

    LabConsume(Plugin plugin, LabSettings settings, LabConsumeState state, Predicate<Player> inCombat) {
        this.plugin = plugin;
        this.settings = settings;
        this.state = state;
        this.inCombat = inCombat;
    }

    /** Registers consuming, and the check that starts due crashes. */
    public static void init(Plugin plugin, LabSettings settings) {
        LabConsumeState state = new LabConsumeState(plugin);
        LabCombat combat = new LabCombat(settings, state);
        LabConsume consume = new LabConsume(plugin, settings, state, combat::inCombat);
        Bukkit.getPluginManager().registerEvents(combat, plugin);
        Bukkit.getPluginManager().registerEvents(consume, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, consume::startDueCrashes, 20L, 20L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        EquipmentSlot hand = event.getHand();
        if (hand == null) return;
        ItemStack item = event.getItem();
        Optional<LabData> data = LabItems.read(item);
        if (data.isEmpty() || data.get().kind() != LabKind.KOKAIN) return;

        // Kokain does nothing else: no block is placed or used with it
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR) return;
        // the event comes once per hand; one tick is one use
        int tick = Bukkit.getCurrentTick();
        Integer before = handledTick.put(player.getUniqueId(), tick);
        if (before != null && before == tick) return;

        long now = System.currentTimeMillis();
        int left = LabConsumeRules.cooldownLeft(state.lastConsume(player), now, settings.cooldownSeconds());
        if (left > 0) {
            player.sendMessage(Component.text("Noch " + left + (left == 1 ? " Sekunde" : " Sekunden")
                    + " warten, bevor du wieder Kokain nehmen kannst.", NamedTextColor.RED));
            return;
        }

        consume(player, data.get(), now);
        ItemStack held = hand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand() : player.getInventory().getItemInMainHand();
        held.setAmount(held.getAmount() - 1);
        if (hand == EquipmentSlot.OFF_HAND) player.getInventory().setItemInOffHand(held);
        else player.getInventory().setItemInMainHand(held);
    }

    private void consume(Player player, LabData data, long now) {
        Grant grant = LabConsumeRules.grant(data.purity(), inCombat.test(player));
        int ticks = grant.seconds() * 20;
        long end = now + grant.seconds() * 1000L;

        state.setLastConsume(player, now);
        state.clearEffect(player, true);
        state.clearEffect(player, false);
        if (grant.speed() > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ticks, grant.speed() - 1));
            state.setEffect(player, true, grant.speed() - 1, end);
        }
        if (grant.haste() > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, ticks, grant.haste() - 1));
            state.setEffect(player, false, grant.haste() - 1, end);
        }
        for (LabEffect effect : data.effects()) {
            player.addPotionEffect(new PotionEffect(type(effect), ticks, 0));
        }
        state.setCutEffects(player, data.effects());
        state.setCrash(player, end, data.purity());
    }

    /** Starts the crash of every online player whose time has come. */
    private void startDueCrashes() {
        for (Player player : Bukkit.getOnlinePlayers()) startCrashIfDue(player, false);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        startCrashIfDue(event.getPlayer(), true);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        state.clearCrash(event.getEntity());
    }

    /**
     * Starts the pending crash if it is due. The pending data is removed first, so a crash neither comes twice
     * nor survives a restart. On join the clock of the running effects stood still while the player was
     * offline, but the recorded end is wall clock: if the crash is due, what Kokain gave (Speed, Haste and the
     * cut-in effects) is taken away; if not, those effects are set to the time that is left.
     */
    private void startCrashIfDue(Player player, boolean onJoin) {
        long[] pending = state.crash(player);
        if (pending == null) return;
        long now = System.currentTimeMillis();
        if (!LabConsumeRules.crashDue(pending[0], now)) {
            if (onJoin) shortenOurs(player, pending[0], now);
            return;
        }
        if (player.isDead()) return;
        if (onJoin) {
            removeOurs(player, PotionEffectType.SPEED, true);
            removeOurs(player, PotionEffectType.HASTE, false);
            for (LabEffect effect : state.cutEffects(player)) {
                PotionEffect active = player.getPotionEffect(type(effect));
                if (active != null && active.getAmplifier() == 0) player.removePotionEffect(type(effect));
            }
        }
        state.clearCrash(player);

        Crash crash = LabConsumeRules.crash((int) pending[1]);
        int ticks = crash.seconds() * 20;
        if (crash.nausea()) player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, ticks, 0));
        if (crash.slowness() > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, crash.slowness() - 1));
        }
    }

    /** Sets the running effects of the dose to the wall-clock time that is left. */
    private void shortenOurs(Player player, long endMillis, long now) {
        LabConsumeState.Recorded speed = state.effect(player, true);
        if (speed != null) shorten(player, PotionEffectType.SPEED, speed.amplifier(), endMillis, now);
        LabConsumeState.Recorded haste = state.effect(player, false);
        if (haste != null) shorten(player, PotionEffectType.HASTE, haste.amplifier(), endMillis, now);
        for (LabEffect effect : state.cutEffects(player)) shorten(player, type(effect), 0, endMillis, now);
    }

    private void shorten(Player player, PotionEffectType type, int amplifier, long endMillis, long now) {
        PotionEffect active = player.getPotionEffect(type);
        if (active == null || active.isInfinite() || active.getAmplifier() != amplifier) return;
        if (!LabConsumeRules.needsShortening(active.getDuration(), endMillis, now)) return;
        int ticks = LabConsumeRules.remainingTicks(endMillis, now);
        player.removePotionEffect(type);
        player.addPotionEffect(new PotionEffect(type, ticks, amplifier, active.isAmbient(),
                active.hasParticles(), active.hasIcon()));
    }

    private void removeOurs(Player player, PotionEffectType type, boolean speed) {
        LabConsumeState.Recorded recorded = state.effect(player, speed);
        PotionEffect active = player.getPotionEffect(type);
        if (recorded != null && active != null && active.getAmplifier() == recorded.amplifier()) {
            player.removePotionEffect(type);
        }
    }

    static PotionEffectType type(LabEffect effect) {
        return switch (effect) {
            case NIGHT_VISION -> PotionEffectType.NIGHT_VISION;
            case JUMP_BOOST -> PotionEffectType.JUMP_BOOST;
            case FIRE_RESISTANCE -> PotionEffectType.FIRE_RESISTANCE;
            case REGENERATION -> PotionEffectType.REGENERATION;
            case STRENGTH -> PotionEffectType.STRENGTH;
        };
    }
}

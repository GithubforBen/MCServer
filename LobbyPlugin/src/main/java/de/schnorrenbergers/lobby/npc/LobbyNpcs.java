package de.schnorrenbergers.lobby.npc;

import de.hems.api.ServerApi;
import de.hems.paper.PaperContext;
import de.hems.paper.event.EventAnnouncer;
import de.hems.paper.event.EventCalendarUi;
import de.hems.paper.hologram.Hologram;
import de.hems.paper.servermanager.NetworkPlayers;
import de.hems.types.Server;
import de.hems.types.admin.PlayerSnapshot;
import de.schnorrenbergers.lobby.LobbyWorld;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import io.papermc.paper.entity.LookAnchor;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The npcs of the lobby: people standing around who take you somewhere when you click them.
 * <p>
 * A {@link NpcType#WARP} npc sends you to its server and shows over its head whether that server is up
 * and how many are on it. An {@link NpcType#EVENTS} npc opens the event calendar and shows what is running
 * or coming next. Both answer a right click and a left click - hitting the thing you want is the first
 * thing half the people in a lobby try.
 * <p>
 * They are {@link Mannequin}s, the player-shaped entity the game has had since 1.21.9: they wear a real
 * skin and need no plugin and no packets. Like the lotto stand they are never saved with the world, and
 * are put back on every start and whenever their chunk comes back.
 */
public final class LobbyNpcs implements Listener {

    /** How far away a player has to be for an npc to stop looking at them. */
    private static final double LOOK_RANGE = 8.0d;
    /** How long a click is ignored after the last one, so a double click does not warp twice. */
    private static final long CLICK_COOLDOWN_MILLIS = 1000L;

    private final NpcStore store;
    private final NamespacedKey key;
    private final Map<String, LobbyNpc> npcs = new LinkedHashMap<>();
    private final Map<UUID, Long> lastClick = new HashMap<>();

    /** What the last look at the network said about each server, by upper-case name. */
    private volatile Map<String, ServerStatus> statuses = Map.of();
    private final AtomicBoolean refreshing = new AtomicBoolean();

    public LobbyNpcs(Plugin plugin, NpcStore store) {
        this.store = store;
        this.key = new NamespacedKey(plugin, "lobby-npc");
        World world = LobbyWorld.get();
        if (world != null) npcs.putAll(store.load(world));
        Bukkit.getPluginManager().registerEvents(this, plugin);
        // puts them back when their chunk comes back, and keeps the event countdown current
        Bukkit.getScheduler().runTaskTimer(plugin, this::ensure, 20L, 20L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::lookAround, 5L, 4L);
        // who is on which server takes a round trip through the whole network, so not every second
        Bukkit.getScheduler().runTaskTimer(plugin, this::refreshServers, 40L, 200L);
    }

    // ------------------------------------------------------------------ what admins do

    /**
     * @return every npc, in the order they were made
     */
    public Collection<LobbyNpc> all() {
        return Collections.unmodifiableCollection(npcs.values());
    }

    /**
     * @param id the id
     * @return the npc, or {@code null} when there is none by that id
     */
    public @Nullable LobbyNpc get(String id) {
        return npcs.get(id.toLowerCase(Locale.ROOT));
    }

    /**
     * Puts up a new npc.
     *
     * @param base     what the id should be based on - a number is added when it is taken
     * @param type     what it does
     * @param target   the server of a warp npc
     * @param name     its name, with {@code &} colour codes
     * @param location where it stands
     * @return the new npc
     */
    public LobbyNpc create(String base, NpcType type, @Nullable String target, String name, Location location) {
        String id = freeId(base);
        LobbyNpc npc = new LobbyNpc(id, type, target, name, null, location);
        npcs.put(id, npc);
        save();
        ensure();
        refreshServers();
        return npc;
    }

    /**
     * Takes an npc away for good.
     *
     * @param npc the npc
     */
    public void remove(LobbyNpc npc) {
        npc.despawn();
        npcs.remove(npc.getId());
        save();
    }

    /**
     * Saves a change to an npc and shows it: the mannequin is put up again, so a new skin or spot is
     * visible straight away.
     *
     * @param npc the npc that was changed
     */
    public void changed(LobbyNpc npc) {
        npc.despawn();
        save();
        ensure();
        refreshServers();
    }

    private void save() {
        store.save(npcs.values());
    }

    private String freeId(String base) {
        String clean = base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        if (clean.isEmpty()) clean = "npc";
        if (!npcs.containsKey(clean)) return clean;
        int number = 2;
        while (npcs.containsKey(clean + number)) number++;
        return clean + number;
    }

    // ------------------------------------------------------------------ in the world

    /**
     * Makes sure every npc whose chunk is loaded is standing there, and that the text over it is current.
     */
    public void ensure() {
        for (LobbyNpc npc : npcs.values()) {
            Location at = npc.getLocation();
            if (at.getWorld() == null) continue;
            if (!at.getWorld().isChunkLoaded(at.getBlockX() >> 4, at.getBlockZ() >> 4)) continue;
            if (!npc.isSpawned()) spawn(npc);
            if (npc.getSign() == null) {
                npc.setSign(new Hologram(at).height(LobbyNpc.TEXT_HEIGHT));
            }
            npc.getSign().setLines(lines(npc)).spawn();
        }
    }

    private void spawn(LobbyNpc npc) {
        Location at = npc.getLocation();
        // one left over from before a crash would have been saved with the chunk - there is only ever one
        for (Entity entity : at.getWorld().getNearbyEntities(at, 2, 3, 2)) {
            if (npc.getId().equals(entity.getPersistentDataContainer().get(key, PersistentDataType.STRING))) {
                entity.remove();
            }
        }
        npc.setEntity(at.getWorld().spawn(at, Mannequin.class, mannequin -> {
            mannequin.setPersistent(false);
            mannequin.setAI(false);
            mannequin.setInvulnerable(true);
            mannequin.setSilent(true);
            mannequin.setCollidable(false);
            mannequin.setImmovable(true);
            // the name and what the npc does are in the text over its head, which can say more
            mannequin.setCustomNameVisible(false);
            mannequin.setDescription(null);
            if (npc.getSkin() != null) {
                mannequin.setProfile(ResolvableProfile.resolvableProfile().name(npc.getSkin()).build());
            }
            mannequin.getPersistentDataContainer().set(key, PersistentDataType.STRING, npc.getId());
        }));
    }

    /**
     * @param npc the npc
     * @return what it says over its head
     */
    private List<Component> lines(LobbyNpc npc) {
        List<Component> lines = new ArrayList<>();
        lines.add(npc.displayName().decorationIfAbsent(TextDecoration.BOLD, TextDecoration.State.TRUE));
        switch (npc.getType()) {
            case WARP -> {
                lines.add(statusLine(npc.getTarget()));
                lines.add(Component.text("Klicken zum Beitreten", NamedTextColor.GRAY));
            }
            case EVENTS -> {
                Component events = EventAnnouncer.tabLine();
                lines.add(events != null ? events
                        : Component.text("Gerade ist nichts geplant", NamedTextColor.GRAY));
                lines.add(Component.text("Klicken für den Kalender", NamedTextColor.GRAY));
            }
        }
        return lines;
    }

    private Component statusLine(@Nullable String target) {
        if (target == null) return Component.text("kein Ziel", NamedTextColor.RED);
        ServerStatus status = statuses.get(target.toUpperCase(Locale.ROOT));
        if (status == null) {
            // not heard back yet, or the server is not known at all - "offline" is the honest answer to both
            // once the first look has been taken
            return statuses.isEmpty() ? Component.text("...", NamedTextColor.DARK_GRAY)
                    : Component.text("● offline", NamedTextColor.RED);
        }
        if (status.joinable()) {
            if (status.players() < 0) return Component.text("● online", NamedTextColor.GREEN);
            return Component.text("● " + status.players() + " Spieler online", NamedTextColor.GREEN);
        }
        if (status.starting()) return Component.text("● " + status.description(), NamedTextColor.YELLOW);
        return Component.text("● offline", NamedTextColor.RED);
    }

    /**
     * Asks the network which servers are up and who is on them, off the main thread.
     */
    private void refreshServers() {
        boolean anyWarp = false;
        for (LobbyNpc npc : npcs.values()) anyWarp |= npc.getType() == NpcType.WARP;
        if (!anyWarp || !refreshing.compareAndSet(false, true)) return;
        PaperContext.async(() -> {
            try {
                Map<String, ServerStatus> found = new HashMap<>();
                Map<String, List<PlayerSnapshot>> players = NetworkPlayers.byServer();
                for (Server server : ServerApi.listServers()) {
                    if (server == null) continue;
                    int count = NetworkPlayers.answered(players, server.name)
                            ? NetworkPlayers.of(players, server.name).size() : -1;
                    found.put(server.name.toUpperCase(Locale.ROOT), new ServerStatus(server.isJoinable(),
                            server.isStartingUp(), server.getPhaseDescription(), count));
                }
                statuses = found;
            } catch (Exception e) {
                // the host not answering leaves the last known state up, which is better than none
            } finally {
                refreshing.set(false);
            }
        });
    }

    /**
     * Turns every npc towards the nearest player, and back to where it faced when nobody is near.
     */
    private void lookAround() {
        for (LobbyNpc npc : npcs.values()) {
            Mannequin entity = npc.getEntity();
            if (entity == null || !entity.isValid()) continue;
            Player nearest = null;
            double best = LOOK_RANGE * LOOK_RANGE;
            for (Player player : entity.getWorld().getPlayers()) {
                if (player.getGameMode() == GameMode.SPECTATOR) continue;
                double distance = player.getLocation().distanceSquared(entity.getLocation());
                if (distance < best) {
                    best = distance;
                    nearest = player;
                }
            }
            if (nearest != null) {
                Location eyes = nearest.getEyeLocation();
                entity.lookAt(eyes.getX(), eyes.getY(), eyes.getZ(), LookAnchor.EYES);
                entity.setBodyYaw(entity.getYaw());
            } else if (entity.getYaw() != npc.getLocation().getYaw() || entity.getPitch() != 0f) {
                entity.setRotation(npc.getLocation().getYaw(), 0f);
                entity.setBodyYaw(npc.getLocation().getYaw());
            }
        }
    }

    /**
     * Takes every npc out of the world, for when the plugin stops.
     */
    public void despawnAll() {
        for (LobbyNpc npc : npcs.values()) npc.despawn();
    }

    // ------------------------------------------------------------------ clicks

    private @Nullable LobbyNpc of(Entity entity) {
        String id = entity.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return id == null ? null : npcs.get(id);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRightClick(PlayerInteractEntityEvent event) {
        LobbyNpc npc = of(event.getRightClicked());
        if (npc == null) return;
        event.setCancelled(true);
        // the event comes once per hand
        if (event.getHand() != EquipmentSlot.HAND) return;
        use(event.getPlayer(), npc);
    }

    /**
     * A left click. This comes before the game looks at whether the target can be hurt at all, so it also
     * reaches an npc that is invulnerable - the damage event never would.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onLeftClick(PrePlayerAttackEntityEvent event) {
        LobbyNpc npc = of(event.getAttacked());
        if (npc == null) return;
        event.setCancelled(true);
        use(event.getPlayer(), npc);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (of(event.getEntity()) != null) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastClick.remove(event.getPlayer().getUniqueId());
    }

    private void use(Player player, LobbyNpc npc) {
        long now = System.currentTimeMillis();
        Long last = lastClick.get(player.getUniqueId());
        if (last != null && now - last < CLICK_COOLDOWN_MILLIS) return;
        lastClick.put(player.getUniqueId(), now);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
        switch (npc.getType()) {
            case WARP -> {
                if (npc.getTarget() == null) {
                    player.sendMessage(Component.text("Dieser NPC hat noch kein Ziel.", NamedTextColor.RED));
                    return;
                }
                // the same way /warp goes: it knows about servers that are still starting and waits for them
                player.performCommand("warp " + npc.getTarget());
            }
            case EVENTS -> EventCalendarUi.open(player);
        }
    }

    /**
     * What the network said about one server.
     *
     * @param joinable    whether players can go there right now
     * @param starting    whether it is on its way up
     * @param description how far it has got, while it is on its way up
     * @param players     how many are on it, {@code -1} when it did not say
     */
    private record ServerStatus(boolean joinable, boolean starting, String description, int players) {
    }
}

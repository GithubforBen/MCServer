package com.benschnorr.survival.featrues.lab.brew;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.view.BrewingStandView;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * The progress arrow for a brew run by our own tick. The view setters write the stand's own brew time,
 * which vanilla zeroes in the same tick for a stand it cannot brew, before the menu syncs, so the client
 * never saw it. The value is sent to each viewer as a data packet instead: vanilla only resends a slot
 * when its own value changes, and that stays 0, so nothing overwrites ours. It counts down like
 * vanilla's, so the arrow fills as the brew advances. Only running brews with a viewer cost anything.
 * see knowledge: /decisions/lab-brewing-engine.md
 */
final class LabArrow {

    private static final int TOTAL = 400;
    private static final int PERIOD = 2;

    private record Running(BrewState brew, int tick) {
    }

    private final JavaPlugin plugin;
    private final Map<Block, Running> running = new HashMap<>();
    private final Sender sender = new Sender();
    private boolean failed;

    LabArrow(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, PERIOD, PERIOD);
    }

    /** A brew advanced; the arrow interpolates from here until the next step. */
    void running(Block block, BrewState brew) {
        running.put(block, new Running(brew, Bukkit.getCurrentTick()));
    }

    /** The brew ended or paused: the arrow goes empty once and the stand is no longer followed. */
    void stopped(Block block) {
        if (running.remove(block) != null) send(block, 0);
    }

    private void tick() {
        if (running.isEmpty() || failed) return;
        int now = Bukkit.getCurrentTick();
        running.entrySet().removeIf(entry -> {
            Block block = entry.getKey();
            if (!block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)
                    || block.getType() != Material.BREWING_STAND) return true;
            Running run = entry.getValue();
            send(block, run.brew().arrowTicks(now - run.tick(), TOTAL));
            return false;
        });
    }

    private void send(Block block, int ticks) {
        if (failed || !block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)
                || block.getType() != Material.BREWING_STAND) return;
        if (!(block.getState(false) instanceof BrewingStand stand)) return;
        for (HumanEntity viewer : stand.getInventory().getViewers()) {
            if (viewer instanceof Player player && player.getOpenInventory() instanceof BrewingStandView) {
                try {
                    sender.send(player, ticks, TOTAL);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    failed = true;
                    plugin.getLogger().warning("Lab: progress arrow disabled: " + e);
                    return;
                }
            }
        }
    }

    /** Sends {@code ClientboundContainerSetDataPacket(containerId, 2, total)} then slot 0 as {@code value}; the names are Mojang's. */
    private static final class Sender {
        private Method getHandle;
        private Field menuField;
        private Field idField;
        private Field connectionField;
        private Method send;
        private Constructor<?> packet;

        void send(Player player, int value, int total) throws ReflectiveOperationException {
            if (getHandle == null) {
                Method handleMethod = player.getClass().getMethod("getHandle");
                Class<?> serverPlayer = handleMethod.getReturnType();
                menuField = Class.forName("net.minecraft.world.entity.player.Player").getField("containerMenu");
                idField = Class.forName("net.minecraft.world.inventory.AbstractContainerMenu").getField("containerId");
                connectionField = serverPlayer.getField("connection");
                send = Class.forName("net.minecraft.server.network.ServerCommonPacketListenerImpl")
                        .getMethod("send", Class.forName("net.minecraft.network.protocol.Packet"));
                packet = Class.forName("net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket")
                        .getConstructor(int.class, int.class, int.class);
                getHandle = handleMethod;
            }
            Object handle = getHandle.invoke(player);
            int id = idField.getInt(menuField.get(handle));
            Object connection = connectionField.get(handle);
            // slot 2 is the total: the client draws the arrow only when slots 0 and 2 are both above 0
            send.invoke(connection, packet.newInstance(id, 2, total));
            send.invoke(connection, packet.newInstance(id, 0, value));
        }
    }
}

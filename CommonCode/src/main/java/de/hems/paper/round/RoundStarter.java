package de.hems.paper.round;

import de.hems.api.ServerApi;
import de.hems.paper.PaperContext;
import de.hems.paper.event.EventService;
import de.hems.paper.warp.ServerStartup;
import de.hems.types.ServerTemplate;
import de.hems.types.event.EventData;
import de.hems.types.round.RoundData;
import de.hems.types.round.RoundPolicy;
import de.hems.types.round.RoundState;
import de.hems.types.server.CapacityData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Everything that has to be true before a player may put a round up, and the start itself.
 * <p>
 * The checks are in a deliberate order: the cheap local ones first, the one that costs a round trip to the
 * launcher last. The last one is also the only one that is not a rule but a fact - the machine either has
 * two more gigabytes or it does not - and it is asked at the launcher rather than worked out here, because
 * two players pressing the button in the same second would otherwise both be told yes.
 */
public final class RoundStarter {

    private RoundStarter() {
    }

    /**
     * Why a round may not be started, or {@code null} when it may.
     *
     * @param message  what to tell the player
     * @param capacity {@code true} when the machine is simply full, which is the case an admin can fix
     */
    public record Refusal(String message, boolean capacity) {
    }

    /**
     * The checks that need nothing but what this server already knows.
     *
     * @param player who wants to start
     * @return why not, or {@code null} when nothing speaks against it
     */
    public static @Nullable Refusal precheck(Player player) {
        RoundPolicy policy = RoundService.getPolicy();
        boolean operator = player.isOp();
        if (!policy.isSelfStartEnabled() && !operator) {
            return new Refusal("Eigene Runden sind gerade nicht freigeschaltet.", false);
        }
        if (!RoundService.isLoaded()) {
            return new Refusal("Die Rundenliste ist noch nicht da - gleich nochmal versuchen.", false);
        }
        // an operator is exempt from the rules, but not from the machine: the memory check below still runs
        if (operator) return null;

        Refusal blockedByEvent = eventBlock(policy);
        if (blockedByEvent != null) return blockedByEvent;

        if (policy.getMaxRounds() > 0 && RoundService.aliveRounds() >= policy.getMaxRounds()) {
            return new Refusal("Es laufen gerade schon " + RoundService.aliveRounds()
                    + " eigene Runden. Warte, bis eine davon vorbei ist.", false);
        }
        int open = RoundService.openOf(player.getUniqueId());
        if (open >= policy.getMaxPerPlayer()) {
            return new Refusal(open == 1
                    ? "Du hast schon eine Runde offen. Beende sie zuerst."
                    : "Du hast schon " + open + " Runden offen.", false);
        }
        long since = System.currentTimeMillis() - RoundService.lastStartOf(player.getUniqueId());
        long cooldown = policy.getCooldownSeconds() * 1000L;
        if (cooldown > 0 && since < cooldown) {
            long left = (cooldown - since + 999L) / 1000L;
            return new Refusal("Du kannst in " + describe(left) + " wieder eine Runde starten.", false);
        }
        return null;
    }

    /**
     * @param policy the rules
     * @return why an event is in the way, or {@code null}
     */
    private static @Nullable Refusal eventBlock(RoundPolicy policy) {
        if (!EventService.isLoaded()) return null;
        if (policy.isBlockWhileEventRunning()) {
            List<EventData> running = EventService.getRunning();
            if (!running.isEmpty()) {
                return new Refusal("Während \"" + running.get(0).getName()
                        + "\" läuft, kann niemand eine eigene Runde starten.", false);
            }
        }
        int minutes = policy.getBlockBeforeEventMinutes();
        if (minutes <= 0) return null;
        EventData next = EventService.getNext();
        if (next == null) return null;
        long untilStart = next.getStartsAt() - System.currentTimeMillis();
        if (untilStart <= 0 || untilStart > minutes * 60_000L) return null;
        return new Refusal(next.getName() + " startet in "
                + describe((untilStart + 59_999L) / 60_000L * 60L)
                + " - bis dahin gehört der Platz dem Event.", false);
    }

    /**
     * @param seconds a duration
     * @return it written out in German
     */
    private static String describe(long seconds) {
        if (seconds < 60) return seconds + (seconds == 1 ? " Sekunde" : " Sekunden");
        long minutes = (seconds + 59) / 60;
        return minutes + (minutes == 1 ? " Minute" : " Minuten");
    }

    /**
     * @param policy the rules
     * @return the heap a round server is started with
     */
    public static int memoryOf(RoundPolicy policy) {
        int configured = policy.getMemoryMB();
        return configured > 0 ? configured : ServerTemplate.BEDWARS.getDefaultMemoryMB();
    }

    /**
     * Puts a round up and takes its owner along.
     * <p>
     * The order is not free: the round is written down with the name of its server before the server is
     * ordered, because the bedwars server looks itself up by that name in the first second of its life.
     * Writing afterwards is a race it can lose, and a round that loses it plays the wrong map.
     *
     * @param player who is starting it
     * @param draft  what they configured, without a server name yet
     */
    public static void start(Player player, RoundData draft) {
        Refusal refusal = precheck(player);
        if (refusal != null) {
            player.sendMessage(Component.text(refusal.message(), NamedTextColor.RED));
            return;
        }
        int memory = memoryOf(RoundService.getPolicy());
        player.sendMessage(Component.text("Die Runde wird vorbereitet ...", NamedTextColor.GRAY));
        PaperContext.async(() -> {
            ServerApi.Slot slot;
            try {
                slot = ServerApi.requestSlot(memory, player.getUniqueId(), player.getName(), "bedwars");
            } catch (Exception e) {
                tell(player, "Der Host antwortet gerade nicht.", NamedTextColor.RED);
                return;
            }
            if (!slot.granted()) {
                tellFull(player, slot.capacity());
                return;
            }
            String name;
            try {
                name = ServerApi.freeName("BEDWARS");
            } catch (Exception e) {
                tell(player, "Der Server konnte nicht benannt werden: " + e.getMessage(), NamedTextColor.RED);
                return;
            }
            RoundData round = draft.copy();
            round.setServerName(name);
            round.setOwnerId(player.getUniqueId());
            round.setOwnerName(player.getName());
            round.setState(RoundState.PREPARING);
            if (!RoundService.saveBlocking(round)) {
                tell(player, "Die Runde konnte nicht gespeichert werden.", NamedTextColor.RED);
                return;
            }
            PaperContext.sync(() -> ServerStartup.createAndWarp(List.of(player), name,
                    ServerTemplate.BEDWARS, memory, null));
        });
    }

    /**
     * Says no because the machine is full - with the numbers when the player can do something with them.
     *
     * @param player   who asked
     * @param capacity what the host reported, may be {@code null}
     */
    private static void tellFull(Player player, @Nullable CapacityData capacity) {
        tell(player, "Gerade ist kein Platz für eine weitere Runde - der Server ist voll ausgelastet.",
                NamedTextColor.RED);
        if (!player.isOp() || capacity == null) return;
        tell(player, "Vergeben: " + capacity.getAllocatedMB() + " von " + capacity.getBudgetMB()
                + " MB. Abgelehnt in den letzten 7 Tagen: " + capacity.getRefusedRecently()
                + ". Details im Server Manager.", NamedTextColor.GRAY);
    }

    private static void tell(Player player, String message, NamedTextColor color) {
        PaperContext.sync(() -> {
            if (player.isOnline()) player.sendMessage(Component.text(message, color));
        });
    }

    /**
     * @param name a server name
     * @return whether it looks like a bedwars round server
     */
    public static boolean isRoundServer(String name) {
        return name != null && name.toUpperCase(Locale.ROOT).startsWith("BEDWARS");
    }
}

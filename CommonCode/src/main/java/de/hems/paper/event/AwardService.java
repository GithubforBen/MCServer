package de.hems.paper.event;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.event.ClaimAwardEvent;
import de.hems.communication.events.event.RequestAwardsEvent;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.paper.PaperContext;
import de.hems.types.event.AwardData;
import de.hems.types.event.PrizeData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Hands out the prizes waiting for a player.
 * <p>
 * Nothing is handed over until there is room for it. A prize that does not fit stays on the launcher and is
 * offered again next time, which is why the claim is only sent once the items are really in the inventory.
 */
public final class AwardService {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    /**
     * Where the money side of a prize goes, or {@code null} on a server without an economy.
     * <p>
     * It takes the player it is paying, because it is set once per server rather than once per delivery -
     * a field holding one particular player would pay the wrong person as soon as two join at the same
     * moment.
     */
    private static BiConsumer<Player, Integer> moneyGiver;

    private AwardService() {
    }

    /**
     * Tells this service how to pay out money. Called once when the plugin that owns the economy starts.
     *
     * @param giver what to call with the player and the amount, on the main thread
     */
    public static void setMoneyGiver(BiConsumer<Player, Integer> giver) {
        moneyGiver = giver;
    }

    /**
     * @param prize the prize to hand over
     * @return whether this server can pay all of it out
     */
    private static boolean canPay(PrizeData prize) {
        return prize.getMoney() <= 0 || moneyGiver != null;
    }

    /**
     * Looks for prizes a player has waiting and hands over what fits.
     *
     * @param player who just joined
     */
    public static void deliverAsync(Player player) {
        if (!PaperContext.hasPlugin()) return;
        PaperContext.async(() -> {
            List<AwardData> pending = fetch(player);
            if (pending.isEmpty()) return;
            PaperContext.sync(() -> deliver(player, pending));
        });
    }

    /**
     * @param player the player to ask about
     * @return what they still have to collect
     */
    private static List<AwardData> fetch(Player player) {
        RequestAwardsEvent request = new RequestAwardsEvent(player.getUniqueId());
        RespondDataEvent response = ListenerAdapter.ask(request, TIMEOUT);
        if (response == null || !(response.getData() instanceof List<?> list)) return List.of();
        List<AwardData> awards = new ArrayList<>();
        for (Object entry : list) {
            if (entry instanceof AwardData award) awards.add(award);
        }
        return awards;
    }

    /**
     * Hands over everything that fits, one prize after the other, on the main thread.
     *
     * @param player  who is collecting
     * @param pending what they have waiting
     */
    private static void deliver(Player player, List<AwardData> pending) {
        deliverFrom(player, pending, 0);
    }

    /**
     * Hands over one prize and moves on to the next.
     * <p>
     * The prize is reserved on the launcher first and handed over only when the launcher says yes - so a
     * player who is collecting on two servers at once, or whose confirmation got lost, cannot get it twice.
     * If it cannot be handed over after all, the reservation is given back.
     */
    private static void deliverFrom(Player player, List<AwardData> pending, int index) {
        if (index >= pending.size() || !player.isOnline()) return;
        AwardData award = pending.get(index);
        // a prize with money in it waits for a server that has an economy rather than losing the money
        if (!canPay(award.getPrize())) {
            deliverFrom(player, pending, index + 1);
            return;
        }
        if (!fits(player, toStacks(award.getPrize()))) {
            tellFull(player, award);
            return;
        }
        PaperContext.async(() -> {
            boolean reserved = claimBlocking(award);
            PaperContext.sync(() -> {
                if (!reserved) {
                    // collected somewhere else already, or the launcher did not answer - either way not here
                    deliverFrom(player, pending, index + 1);
                    return;
                }
                if (!player.isOnline() || !hand(player, award)) {
                    release(award);
                    if (player.isOnline()) tellFull(player, award);
                    return;
                }
                player.sendMessage(Component.text("★ " + award.getPlaceTitle() + " bei "
                        + award.getEventName(), NamedTextColor.GOLD));
                for (String line : award.getPrize().describe()) {
                    player.sendMessage(Component.text("  " + line, NamedTextColor.GRAY));
                }
                deliverFrom(player, pending, index + 1);
            });
        });
    }

    private static void tellFull(Player player, AwardData award) {
        player.sendMessage(Component.text("Du hast noch einen Preis offen ("
                + award.getEventName() + ") - mach Platz im Inventar.", NamedTextColor.YELLOW));
    }

    /**
     * Puts one prize into a player's hands.
     *
     * @param player who is collecting
     * @param award  the prize
     * @return whether all of it fitted - a prize is never handed over in part
     */
    private static boolean hand(Player player, AwardData award) {
        PrizeData prize = award.getPrize();
        List<ItemStack> stacks = toStacks(prize);
        // check first, hand over second: half a prize would be worse than none, because the rest is lost
        if (!fits(player, stacks)) return false;
        for (ItemStack stack : stacks) player.getInventory().addItem(stack);
        if (prize.getMoney() > 0 && moneyGiver != null) moneyGiver.accept(player, prize.getMoney());
        return true;
    }

    /**
     * @param prize the prize to turn into real items
     * @return its items, with anything the server does not know left out
     */
    private static List<ItemStack> toStacks(PrizeData prize) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Map.Entry<String, Integer> item : prize.getItems().entrySet()) {
            Material material = Material.matchMaterial(item.getKey().toUpperCase(Locale.ROOT));
            if (material == null || material.isAir()) {
                Bukkit.getLogger().warning("Unknown prize item: " + item.getKey());
                continue;
            }
            int left = item.getValue();
            // a prize may be larger than one stack, so it is split the way the inventory expects
            while (left > 0) {
                int amount = Math.min(left, material.getMaxStackSize());
                stacks.add(new ItemStack(material, amount));
                left -= amount;
            }
        }
        return stacks;
    }

    /**
     * @param player the player to check
     * @param stacks what is about to be handed over
     * @return whether there is room for all of it
     */
    private static boolean fits(Player player, List<ItemStack> stacks) {
        if (stacks.isEmpty()) return true;
        int free = 0;
        for (int slot = 0; slot < player.getInventory().getStorageContents().length; slot++) {
            ItemStack current = player.getInventory().getStorageContents()[slot];
            if (current == null || current.getType().isAir()) free++;
        }
        // one free slot per stack is the pessimistic answer, and being pessimistic here is the point
        return free >= stacks.size();
    }

    /**
     * Reserves a prize on the launcher and waits for the answer. Blocks.
     *
     * @param award the prize
     * @return whether this server may hand it over
     */
    private static boolean claimBlocking(AwardData award) {
        ClaimAwardEvent request = new ClaimAwardEvent(award.getId(), false);
        RespondDataEvent response = ListenerAdapter.ask(request, TIMEOUT);
        if (response == null) {
            // the launcher may have said yes after all, just too late - nothing was handed over here,
            // so the reservation goes back rather than leaving the prize stuck as collected
            release(award);
            return false;
        }
        return Boolean.TRUE.equals(response.getData());
    }

    /**
     * Gives a reservation back, so the prize waits for the next join.
     *
     * @param award the prize
     */
    private static void release(AwardData award) {
        PaperContext.async(() -> {
            try {
                ListenerAdapter.sendListeners(new ClaimAwardEvent(award.getId(), true));
            } catch (Exception e) {
                Bukkit.getLogger().warning("Could not give the award " + award.getId() + " back - it stays "
                        + "reserved and has to be handed over by hand: " + e.getMessage());
            }
        });
    }
}

package de.hems.paper.lotto;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.lotto.LottoRequestEvent;
import de.hems.communication.events.lotto.LottoUpdatedEvent;
import de.hems.communication.events.lotto.RespondLottoEvent;
import de.hems.communication.events.types.RespondDataEvent;
import de.hems.paper.NetworkSync;
import de.hems.paper.PaperContext;
import de.hems.paper.money.MoneyService;
import de.hems.types.lotto.LottoDraw;
import de.hems.types.lotto.LottoStatus;
import de.hems.types.lotto.LottoTicket;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The lotto on a game server: where it stands, asking the launcher, and announcing a draw to the players
 * here. Every server announces to its own players, so everybody hears it once, wherever they are.
 */
public final class LottoClient {

    /** Who may draw, and set the price and the date. Operators have it. */
    public static final String ADMIN_PERMISSION = "network.lotto.admin";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private static volatile LottoStatus status = new LottoStatus();
    private static volatile boolean loaded;
    private static boolean initialized;
    /** Run on the main thread whenever the status changes - the lobby stand hangs on this. */
    private static final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private LottoClient() {
    }

    /**
     * Starts following the lotto. Called by {@code NetworkPlugin.connect} on every server.
     *
     * @param plugin the plugin of this server
     */
    public static synchronized void init(Plugin plugin) {
        if (initialized) return;
        initialized = true;
        PaperContext.setPlugin(plugin);
        // the menu shows what a player has, so the balances have to be here too
        MoneyService.init(plugin);
        ListenerAdapter.register(LottoUpdatedEvent.class, event -> {
            LottoUpdatedEvent update = (LottoUpdatedEvent) event;
            if (update.getStatus() != null) {
                status = update.getStatus();
                loaded = true;
            }
            PaperContext.sync(() -> {
                if (update.getDraw() != null) announce(update.getDraw(), status);
                changed();
            });
        });
        NetworkSync.keepFresh(plugin, LottoClient::refreshBlocking, () -> loaded, NetworkSync.DEFAULT_REFRESH_TICKS);
    }

    private static void refreshBlocking() {
        RespondDataEvent response = ListenerAdapter.ask(
                new LottoRequestEvent(LottoRequestEvent.Action.STATUS, null, null, false), TIMEOUT);
        if (response == null || !(response.getData() instanceof LottoStatus fresh)) return;
        status = fresh;
        loaded = true;
        PaperContext.sync(LottoClient::changed);
    }

    public static LottoStatus getStatus() {
        return status;
    }

    /**
     * @param listener run on the main thread whenever the pot, the date or the last draw changes
     */
    public static void onChange(Runnable listener) {
        listeners.add(listener);
    }

    private static void changed() {
        for (Runnable listener : listeners) {
            try {
                listener.run();
            } catch (RuntimeException e) {
                Bukkit.getLogger().warning("A lotto listener failed: " + e.getMessage());
            }
        }
    }

    public static boolean isAdmin(Player player) {
        return player.hasPermission(ADMIN_PERMISSION);
    }

    /** The answer of the launcher: what came back, or why not. */
    public record Answer(Object data, String error) {
        @SuppressWarnings("unchecked")
        public List<LottoTicket> tickets() {
            return data instanceof List<?> list ? (List<LottoTicket>) list : List.of();
        }
    }

    /**
     * @return a request on behalf of this player
     */
    public static LottoRequestEvent request(Player player, LottoRequestEvent.Action action) {
        return new LottoRequestEvent(action, player.getUniqueId(), player.getName(), isAdmin(player));
    }

    /**
     * Asks the launcher off the main thread and hands the answer back on it.
     */
    public static void ask(LottoRequestEvent request, Consumer<Answer> answer) {
        PaperContext.async(() -> {
            RespondDataEvent response = ListenerAdapter.ask(request, TIMEOUT);
            Answer result = response instanceof RespondLottoEvent lotto
                    ? new Answer(lotto.getData(), lotto.getError())
                    : new Answer(null, "Das Lotto antwortet gerade nicht. Versuch es gleich noch einmal.");
            if (result.data() instanceof LottoStatus fresh) status = fresh;
            PaperContext.sync(() -> answer.accept(result));
        });
    }

    // ---- the draw --------------------------------------------------------------------------------------------

    private static void announce(LottoDraw draw, LottoStatus now) {
        String numbers = LottoTicket.format(draw.getNumbers());
        Component head = Component.text("🎱 Lotto · Ziehung ", NamedTextColor.GOLD)
                .append(Component.text(numbers, NamedTextColor.YELLOW, TextDecoration.BOLD));
        Component result;
        if (draw.getWinners().isEmpty()) {
            result = Component.text("Niemand hat alle vier. Der Topf von " + draw.getPot()
                    + " Bits geht in die nächste Runde.", NamedTextColor.GRAY);
        } else {
            result = Component.text(String.join(", ", draw.getWinners().stream().distinct().toList())
                    + " gewinn" + (draw.getWinners().size() == 1 ? "t " : "en je Tipp ")
                    + draw.getPayoutEach() + " Bits!", NamedTextColor.GREEN);
        }
        Component next = Component.text("Nächste Ziehung in " + LottoStatus.span(now.getNextDrawAt()
                - System.currentTimeMillis()) + ". Tippen mit /lotto", NamedTextColor.DARK_GRAY);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(head);
            player.sendMessage(result);
            player.sendMessage(next);
            int wins = draw.winsOf(player.getUniqueId());
            if (wins > 0) {
                player.showTitle(Title.title(Component.text("Gewonnen!", NamedTextColor.GOLD),
                        Component.text("+" + (wins * draw.getPayoutEach()) + " Bits", NamedTextColor.YELLOW)));
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            } else {
                player.showTitle(Title.title(Component.text(numbers, NamedTextColor.YELLOW),
                        Component.text("Die Lottozahlen", NamedTextColor.GRAY),
                        Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(700))));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.2f);
            }
        }
    }
}

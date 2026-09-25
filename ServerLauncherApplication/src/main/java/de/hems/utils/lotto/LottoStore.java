package de.hems.utils.lotto;

import de.hems.types.lotto.LottoDraw;
import de.hems.types.lotto.LottoTicket;
import de.hems.utils.YamlFiles;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The lotto on disk: {@code lotto.yml} next to the launcher - the settings, the pot, the tips of the round
 * that is running and the last draws.
 * <p>
 * Not thread safe on its own; {@link LottoService} holds the lock.
 */
public class LottoStore {

    /** How many past draws are kept. */
    static final int HISTORY = 20;

    private final File file;
    private final YamlConfiguration config;
    private final List<LottoTicket> tickets = new ArrayList<>();
    private final List<LottoDraw> history = new ArrayList<>();

    public LottoStore() {
        this(new File("./lotto.yml"));
    }

    public LottoStore(File file) {
        this.file = file;
        this.config = YamlFiles.load(file);
        if (!config.contains("settings.price")) config.set("settings.price", 100);
        if (!config.contains("settings.schedule")) config.set("settings.schedule", "SONNTAG 20:00");
        if (!config.contains("settings.zone")) config.set("settings.zone", "Europe/Berlin");
        if (!config.contains("round")) config.set("round", 1);
        for (Map<?, ?> line : config.getMapList("tickets")) {
            LottoTicket ticket = readTicket(line);
            if (ticket != null) tickets.add(ticket);
        }
        for (Map<?, ?> line : config.getMapList("history")) {
            LottoDraw draw = readDraw(line);
            if (draw != null) history.add(draw);
        }
    }

    // ---- settings and state ----------------------------------------------------------------------------------

    public int getPrice() {
        return config.getInt("settings.price", 100);
    }

    public void setPrice(int price) {
        config.set("settings.price", price);
    }

    public String getSchedule() {
        return config.getString("settings.schedule", "SONNTAG 20:00");
    }

    public void setSchedule(String schedule) {
        config.set("settings.schedule", schedule);
    }

    public String getZone() {
        return config.getString("settings.zone", "Europe/Berlin");
    }

    public int getRound() {
        return config.getInt("round", 1);
    }

    public void setRound(int round) {
        config.set("round", round);
    }

    public int getPot() {
        return config.getInt("pot", 0);
    }

    public void setPot(int pot) {
        config.set("pot", pot);
    }

    public long getNextDrawAt() {
        return config.getLong("next-draw-at", 0L);
    }

    public void setNextDrawAt(long at) {
        config.set("next-draw-at", at);
    }

    public long nextTicketId() {
        long next = config.getLong("next-ticket-id", 1L);
        config.set("next-ticket-id", next + 1);
        return next;
    }

    // ---- tips and draws --------------------------------------------------------------------------------------

    public List<LottoTicket> getTickets() {
        return new ArrayList<>(tickets);
    }

    public void addTicket(LottoTicket ticket) {
        tickets.add(ticket);
    }

    public void clearTickets() {
        tickets.clear();
    }

    /**
     * @return the past draws, newest first
     */
    public List<LottoDraw> getHistory() {
        return new ArrayList<>(history);
    }

    public void addDraw(LottoDraw draw) {
        history.addFirst(draw);
        while (history.size() > HISTORY) history.removeLast();
    }

    /**
     * Writes everything. Called once per change, under the service's lock.
     */
    public void save() {
        List<Map<String, Object>> ticketLines = new ArrayList<>();
        for (LottoTicket ticket : tickets) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("id", ticket.getId());
            line.put("round", ticket.getRound());
            line.put("player", ticket.getPlayer().toString());
            line.put("name", ticket.getPlayerName());
            line.put("numbers", toList(ticket.getNumbers()));
            line.put("at", ticket.getBoughtAt());
            ticketLines.add(line);
        }
        config.set("tickets", ticketLines);
        List<Map<String, Object>> drawLines = new ArrayList<>();
        for (LottoDraw draw : history) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("round", draw.getRound());
            line.put("numbers", toList(draw.getNumbers()));
            line.put("at", draw.getDrawnAt());
            line.put("pot", draw.getPot());
            line.put("tickets", draw.getTickets());
            line.put("each", draw.getPayoutEach());
            List<String> winners = new ArrayList<>();
            for (int i = 0; i < draw.getWinners().size(); i++) {
                winners.add(draw.getWinnerIds().get(i) + "|" + draw.getWinners().get(i));
            }
            line.put("winners", winners);
            drawLines.add(line);
        }
        config.set("history", drawLines);
        YamlFiles.saveOrLog(config, file);
    }

    private static List<Integer> toList(int[] numbers) {
        List<Integer> list = new ArrayList<>();
        for (int number : numbers) list.add(number);
        return list;
    }

    private static int[] toArray(Object value) {
        if (!(value instanceof List<?> list)) return null;
        int[] numbers = new int[list.size()];
        for (int i = 0; i < numbers.length; i++) {
            if (!(list.get(i) instanceof Number number)) return null;
            numbers[i] = number.intValue();
        }
        return numbers;
    }

    private static long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static LottoTicket readTicket(Map<?, ?> line) {
        int[] numbers = LottoTicket.normalize(toArray(line.get("numbers")));
        if (numbers == null) return null;
        try {
            return new LottoTicket(number(line.get("id")), (int) number(line.get("round")),
                    UUID.fromString(String.valueOf(line.get("player"))), String.valueOf(line.get("name")),
                    numbers, number(line.get("at")));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static LottoDraw readDraw(Map<?, ?> line) {
        int[] numbers = toArray(line.get("numbers"));
        if (numbers == null) return null;
        List<String> names = new ArrayList<>();
        List<UUID> ids = new ArrayList<>();
        if (line.get("winners") instanceof List<?> winners) {
            for (Object winner : winners) {
                String[] parts = String.valueOf(winner).split("\\|", 2);
                if (parts.length != 2) continue;
                try {
                    ids.add(UUID.fromString(parts[0]));
                    names.add(parts[1]);
                } catch (IllegalArgumentException ignored) {
                    // a winner nobody can be found for is left out of the record, not out of the payout
                }
            }
        }
        return new LottoDraw((int) number(line.get("round")), numbers, number(line.get("at")),
                (int) number(line.get("pot")), (int) number(line.get("tickets")), names, ids,
                (int) number(line.get("each")));
    }
}

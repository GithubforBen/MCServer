package de.hems.utils.event;

import de.hems.types.event.EventData;
import de.hems.types.event.EventRewards;
import de.hems.types.event.EventSetting;
import de.hems.types.event.EventType;
import de.hems.types.event.PokerEventSettings;
import de.hems.types.event.PrizeData;
import de.hems.types.event.RewardRule;
import de.hems.types.poker.PokerFormat;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * The settings and rewards of an event as the website edits them.
 * <p>
 * The game changes an event one click at a time; the website sends the whole form at once. This is the
 * translation between the two, and it checks everything that comes in against the same presets the game
 * offers - a number on the website is a choice from a list too, so a buy-in cannot end up at 100000 because
 * somebody typed one zero too many. The keys that tie an event to its server are never part of the form, so
 * a save from the website cannot cut a running round loose from its event.
 */
public final class EventForm {

    /** What an item name has to look like before it is even compared with the real list. */
    private static final Pattern MATERIAL = Pattern.compile("[A-Z0-9_]+");
    /** Plenty for a prize, and far below anything that overflows when it is added up. */
    private static final int MAX_MONEY = 1_000_000_000;
    /** Twenty-seven stacks of sixty-four, a full chest. */
    private static final int MAX_ITEM_AMOUNT = 27 * 64;

    private EventForm() {
    }

    /**
     * One choice of a setting.
     *
     * @param value what is stored
     * @param label what is shown
     */
    private record Option(String value, String label) {
    }

    /**
     * One setting as the form shows it.
     *
     * @param key         what it is sent under
     * @param title       what it is called
     * @param toggle      whether it is a switch rather than a list
     * @param value       what it is now
     * @param options     what it may be, for a list
     * @param description what it does
     * @param write       how a checked value is written onto an event
     */
    private record Field(String key, String title, boolean toggle, String value, List<Option> options,
                         List<String> description, BiConsumer<EventData, String> write) {

        JSONObject toJson() {
            JSONArray choices = new JSONArray();
            for (Option option : options) {
                choices.put(new JSONObject().put("value", option.value()).put("label", option.label()));
            }
            return new JSONObject()
                    .put("key", key)
                    .put("title", title)
                    .put("kind", toggle ? "toggle" : "choice")
                    .put("value", value)
                    .put("options", choices)
                    .put("description", new JSONArray(description));
        }

        boolean allows(String candidate) {
            if (toggle) return candidate.equals("true") || candidate.equals("false");
            for (Option option : options) {
                if (option.value().equals(candidate)) return true;
            }
            return false;
        }
    }

    /* ------------------------------------------------------------------------------------ settings */

    /**
     * @param event the event
     * @return its settings with their current values, in the order the game shows them
     */
    public static JSONArray describeSettings(EventData event) {
        JSONArray array = new JSONArray();
        for (Field field : fieldsOf(event)) array.put(field.toJson());
        return array;
    }

    /**
     * Writes the settings the form sent onto an event. Keys the form does not know are ignored, a value
     * that is not one of the choices refuses the whole save.
     *
     * @param event  the event to change, a copy
     * @param values the values by key, as the form sent them
     * @return what is wrong, or {@code null} when everything was written
     */
    public static String applySettings(EventData event, JSONObject values) {
        if (values == null) return null;
        List<Field> fields = fieldsOf(event);
        for (Field field : fields) {
            if (!values.has(field.key()) || values.isNull(field.key())) continue;
            String value = String.valueOf(values.get(field.key())).trim();
            if (!field.allows(value)) return "'" + value + "' ist für " + field.title() + " nicht erlaubt.";
        }
        // checked first and written second, so a refused save leaves nothing half done - and in the order
        // of the list, because the qualifying volume of a poker night is counted in the buy-in before it
        for (Field field : fields) {
            if (!values.has(field.key()) || values.isNull(field.key())) continue;
            field.write().accept(event, String.valueOf(values.get(field.key())).trim());
        }
        return null;
    }

    /**
     * Writes the defaults of every setting onto a new event, the way creating it in the game does, so what
     * the form shows is what is stored.
     *
     * @param event the new event
     */
    public static void applyDefaults(EventData event) {
        EventSetting.applyDefaults(event, event.getType().getSettings());
        if (event.getType() == EventType.POKER) new PokerEventSettings(event).applyDefaults();
    }

    private static List<Field> fieldsOf(EventData event) {
        if (event.getType() == EventType.POKER) return pokerFields(event);
        List<Field> fields = new ArrayList<>();
        for (EventSetting setting : event.getType().getSettings()) {
            int current = setting.read(event);
            if (setting.getKind() == EventSetting.Kind.TOGGLE) {
                fields.add(new Field(setting.getKey(), setting.getTitle(), true, String.valueOf(current == 1),
                        List.of(), setting.getDescription(),
                        (target, value) -> target.setSetting(setting.getKey(), value)));
                continue;
            }
            List<Option> options = new ArrayList<>();
            for (int choice : withCurrent(setting.getChoices(), current)) {
                options.add(new Option(String.valueOf(choice), setting.format(choice)));
            }
            fields.add(new Field(setting.getKey(), setting.getTitle(), false, String.valueOf(current), options,
                    setting.getDescription(), (target, value) -> target.setSetting(setting.getKey(), value)));
        }
        return fields;
    }

    /**
     * The knobs of a poker night, the same ones and the same steps as its panel in the game.
     */
    private static List<Field> pokerFields(EventData event) {
        PokerEventSettings poker = new PokerEventSettings(event);
        List<Field> fields = new ArrayList<>();

        List<Option> formats = new ArrayList<>();
        for (PokerFormat format : PokerFormat.values()) {
            formats.add(new Option(format.name(), format.getTitle()));
        }
        fields.add(new Field(PokerEventSettings.FORMAT, "Format", false, poker.getFormat().name(), formats,
                List.of("Cash Game: feste Blinds, jederzeit rein und raus.",
                        "Turnier: ein Buy-in, steigende Blinds."),
                (target, value) -> new PokerEventSettings(target).setFormat(PokerFormat.byName(value, null))));

        fields.add(numbers(PokerEventSettings.BUY_IN, "Buy-in", poker.getBuyIn(),
                PokerEventSettings.BUY_IN_STEPS, value -> value + " Bits",
                List.of("So viele Chips bekommt man dafür - eins zu eins."),
                (target, value) -> new PokerEventSettings(target).setBuyIn(value)));
        fields.add(numbers(PokerEventSettings.SMALL_BLIND, "Blinds", poker.getSmallBlind(),
                PokerEventSettings.SMALL_BLIND_STEPS, value -> value + "/" + (value * 2),
                List.of("Small Blind / Big Blind."),
                (target, value) -> new PokerEventSettings(target).setSmallBlind(value)));
        fields.add(numbers(PokerEventSettings.RAKE_PERMILLE, "Hausanteil", poker.getRakePermille(),
                PokerEventSettings.RAKE_PERMILLE_STEPS, EventForm::percent,
                List.of("Pro Pot, nur aus Pots, um die wirklich gespielt wurde."),
                (target, value) -> new PokerEventSettings(target).setRakePermille(value)));
        fields.add(numbers(PokerEventSettings.RAKE_CAP_BB, "Deckel für den Hausanteil",
                poker.getRakeCapBigBlinds(), PokerEventSettings.RAKE_CAP_BB_STEPS,
                value -> value == 0 ? "kein Deckel" : value + " Big Blinds",
                List.of("Ohne Deckel kostet ein einziger großer Pot mehr als der ganze restliche Abend."),
                (target, value) -> new PokerEventSettings(target).setRakeCapBigBlinds(value)));
        fields.add(numbers(PokerEventSettings.SEATS, "Plätze pro Tisch", poker.getSeats(),
                range(PokerEventSettings.MIN_SEATS, PokerEventSettings.MAX_SEATS), String::valueOf, List.of(),
                (target, value) -> new PokerEventSettings(target).setSeats(value)));
        fields.add(numbers(PokerEventSettings.TABLES, "Tische", poker.getTables(),
                range(1, PokerEventSettings.MAX_TABLES), String::valueOf, List.of(),
                (target, value) -> new PokerEventSettings(target).setTables(value)));
        fields.add(new Field(PokerEventSettings.BOTS, "Bots erlaubt", true, String.valueOf(poker.isBotsAllowed()),
                List.of(), List.of("Wer einen setzt, bezahlt seinen Stack und bekommt zurück, was übrig ist."),
                (target, value) -> new PokerEventSettings(target).setBotsAllowed(Boolean.parseBoolean(value))));
        fields.add(numbers(PokerEventSettings.BOT_FEE, "Gebühr pro Bot", poker.getBotFee(),
                PokerEventSettings.BOT_FEE_STEPS, value -> value + " Bits",
                List.of("Kommt nicht zurück."),
                (target, value) -> new PokerEventSettings(target).setBotFee(value)));
        fields.add(numbers(PokerEventSettings.MIN_HANDS, "Wertung: Mindesthände", poker.getMinHands(),
                PokerEventSettings.MIN_HANDS_STEPS, value -> value + " Hände",
                List.of("Wer weniger gespielt hat, kommt nicht in die Rangliste."),
                (target, value) -> new PokerEventSettings(target).setMinHands(value)));
        // stored in bits but chosen in buy-ins, like in the game, so it keeps meaning the same when the
        // buy-in changes in the same save
        int buyIns = poker.getMinVolume() / Math.max(1, poker.getBuyIn());
        fields.add(numbers(PokerEventSettings.MIN_VOLUME, "Wertung: Mindesteinsatz", buyIns,
                range(0, PokerEventSettings.MAX_MIN_VOLUME_BUY_INS),
                value -> value == 0 ? "aus" : value + (value == 1 ? " Buy-in" : " Buy-ins"),
                List.of("Braucht man normalerweise nicht - gewertet wird der Gewinn."),
                (target, value) -> {
                    PokerEventSettings settings = new PokerEventSettings(target);
                    settings.setMinVolume(value * settings.getBuyIn());
                }));
        fields.add(numbers(PokerEventSettings.BLIND_UP_MINUTES, "Blinds steigen alle", poker.getBlindUpMinutes(),
                PokerEventSettings.BLIND_UP_STEPS, value -> value + " Min", List.of("Nur im Turnier."),
                (target, value) -> new PokerEventSettings(target).setBlindUpMinutes(value)));
        return fields;
    }

    private interface IntWriter {
        void write(EventData event, int value);
    }

    private static Field numbers(String key, String title, int current, int[] steps,
                                 IntFunction<String> label, List<String> description,
                                 IntWriter write) {
        List<Option> options = new ArrayList<>();
        for (int step : withCurrent(steps, current)) {
            options.add(new Option(String.valueOf(step), label.apply(step)));
        }
        return new Field(key, title, false, String.valueOf(current), options, description,
                (target, value) -> write.write(target, Integer.parseInt(value)));
    }

    /**
     * @param steps   the presets
     * @param current the value stored now
     * @return the presets, with the current value added in order if it is not one of them - a value set
     *         some other way stays selectable instead of being changed by merely saving the form
     */
    private static int[] withCurrent(int[] steps, int current) {
        for (int step : steps) {
            if (step == current) return steps;
        }
        int[] all = Arrays.copyOf(steps, steps.length + 1);
        all[steps.length] = current;
        Arrays.sort(all);
        return all;
    }

    private static int[] range(int from, int to) {
        int[] values = new int[to - from + 1];
        for (int i = 0; i < values.length; i++) values[i] = from + i;
        return values;
    }

    private static String percent(int permille) {
        if (permille % 10 == 0) return (permille / 10) + "%";
        return (permille / 10) + "," + (permille % 10) + "%";
    }

    /* ------------------------------------------------------------------------------------- rewards */

    /**
     * @param event the event
     * @return its rewards, in order
     */
    public static JSONArray describeRewards(EventData event) {
        JSONArray array = new JSONArray();
        for (RewardRule rule : EventRewards.of(event)) {
            JSONArray items = new JSONArray();
            for (Map.Entry<String, Integer> item : rule.getPrize().getItems().entrySet()) {
                items.put(new JSONObject().put("material", item.getKey()).put("amount", item.getValue()));
            }
            array.put(new JSONObject()
                    .put("who", rule.getCondition().name())
                    .put("from", rule.getFrom())
                    .put("to", rule.getTo())
                    .put("kills", rule.getKills())
                    .put("label", rule.describeWho())
                    .put("money", rule.getPrize().getMoney())
                    .put("items", items));
        }
        return array;
    }

    /**
     * Replaces the rewards of an event with the ones the form sent.
     *
     * @param event     the event to change, a copy
     * @param rewards   the rewards in order, as the form sent them
     * @param materials the item names that exist, or an empty list when no game server could say - only
     *                  asked for when there is an item to check, because asking can take a moment
     * @return what is wrong, or {@code null} when they were written
     */
    public static String applyRewards(EventData event, JSONArray rewards,
                                      Supplier<? extends Collection<String>> materials) {
        if (rewards == null) return null;
        EventType type = event.getType();
        if (!type.isRanked() && !rewards.isEmpty()) {
            return type.getTitle() + " wertet niemanden - hier gibt es keine Belohnungen.";
        }
        if (rewards.length() > EventRewards.MAX_RULES) {
            return "Höchstens " + EventRewards.MAX_RULES + " Belohnungen pro Event.";
        }
        List<RewardRule> rules = new ArrayList<>();
        Collection<String> known = null;
        for (int i = 0; i < rewards.length(); i++) {
            String where = "Belohnung " + (i + 1) + ": ";
            JSONObject json = rewards.optJSONObject(i);
            if (json == null) return where + "kann nicht gelesen werden.";

            PrizeData prize = new PrizeData();
            long money = json.optLong("money", 0L);
            if (money < 0 || money > MAX_MONEY) return where + "Geld muss zwischen 0 und " + MAX_MONEY + " liegen.";
            prize.setMoney((int) money);
            JSONArray items = json.optJSONArray("items");
            if (items != null) {
                for (int j = 0; j < items.length(); j++) {
                    JSONObject item = items.optJSONObject(j);
                    if (item == null) return where + "ein Item kann nicht gelesen werden.";
                    String material = normaliseMaterial(item.optString("material", ""));
                    if (material.isEmpty()) continue;
                    if (known == null) known = materials.get();
                    if (!MATERIAL.matcher(material).matches() || (!known.isEmpty() && !known.contains(material))) {
                        return where + "'" + material + "' ist kein Minecraft-Item.";
                    }
                    int amount = item.optInt("amount", 0);
                    if (amount < 1 || amount > MAX_ITEM_AMOUNT) {
                        return where + "die Anzahl von " + material + " muss zwischen 1 und " + MAX_ITEM_AMOUNT
                                + " liegen.";
                    }
                    prize.withItem(material, amount);
                }
            }
            if (prize.isEmpty()) return where + "gibt weder Geld noch Items.";

            RewardRule.Condition condition;
            try {
                condition = RewardRule.Condition.valueOf(json.optString("who", "").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return where + "es ist nicht festgelegt, wer sie bekommt.";
            }
            switch (condition) {
                case PARTICIPATION -> rules.add(RewardRule.participation(prize));
                case KILLS -> {
                    if (!type.countsKills()) return where + type.getTitle() + " zählt keine Kills.";
                    int kills = json.optInt("kills", 0);
                    if (kills < 1) return where + "es braucht mindestens einen Kill.";
                    rules.add(RewardRule.kills(kills, prize));
                }
                case PLACE -> {
                    int from = json.optInt("from", 0);
                    int to = json.optInt("to", from);
                    if (from < 1) return where + "der beste Platz muss mindestens 1 sein.";
                    if (to != RewardRule.OPEN_END && to < from) {
                        return where + "der letzte Platz liegt vor dem ersten.";
                    }
                    rules.add(RewardRule.places(from, to, prize));
                }
            }
        }
        EventRewards.set(event, rules);
        return null;
    }

    /**
     * @param text an item name as somebody typed it, with or without the namespace
     * @return it the way bukkit spells it
     */
    static String normaliseMaterial(String text) {
        String material = text == null ? "" : text.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (material.startsWith("MINECRAFT:")) material = material.substring("MINECRAFT:".length());
        return material;
    }
}

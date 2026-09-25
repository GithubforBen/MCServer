package de.hems.types.event;

import de.hems.types.Presets;
import java.io.Serializable;
import java.util.List;

/**
 * One knob of an event, described rather than drawn.
 * <p>
 * An event type lists its knobs as these, and the one settings panel draws all of them - a switch for a
 * toggle, a button that steps through presets for a number. That is the whole cost of a new setting: a key,
 * a title and the values it may take. Nobody has to build a panel, and every event's settings sit in the
 * same place and behave the same way.
 * <p>
 * Numbers step through presets instead of being typed, for the same reason the rest of the event panels do:
 * a number typed into chat is how a border ends up at 50000 because a finger slipped.
 * <p>
 * The icon is a material name rather than a bukkit type, because this class is also read by the launcher,
 * which has no bukkit to resolve it with.
 */
public final class EventSetting implements Serializable {

    private static final long serialVersionUID = 4342L;

    /** What sort of knob it is. */
    public enum Kind {
        /** On or off. */
        TOGGLE,
        /** One of a list of numbers. */
        CHOICE
    }

    private final String key;
    private final String title;
    private final String icon;
    private final Kind kind;
    private final int defaultValue;
    private final int[] choices;
    private final String unit;
    private final String zeroLabel;
    private final List<String> description;

    private EventSetting(String key, String title, String icon, Kind kind, int defaultValue, int[] choices,
                         String unit, String zeroLabel, List<String> description) {
        this.key = key;
        this.title = title;
        this.icon = icon;
        this.kind = kind;
        this.defaultValue = defaultValue;
        this.choices = choices;
        this.unit = unit;
        this.zeroLabel = zeroLabel;
        this.description = List.copyOf(description);
    }

    /**
     * @param key          the settings key it is stored under
     * @param title        what it is called on the button
     * @param icon         the material name of the button
     * @param defaultValue what it is until somebody changes it
     * @param description  what it does, one line each
     * @return a switch
     */
    public static EventSetting toggle(String key, String title, String icon, boolean defaultValue,
                                      String... description) {
        return new EventSetting(key, title, icon, Kind.TOGGLE, defaultValue ? 1 : 0, new int[]{0, 1},
                "", null, List.of(description));
    }

    /**
     * @param key          the settings key it is stored under
     * @param title        what it is called on the button
     * @param icon         the material name of the button
     * @param defaultValue what it is until somebody changes it
     * @param unit         what the number counts, like "Min" or "Blöcke", written after it
     * @param zeroLabel    what zero means, like "aus" - or {@code null} to show it as a number
     * @param choices      the values a click steps through, in order
     * @param description  what it does, one line each
     * @return a stepped number
     */
    public static EventSetting choice(String key, String title, String icon, int defaultValue, String unit,
                                      String zeroLabel, int[] choices, String... description) {
        return new EventSetting(key, title, icon, Kind.CHOICE, defaultValue, choices.clone(),
                unit == null ? "" : unit, zeroLabel, List.of(description));
    }

    /**
     * @param event the event to read
     * @return the current value as a number, a toggle being one or zero
     */
    public int read(EventData event) {
        if (kind == Kind.TOGGLE) return event.getFlag(key, defaultValue == 1) ? 1 : 0;
        return event.getNumber(key, defaultValue);
    }

    /**
     * Writes the value down, but only if it is not there yet. Called when an event is created, so what the
     * panel shows is what is stored rather than a default that could change under it with the next update.
     *
     * @param event the event to fill
     */
    public void applyDefault(EventData event) {
        if (event.getSetting(key, null) != null) return;
        write(event, defaultValue);
    }

    /**
     * Moves one step on.
     *
     * @param event   the event to change
     * @param forward whether to go up or down the list
     */
    public void step(EventData event, boolean forward) {
        int current = read(event);
        if (kind == Kind.TOGGLE) {
            write(event, current == 1 ? 0 : 1);
            return;
        }
        write(event, Presets.step(choices, current, forward));
    }

    private void write(EventData event, int value) {
        event.setSetting(key, kind == Kind.TOGGLE ? String.valueOf(value == 1) : String.valueOf(value));
    }

    /**
     * @param event the event to read
     * @return the value the way a player reads it, like "an", "5 Min" or "aus"
     */
    public String display(EventData event) {
        return format(read(event));
    }

    /**
     * @param value a value of this setting
     * @return it written out
     */
    public String format(int value) {
        if (kind == Kind.TOGGLE) return value == 1 ? "an" : "aus";
        if (value == 0 && zeroLabel != null) return zeroLabel;
        return unit.isEmpty() ? String.valueOf(value) : value + " " + unit;
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    public String getIcon() {
        return icon;
    }

    public Kind getKind() {
        return kind;
    }

    public List<String> getDescription() {
        return description;
    }

    /**
     * Fills every setting that is not there yet.
     *
     * @param event    the event
     * @param settings its knobs
     */
    public static void applyDefaults(EventData event, List<EventSetting> settings) {
        for (EventSetting setting : settings) setting.applyDefault(event);
    }
}

package de.hems.paper.event;

import de.hems.types.event.EventData;
import de.hems.types.event.EventSetting;
import de.hems.types.event.EventType;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Everything the event panels need to know about one kind of event.
 * <p>
 * This is the template a new event is built from. Most kinds need nothing but their icon and their list of
 * {@link EventSetting}s - the settings panel, the rewards panel, the calendar and the create panel all read
 * from here and draw the same buttons in the same places for every event. Only a kind whose knobs depend on
 * each other, like the poker night, brings a panel of its own.
 *
 * <pre>{@code
 * EventDefinitions.register(EventDefinition.of(EventType.MY_EVENT, Material.BOW)
 *         .settings(MyEventSettings.SETTINGS));
 * }</pre>
 */
public final class EventDefinition {

    /** A panel of its own, for the kinds whose knobs do not fit the general one. */
    @FunctionalInterface
    public interface SettingsPanel {
        /**
         * @param player  who is editing
         * @param working the event, changed in place
         * @param back    what to call with it when the panel is left
         */
        void open(Player player, EventData working, Consumer<EventData> back);
    }

    private final EventType type;
    private final Material icon;
    private List<EventSetting> settings = List.of();
    private SettingsPanel panel;
    private Consumer<EventData> defaults;
    private Function<EventData, List<String>> summary;

    private EventDefinition(EventType type, Material icon) {
        this.type = type;
        this.icon = icon;
    }

    /**
     * @param type the kind of event
     * @param icon what it looks like in the calendar
     * @return a definition with no knobs, to add to
     */
    public static EventDefinition of(EventType type, Material icon) {
        return new EventDefinition(type, icon);
    }

    /**
     * @param settings the knobs, drawn by the general settings panel in this order
     * @return this definition
     */
    public EventDefinition settings(List<EventSetting> settings) {
        this.settings = List.copyOf(settings);
        return this;
    }

    /**
     * @param panel a panel of its own, used instead of the general one
     * @return this definition
     */
    public EventDefinition panel(SettingsPanel panel) {
        this.panel = panel;
        return this;
    }

    /**
     * @param defaults what to write onto a new event beyond the defaults of its settings
     * @return this definition
     */
    public EventDefinition defaults(Consumer<EventData> defaults) {
        this.defaults = defaults;
        return this;
    }

    /**
     * @param summary the lines that describe the settings on a button, instead of one per setting
     * @return this definition
     */
    public EventDefinition summary(Function<EventData, List<String>> summary) {
        this.summary = summary;
        return this;
    }

    public EventType getType() {
        return type;
    }

    public Material getIcon() {
        return icon;
    }

    public List<EventSetting> getSettings() {
        return settings;
    }

    /**
     * @return whether there is anything to set on this kind at all
     */
    public boolean hasSettings() {
        return panel != null || !settings.isEmpty();
    }

    /**
     * @return the panel of its own, or {@code null} when the general one is used
     */
    public SettingsPanel getPanel() {
        return panel;
    }

    /**
     * Writes every default that is not there yet, so what the panel shows is what is stored.
     *
     * @param event the event to fill
     */
    public void applyDefaults(EventData event) {
        EventSetting.applyDefaults(event, settings);
        if (defaults != null) defaults.accept(event);
    }

    /**
     * @param event the event to describe
     * @return its settings, one line each, uncoloured
     */
    public List<String> describe(EventData event) {
        if (summary != null) return summary.apply(event);
        List<String> lines = new ArrayList<>();
        for (EventSetting setting : settings) {
            lines.add(setting.getTitle() + ": " + setting.display(event));
        }
        return lines;
    }
}

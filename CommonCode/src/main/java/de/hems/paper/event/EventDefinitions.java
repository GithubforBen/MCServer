package de.hems.paper.event;

import de.hems.types.event.BedwarsEventSettings;
import de.hems.types.event.EventType;
import de.hems.types.event.HungerGamesSettings;
import de.hems.types.event.PokerEventSettings;
import de.hems.types.event.UhcSettings;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The definitions of every kind of event, looked up by type.
 * <p>
 * Adding a kind of event is one entry here (see {@link EventDefinition}). A type without an entry still
 * works - it simply has no settings and a paper icon.
 */
public final class EventDefinitions {

    private static final Map<EventType, EventDefinition> definitions = new EnumMap<>(EventType.class);

    static {
        register(EventDefinition.of(EventType.SIMPLE, Material.PAPER));
        register(EventDefinition.of(EventType.OTHER_WORLD, Material.GRASS_BLOCK));
        register(EventDefinition.of(EventType.END, Material.END_PORTAL_FRAME));
        register(EventDefinition.of(EventType.UHC_BOSSES, Material.NETHER_STAR).settings(UhcSettings.SETTINGS));
        register(EventDefinition.of(EventType.UHC_DRAGON, Material.DRAGON_HEAD).settings(UhcSettings.SETTINGS));
        register(EventDefinition.of(EventType.BEDWARS, Material.RED_BED).settings(BedwarsEventSettings.SETTINGS));
        // the poker knobs lean on each other - the qualifying volume follows the buy-in - so they keep their
        // own panel, reached through the same button as everybody else's
        register(EventDefinition.of(EventType.POKER, Material.PLAYER_HEAD)
                .panel((player, working, back) ->
                        player.openInventory(PokerSettingsUi.build(player, working, back).getInventory()))
                .defaults(event -> new PokerEventSettings(event).applyDefaults())
                .summary(event -> {
                    PokerEventSettings poker = new PokerEventSettings(event);
                    return List.of("Format: " + poker.getFormat().getTitle(),
                            "Buy-in: " + poker.getBuyIn() + " Bits",
                            "Blinds: " + poker.getSmallBlind() + "/" + poker.getBigBlind(),
                            "Haus: " + poker.getRakeText());
                }));
        register(EventDefinition.of(EventType.HUNGER_GAMES, Material.BOW).settings(HungerGamesSettings.SETTINGS));
    }

    private EventDefinitions() {
    }

    /**
     * @param definition the kind to add, replacing one of the same type
     */
    public static void register(EventDefinition definition) {
        definitions.put(definition.getType(), definition);
    }

    /**
     * @param type the kind of event
     * @return its definition, a bare one if nothing was registered
     */
    public static EventDefinition of(EventType type) {
        EventDefinition definition = definitions.get(type);
        return definition != null ? definition : EventDefinition.of(type, Material.PAPER);
    }
}

package de.schnorrenbergers.survival.featrues.lab.item;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** The words on lab items, with nothing from Bukkit so a check can read them. */
public final class LabText {

    private static final String[] PURITY = {"", "Dreckig", "Schlecht", "Ordentlich", "Rein", "Makellos"};

    private LabText() {
    }

    public static String purityName(int purity) {
        return PURITY[purity];
    }

    /** The item's name as players see it, for example "Rohpaste (Dreckig)". */
    public static String title(LabData data) {
        String name = data.kind().label();
        if (data.kind().hasPurity()) name += " (" + purityName(data.purity()) + ")";
        return name;
    }

    /** The item's description lines: purity, effects, and the next step. */
    public static List<String> lore(LabData data) {
        List<String> lines = new ArrayList<>();
        if (data.kind().hasPurity()) lines.add("Reinheit: " + data.purity() + " von 5 - " + purityName(data.purity()));
        if (data.stretched()) lines.add("Gestreckt");
        if (!data.effects().isEmpty()) {
            lines.add("Geschnitten mit: " + data.effects().stream().map(LabEffect::label)
                    .collect(Collectors.joining(", ")));
        }
        lines.add("Nächster Schritt: " + nextStep(data));
        return lines;
    }

    /** What to do with the item next. */
    public static String nextStep(LabData data) {
        return switch (data.kind()) {
            case PASTE -> switch (data.purity()) {
                case 1 -> "Papierfilter reinigt sie weiter. Feenstaub macht Kokain daraus.";
                case 2 -> "Kohlefilter reinigt sie weiter. Feenstaub macht Kokain daraus.";
                case 3 -> "Seidenfilter reinigt sie weiter. Feenstaub macht Kokain daraus.";
                case 4 -> "Goldfilter reinigt sie weiter. Feenstaub macht Kokain daraus.";
                default -> "Feenstaub macht Kokain daraus.";
            };
            case KOKAIN -> {
                String rest = data.stretched() || data.purity() < 2
                        ? "Zutaten oben schneiden es mit Effekten."
                        : "Zucker streckt es, Zutaten oben schneiden es mit Effekten.";
                yield "Rechtsklick konsumiert es. " + rest;
            }
            case PAPIERFILTER, KOHLEFILTER, SEIDENFILTER, GOLDFILTER ->
                    "Oben im Braustand reinigt er Paste der Reinheit " + data.kind().filterTier() + ".";
            case FEENSTAUB -> "Oben im Braustand macht er aus Paste Kokain.";
            case PFLANZENABFALL -> "Wertlos. Aussortieren und wegwerfen.";
        };
    }
}

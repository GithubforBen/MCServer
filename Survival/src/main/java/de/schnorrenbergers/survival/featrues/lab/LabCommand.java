package de.schnorrenbergers.survival.featrues.lab;

import de.schnorrenbergers.survival.featrues.lab.LabSettings.Yield;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@code /labor}: the production tree in chat, from sugar cane to Kokain. Every number is read from the
 * settings, so the text follows {@code configs/lab.yml}. see knowledge: /capabilities/lab-brewing.md
 */
public final class LabCommand implements CommandExecutor {

    private static final String[] FILTERS = {"Papierfilter", "Kohlefilter", "Seidenfilter", "Goldfilter"};

    private final LabSettings settings;

    public LabCommand(LabSettings settings) {
        this.settings = settings;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String @NotNull [] args) {
        for (String line : lines()) sender.sendMessage(line);
        return true;
    }

    /** The lines of the tree, separate from the sender so the text can be read on its own. */
    List<String> lines() {
        ChatColor head = ChatColor.GOLD;
        ChatColor text = ChatColor.GRAY;
        List<String> lines = new ArrayList<>();
        lines.add(head + "Das Labor" + text + " - von Zuckerrohr zu Kokain");
        lines.add(text + "Alles passiert im Braustand: Zutat oben, Brennstoff, Ergebnisse unten. Die Slots unten bleiben leer, Flaschen brauchst du nicht.");
        lines.add(head + "1. Rohpaste" + text + ": Zuckerrohr oben, " + chances(settings.sugarCane()) + " pro leerem Slot, ein Zuckerrohr je Slot.");
        for (int tier = 1; tier <= 4; tier++) {
            lines.add(head + "" + (tier + 1) + ". " + FILTERS[tier - 1] + text + ": oben auf Paste der Reinheit " + tier
                    + ", Reinheit " + (tier + 1) + " zu " + percent(settings.filter(tier).goods()) + ", Abfall "
                    + percent(settings.filter(tier).waste()) + ", sonst nichts. Ein Filter für bis zu drei Stück.");
        }
        lines.add(head + "Kokain" + text + ": Feenstaub oben macht aus jeder Paste Kokain gleicher Reinheit, " + chances(settings.kokain()) + ".");
        lines.add(head + "Schneiden" + text + ": Goldkarotte (Nachtsicht), Phantomhaut (Sprungkraft), Feuerball (Feuerresistenz), Honigflasche (Regeneration), Magmacreme (Stärke). Bis zu drei verschiedene Effekte, die Reinheit bleibt.");
        lines.add(head + "Strecken" + text + ": Zucker oben macht aus einem Kokain ab Reinheit 2 drei Stück mit einer Reinheit weniger. Gestrecktes lässt sich nicht noch einmal strecken.");
        lines.add(head + "Upgrades" + text + " (Blöcke bis zwei Felder um den Stand, jeder zählt einmal): Wasserkessel und Amethystblock je Ausbeute mal "
                + factor(settings.waterCauldronFactor()) + ", Blitzableiter Brauzeit mal " + factor(settings.lightningRodTimeFactor())
                + ", Räucherofen Brennstoff für " + settings.smokerFuelBrews() + " statt " + settings.fuelBrews() + " Brauvorgänge.");
        lines.add(head + "Konsum" + text + ": Rechtsklick mit Kokain. Wirkung nach Reinheit, danach ein Absturz. Pause von "
                + settings.cooldownSeconds() + " Sekunden. Im Kampf (" + settings.combatSeconds() + " Sekunden nach einem Treffer unter Spielern) nie über Stufe II.");
        lines.add(text + "Pflanzenabfall ist wertlos und blockiert seinen Slot, bis du ihn herausnimmst. Trichter sind erlaubt: oben Zutaten, seitlich Paste und Brennstoff, unten Ergebnisse.");
        return lines;
    }

    private static String chances(Yield yield) {
        return "Ware " + percent(yield.goods()) + ", Abfall " + percent(yield.waste()) + ", sonst nichts";
    }

    private static String percent(double chance) {
        double value = chance * 100;
        return (value == Math.rint(value) ? String.format(Locale.ROOT, "%.0f", value)
                : String.format(Locale.ROOT, "%.1f", value)) + " %";
    }

    private static String factor(double factor) {
        return String.format(Locale.ROOT, "%.2f", factor).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}

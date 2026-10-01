package de.hems.utils.bot.info;

import java.util.List;
import java.util.Map;

/**
 * Checks that both info texts fit into discord and that the parser does what the texts rely on.
 * <p>
 * A class with a main, run like {@code RewardCheck} (see there). Run it from an empty directory, otherwise
 * a {@code ./discord-info/} next to it is read instead of the texts in the jar. Exits non-zero when
 * something is wrong.
 */
public final class InfoTextCheck {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        parser();
        for (String file : List.of("spieler.md", "admins.md")) texts(file);
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void parser() {
        List<InfoText.Section> sections = InfoText.parse("# Titel\nIntro\n\n## Eins\na {x}\nb\n## Zwei\nc {y}\n",
                Map.of("x", "X"));
        check(sections.size() == 3, "three sections, got " + sections.size());
        check(sections.get(0).title().equals("Titel") && sections.get(0).body().equals("Intro"), "intro");
        check(sections.get(1).body().equals("a X\nb"), "placeholder filled: " + sections.get(1).body());
        check(sections.get(2).body().isEmpty(), "line with a missing placeholder left out");

        String paragraph = "x".repeat(1000) + "\n\n";
        List<InfoText.Section> split = InfoText.parse("## Lang\n" + paragraph.repeat(9), Map.of());
        check(split.size() == 3, "9000 characters split into three, got " + split.size());
        check(split.stream().allMatch(s -> s.body().length() <= InfoText.MAX_BODY), "every part fits");
        check(split.get(1).title().equals("Lang (Fortsetzung)"), "continuation is named");
    }

    private static void texts(String file) throws Exception {
        List<InfoText.Section> sections = InfoText.parse(InfoText.load(file),
                Map.of("adresse", "mc.example.org", "regeln", "http://mc.example.org:8080/regeln"));
        check(sections.size() > 1, file + " has sections");
        int longest = 0;
        for (InfoText.Section section : sections) {
            longest = Math.max(longest, section.body().length());
            check(!section.title().isBlank(), file + ": every section has a heading");
            check(!section.body().isBlank(), file + ": " + section.title() + " is not empty");
            check(section.body().length() <= InfoText.MAX_BODY, file + ": " + section.title() + " fits");
            check(!section.title().endsWith("(Fortsetzung)"), file + ": " + section.title() + " needs no split");
            check(!section.body().contains("{"), file + ": " + section.title() + " has no open placeholder");
            check(!section.body().contains("|---"), file + ": " + section.title() + " has no table, discord shows none");
        }
        System.out.println(file + ": " + sections.size() + " messages, longest " + longest + " characters");
    }

    private static void check(boolean condition, String what) {
        if (condition) {
            passed++;
        } else {
            failed++;
            System.out.println("FAILED: " + what);
        }
    }
}

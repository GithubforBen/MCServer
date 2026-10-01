package de.hems.utils.bot.info;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns one of the info texts into the sections the bot posts, one embed each.
 * <p>
 * The text is plain markdown: {@code # } names the first section, every {@code ## } starts a new one. Kept
 * free of JDA so the limits can be checked without discord (see {@code InfoTextCheck}).
 */
public final class InfoText {

    /** What discord takes as the description of an embed. */
    public static final int MAX_BODY = 4096;
    /** What discord takes as the title of an embed. */
    public static final int MAX_TITLE = 256;

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-z]+)}");

    /** One embed: a heading and what is under it. */
    public record Section(String title, String body) {
    }

    private InfoText() {
    }

    /**
     * The text of a page. A file of the same name in {@code ./discord-info/} next to the launcher wins over
     * the one in the jar, so the wording can change without a build.
     *
     * @param file the file name, e.g. {@code spieler.md}
     * @return the markdown
     */
    public static String load(String file) throws IOException {
        Path local = Path.of("discord-info", file);
        if (Files.isRegularFile(local)) return Files.readString(local, StandardCharsets.UTF_8);
        try (InputStream in = InfoText.class.getResourceAsStream("/discord-info/" + file)) {
            if (in == null) throw new IOException("discord-info/" + file + " is missing from the jar");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * @param markdown the text
     * @param values   what the placeholders stand for, e.g. {@code adresse}. A line with a placeholder that
     *                 has no value is left out - a line saying "Adresse: {adresse}" is worse than none
     * @return the sections in order, none longer than discord takes
     */
    public static List<Section> parse(String markdown, Map<String, String> values) {
        List<Section> sections = new ArrayList<>();
        String title = null;
        StringBuilder body = new StringBuilder();
        for (String line : markdown.replace("\r", "").split("\n", -1)) {
            String heading = heading(line);
            if (heading != null) {
                if (title != null || !body.toString().isBlank()) add(sections, title, body.toString());
                title = heading;
                body.setLength(0);
                continue;
            }
            String filled = fill(line, values);
            if (filled == null) continue;
            body.append(filled).append('\n');
        }
        if (title != null || !body.toString().isBlank()) add(sections, title, body.toString());
        return sections;
    }

    private static String heading(String line) {
        if (line.startsWith("## ")) return line.substring(3).strip();
        if (line.startsWith("# ")) return line.substring(2).strip();
        return null;
    }

    private static String fill(String line, Map<String, String> values) {
        Matcher matcher = PLACEHOLDER.matcher(line);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String value = values.get(matcher.group(1));
            if (value == null || value.isBlank()) return null;
            matcher.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /** Adds a section, split at paragraphs into more than one when it is too long for one embed. */
    private static void add(List<Section> sections, String title, String body) {
        String name = title == null ? "" : clip(title, MAX_TITLE);
        String rest = body.strip();
        boolean first = true;
        while (true) {
            String part = rest;
            if (part.length() > MAX_BODY) {
                int cut = rest.lastIndexOf("\n\n", MAX_BODY);
                if (cut <= 0) cut = rest.lastIndexOf('\n', MAX_BODY);
                if (cut <= 0) cut = MAX_BODY;
                part = rest.substring(0, cut);
            }
            sections.add(new Section(first ? name : clip(name + " (Fortsetzung)", MAX_TITLE), part.strip()));
            rest = rest.substring(part.length()).strip();
            first = false;
            if (rest.isEmpty()) return;
        }
    }

    private static String clip(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}

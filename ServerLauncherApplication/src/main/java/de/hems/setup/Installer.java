package de.hems.setup;

import de.hems.utils.Configuration;
import de.hems.utils.QrCode;
import de.hems.utils.bot.verification.DiscordOwner;
import de.hems.utils.server.MemoryLimits;
import de.hems.utils.types.RunningMode;
import de.hems.utils.webconsole.auth.AuthService;
import de.hems.utils.webconsole.auth.Passwords;
import de.hems.utils.webconsole.auth.Totp;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Asks for everything the launcher needs before its first start and writes it into {@code main-config.yml}
 * and {@code .env.local}. Started by {@code install.sh}, from the directory the launcher runs in.
 * <p>
 * Without it a fresh install is three failed starts: the launcher stops for the missing discord token, then
 * for the proxy secret it has just made up, and the website password is only ever shown once in a tmux
 * window. Every question shows what is set now, and Enter keeps it, so running it again changes only what
 * is typed in.
 */
public final class Installer {

    private static final String PLACEHOLDER_TOKEN = "<<add token here>>";
    private static final String ENV_FILE = ".env.local";
    private static final String RESERVE_ENV = "MCSERVER_MEMORY_RESERVE_MB";

    private static final Console CONSOLE = System.console();
    private static final BufferedReader STDIN =
            new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

    private final Configuration configuration = new Configuration();
    private final YamlConfiguration config = configuration.getConfig();
    /** What the summary at the end prints, in the order it was asked. */
    private final Map<String, String> summary = new LinkedHashMap<>();
    private final List<String> notes = new ArrayList<>();

    /** The website account to write at the end, or {@code null} to leave the accounts alone. */
    private String accountName;
    private String accountPassword;
    private boolean passwordGenerated;

    public static void main(String[] args) throws Exception {
        new Installer().run();
    }

    private void run() throws IOException {
        title("Einrichtung des Netzwerks");
        System.out.println("Enter übernimmt den Wert in [Klammern]. Abbrechen mit Strg+C - dann wird nichts gespeichert.");

        discord();
        address();
        secrets();
        players();
        website();
        memory();

        String totpSecret = null;
        AuthService auth = null;
        if (accountName != null) {
            auth = new AuthService(configuration);
            totpSecret = Totp.generateSecret();
            // saves the whole file, with everything asked above
            auth.saveAccount(accountName, accountPassword, totpSecret);
        }
        configuration.save();

        title("Gespeichert");
        summary.forEach((key, value) -> System.out.printf("  %-28s %s%n", key, value));
        if (auth != null) {
            System.out.println();
            System.out.println("  Admin-Website: " + accountName);
            if (passwordGenerated) System.out.println("  Passwort:      " + accountPassword);
            System.out.println("  2FA-Schlüssel: " + totpSecret);
            String uri = auth.toAuthenticatorUri(accountName, totpSecret);
            String qr = QrCode.terminal(uri, "  ");
            if (qr != null) {
                System.out.println("  Mit Google Authenticator scannen (\"+\" -> QR-Code scannen):");
                System.out.println();
                System.out.print(qr);
                System.out.println();
            }
            System.out.println("  Oder den Schlüssel von Hand eingeben bzw. diesen Link öffnen:");
            System.out.println("  " + uri);
            System.out.println("  Das wird nur jetzt angezeigt - bitte sofort sichern.");
        }
        for (String note : notes) {
            System.out.println();
            System.out.println("  Hinweis: " + note);
        }
        System.out.println();
    }

    // --- the questions ---------------------------------------------------------------------------------

    private void discord() {
        title("Discord-Bot");
        System.out.println("Der Token steht im Discord Developer Portal unter Bot -> Reset Token. Dort unter");
        System.out.println("\"Privileged Gateway Intents\" auch SERVER MEMBERS und MESSAGE CONTENT einschalten.");
        String current = config.getString("discord-token");
        boolean set = current != null && !current.isBlank() && !current.equals(PLACEHOLDER_TOKEN);
        while (true) {
            String token = secret("Bot-Token", set);
            if (token.isEmpty()) {
                if (set) break;
                System.out.println("  Ohne Token startet der Launcher nicht.");
                continue;
            }
            if (token.split("\\.").length != 3 && !yes("  Das sieht nicht wie ein Bot-Token aus. Trotzdem nehmen?", false)) {
                continue;
            }
            config.set("discord-token", token);
            config.setComments("discord-token", List.of("The discord token to use for the bot!"));
            break;
        }
        summary.put("discord-token", "gesetzt");

        System.out.println();
        System.out.println("Die Discord-ID des Besitzers darf /op, /deop und /unlink benutzen.");
        System.out.println("(Discord: Einstellungen -> Erweitert -> Entwicklermodus, dann Rechtsklick auf dich -> ID kopieren)");
        String owner = ask("Discord-ID des Besitzers", config.getString(DiscordOwner.CONFIG_KEY, DiscordOwner.DEFAULT_ID),
                value -> value.matches("\\d{17,20}") ? null : "Eine Discord-ID sind 17 bis 20 Ziffern.");
        config.set(DiscordOwner.CONFIG_KEY, owner);
        summary.put(DiscordOwner.CONFIG_KEY, owner);
    }

    private void address() {
        title("Erreichbarkeit");
        String lan = localAddress();
        System.out.println("Auf diese Adresse bindet sich der Proxy (Port 25565):");
        System.out.println("  1) LOCAL    nur dieser Rechner (localhost) - zum Testen");
        System.out.println("  2) LOCALIP  die Adresse dieses Rechners im Netzwerk, gerade " + lan);
        System.out.println("  3) PUBLIC   eine feste Adresse - die öffentliche IP eines Root-Servers oder eine eigene");
        String current = config.getString("running-mode", RunningMode.LOCAL.name());
        String choice = ask("Modus (1-3 oder Name)", current, value -> mode(value) == null ? "1, 2 oder 3." : null);
        RunningMode mode = mode(choice);
        config.set("running-mode", mode.name());
        summary.put("running-mode", mode.name());

        if (mode == RunningMode.LOCALIP && lan.startsWith("127.")) {
            notes.add("Dieser Rechner meldet " + lan + " als seine Adresse (Eintrag in /etc/hosts), damit wäre der "
                    + "Proxy nur lokal erreichbar. Besser PUBLIC mit der LAN-Adresse als server-ip.");
        }
        if (mode == RunningMode.PUBLIC) {
            System.out.println("Leer lassen, um die öffentliche IP beim Start automatisch zu ermitteln - das klappt nur,");
            System.out.println("wenn sie auch auf diesem Rechner liegt (Root-Server, nicht hinter einem Router).");
            String ip = ask("server-ip", config.getString("server-ip", ""), value -> null);
            if (ip.isEmpty() || ip.equals("-")) {
                config.set("server-ip", null);
                summary.put("server-ip", "automatisch");
            } else {
                config.set("server-ip", ip);
                summary.put("server-ip", ip);
            }
        }
        if (mode != RunningMode.LOCAL) {
            notes.add("Port 25565/tcp muss in der Firewall (und am Router) offen sein, damit Spieler joinen können.");

            // the name players type in - the address the proxy binds to is often only an ip, or not one that
            // is reachable from outside at all, so the discord texts would show the wrong thing
            System.out.println("Die Adresse, die Spieler in Minecraft eintragen (z.B. mc.ben-schnorr.com).");
            System.out.println("Sie steht in den Discord-Infotexten; \"-\" = keine eigene, dann wird die IP gezeigt.");
            String domain = ask("public-address", config.getString("public-address", ""), value -> null);
            if (domain.isEmpty() || domain.equals("-")) {
                config.set("public-address", null);
                summary.put("public-address", "keine (IP)");
            } else {
                config.set("public-address", domain);
                config.setComments("public-address",
                        List.of("The address players connect with, shown in the discord info texts."));
                summary.put("public-address", domain);
            }
        }
    }

    private void secrets() {
        title("Secrets");
        if (config.getString("serversecret", "").isBlank()) {
            config.set("serversecret", Passwords.randomToken());
            config.setComments("serversecret", List.of("The velocity secret key."));
            System.out.println("Velocity-Secret für Proxy und Server erzeugt.");
        } else if (yes("Velocity-Secret ist gesetzt. Neu erzeugen?", false)) {
            // the launcher writes it into velocity and every paper server on each start, so nothing else changes
            config.set("serversecret", Passwords.randomToken());
            System.out.println("Neu erzeugt - gilt ab dem nächsten Start des ganzen Netzwerks.");
        }
        summary.put("serversecret", "gesetzt");

        String command = config.getString("web.command-secret", "");
        if (command.isBlank() || yes("Secret für POST /command ist gesetzt. Neu erzeugen?", false)) {
            command = Passwords.randomToken();
            config.set("web.command-secret", command);
            config.setComments("web.command-secret", List.of("The secret scripts have to send to POST /command."));
            System.out.println("Secret für POST /command (executeCommand.py): " + command);
        }
        summary.put("web.command-secret", "gesetzt");
    }

    private void players() {
        title("Spieler und Server");
        System.out.println("Listen mit Komma trennen, \"-\" leert eine Liste.");
        List<String> ops = list("Operatoren (Minecraft-Namen)", "ops", List.of());
        List<String> whitelist = list("Whitelist (Minecraft-Namen)", "whitelist", List.of("for_Sale", "SA_MI"));
        config.setComments("whitelist", List.of("Players that are put onto the whitelist of every server."));
        List<String> autostart = list("Server, die mit dem Netzwerk starten", "autostart", List.of("LOBBY", "SURVIVAL"));
        autostart.replaceAll(name -> name.toUpperCase(Locale.ROOT));
        config.set("autostart", autostart);
        config.setComments("autostart", List.of("Servers that are started together with the network."));
        summary.put("ops", String.join(", ", ops));
        summary.put("whitelist", String.join(", ", whitelist));
        summary.put("autostart", String.join(", ", autostart));
    }

    private void website() {
        title("Admin-Website");
        boolean enabled = yes("Admin-Website einschalten?", config.getBoolean("web.enabled", true));
        config.set("web.enabled", enabled);
        summary.put("web.enabled", enabled ? "an" : "aus");
        if (!enabled) return;

        String port = ask("Port", String.valueOf(config.getInt("web.port", 8080)), Installer::port);
        config.set("web.port", Integer.parseInt(port));
        boolean https = yes("Läuft sie hinter HTTPS (Reverse Proxy)? Dann bekommt das Cookie das Secure-Flag.",
                config.getBoolean("web.secure-cookie", false));
        config.set("web.secure-cookie", https);
        summary.put("web.port", port);

        var accounts = config.getConfigurationSection("web.admins");
        if (accounts != null && !accounts.getKeys(false).isEmpty()) {
            System.out.println("Vorhandene Accounts: " + String.join(", ", accounts.getKeys(false)));
            if (!yes("Einen Account anlegen oder sein Passwort und 2FA neu setzen?", false)) return;
        }
        accountName = ask("Benutzername", "admin",
                value -> value.matches("[A-Za-z0-9_.-]{1,32}") ? null : "Nur Buchstaben, Ziffern, _ . und -.")
                .toLowerCase(Locale.ROOT);
        while (true) {
            String password = secret("Passwort (leer = zufällig erzeugen)", false);
            if (password.isEmpty()) {
                accountPassword = Passwords.generate(20);
                passwordGenerated = true;
                break;
            }
            if (password.length() < 12) {
                System.out.println("  Mindestens 12 Zeichen.");
                continue;
            }
            if (!password.equals(secret("Passwort wiederholen", false))) {
                System.out.println("  Die beiden stimmen nicht überein.");
                continue;
            }
            accountPassword = password;
            break;
        }
        summary.put("web.admins", accountName);
    }

    private void memory() throws IOException {
        title("Arbeitsspeicher");
        Path file = Path.of(ENV_FILE);
        List<String> env = Files.exists(file) ? new ArrayList<>(Files.readAllLines(file)) : new ArrayList<>();
        long total = totalMemoryMb();
        if (total > 0) System.out.println("Dieser Rechner hat " + total + " MB. Survival will 4096 MB, eine Bedwars-Runde 2048.");

        String reserve = ask("Reserve fürs System in MB", env(env, RESERVE_ENV, "2048"),
                value -> value.matches("\\d+") ? null : "Eine Zahl in MB.");
        String max = ask("Höchstens so viel MB pro Server (\"-\" = kein Deckel)", env(env, MemoryLimits.MAX_ENV, "-"),
                value -> value.equals("-") || value.matches("\\d+") ? null : "Eine Zahl in MB, oder -.");
        setEnv(env, RESERVE_ENV, reserve);
        setEnv(env, MemoryLimits.MAX_ENV, max.equals("-") ? null : max);
        Files.write(file, env);
        summary.put(RESERVE_ENV, reserve + " MB");
        summary.put(MemoryLimits.MAX_ENV, max.equals("-") ? "kein Deckel" : max + " MB");
    }

    // --- helpers ---------------------------------------------------------------------------------------

    private List<String> list(String question, String key, List<String> fallback) {
        List<String> current = config.contains(key) ? config.getStringList(key) : fallback;
        String answer = ask(question, String.join(", ", current), value -> null);
        List<String> values = answer.equals("-") ? new ArrayList<>() : new ArrayList<>(Arrays.stream(answer.split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).distinct().toList());
        config.set(key, values);
        return values;
    }

    private static RunningMode mode(String value) {
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "1", "LOCAL" -> RunningMode.LOCAL;
            case "2", "LOCALIP" -> RunningMode.LOCALIP;
            case "3", "PUBLIC" -> RunningMode.PUBLIC;
            default -> null;
        };
    }

    private static String port(String value) {
        try {
            int port = Integer.parseInt(value);
            if (port == 25565) return "25565 ist der Port des Proxys.";
            return port > 0 && port < 65536 ? null : "Ein Port zwischen 1 und 65535.";
        } catch (NumberFormatException e) {
            return "Ein Port zwischen 1 und 65535.";
        }
    }

    private static String localAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (IOException e) {
            return "unbekannt";
        }
    }

    private static long totalMemoryMb() {
        try {
            for (String line : Files.readAllLines(Path.of("/proc/meminfo"))) {
                if (line.startsWith("MemTotal:")) return Long.parseLong(line.replaceAll("\\D", "")) / 1024L;
            }
        } catch (IOException | NumberFormatException ignored) {
        }
        return -1L;
    }

    /** @return the value of {@code KEY=value} in the machine settings, or the fallback */
    private static String env(List<String> lines, String key, String fallback) {
        for (String line : lines) {
            if (line.startsWith(key + "=")) return line.substring(key.length() + 1).trim();
        }
        return fallback;
    }

    /** Sets, or with {@code null} removes, one line of the machine settings; every other line stays as it is. */
    private static void setEnv(List<String> lines, String key, String value) {
        lines.removeIf(line -> line.startsWith(key + "="));
        if (value != null) lines.add(key + "=" + value);
    }

    private static void title(String text) {
        System.out.println();
        System.out.println("== " + text + " ==");
    }

    private interface Check {
        /** @return why the value is not usable, or {@code null} if it is */
        String problem(String value);
    }

    private static String ask(String question, String current, Check check) {
        while (true) {
            String prompt = question + (current == null || current.isEmpty() ? "" : " [" + current + "]") + ": ";
            String answer = read(prompt, false).trim();
            if (answer.isEmpty() && current != null) answer = current;
            String problem = check.problem(answer);
            if (problem == null) return answer;
            System.out.println("  " + problem);
        }
    }

    /** Asks without echo. An empty answer means "keep what is set" when {@code set} is true. */
    private static String secret(String question, boolean set) {
        return read(question + (set ? " [gesetzt, Enter behält ihn]" : "") + ": ", true).trim();
    }

    private static boolean yes(String question, boolean fallback) {
        while (true) {
            String answer = read(question + (fallback ? " [J/n]: " : " [j/N]: "), false).trim().toLowerCase(Locale.ROOT);
            if (answer.isEmpty()) return fallback;
            if (answer.startsWith("j") || answer.startsWith("y")) return true;
            if (answer.startsWith("n")) return false;
        }
    }

    private static String read(String prompt, boolean hidden) {
        String line;
        if (CONSOLE != null) {
            if (hidden) {
                char[] chars = CONSOLE.readPassword("%s", prompt);
                line = chars == null ? null : new String(chars);
            } else {
                line = CONSOLE.readLine("%s", prompt);
            }
        } else {
            System.out.print(prompt);
            System.out.flush();
            try {
                line = STDIN.readLine();
            } catch (IOException e) {
                line = null;
            }
        }
        if (line == null) {
            System.out.println();
            System.out.println("Eingabe beendet - nichts gespeichert.");
            System.exit(1);
        }
        return line;
    }
}

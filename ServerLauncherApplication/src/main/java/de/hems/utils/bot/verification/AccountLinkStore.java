package de.hems.utils.bot.verification;

import de.hems.types.discord.AccountLink;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which discord account belongs to which minecraft account.
 * <p>
 * Linking is two steps on purpose. The discord side says "I am player X" and gets a code back; the code is
 * then typed in on the server, where only somebody actually logged in as X can type it. Without that step
 * anybody could claim to be anybody, and a list of links that might be lies is worse than no list - the
 * whole point of it is to know who you are writing to.
 * <p>
 * The codes live in memory only. They are worth nothing after ten minutes, and a launcher restart in the
 * middle of somebody linking costs them one command.
 */
public class AccountLinkStore {

    /** How long a code is good for. */
    public static final long CODE_VALID_MS = 10L * 60L * 1000L;
    /** How long the code is. */
    private static final int CODE_LENGTH = 6;
    /**
     * The letters a code is built from. No {@code I}, {@code O}, {@code 0} or {@code 1}: the code is read
     * off one screen and typed into another, and those four are where that goes wrong.
     */
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final File file;
    private final YamlConfiguration config;
    /** By minecraft uuid, because that is what a server has when it asks. */
    private final Map<UUID, AccountLink> links = new ConcurrentHashMap<>();
    /** Codes that have been handed out and not used yet, by the code itself. */
    private final Map<String, Pending> pending = new ConcurrentHashMap<>();

    public AccountLinkStore() {
        this(new File("./links.yml"));
    }

    public AccountLinkStore(File file) {
        this.file = file;
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        load();
    }

    private void load() {
        ConfigurationSection section = config.getConfigurationSection("links");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) continue;
            try {
                UUID minecraftId = UUID.fromString(key);
                AccountLink link = new AccountLink();
                link.setMinecraftId(minecraftId);
                link.setMinecraftName(entry.getString("minecraft-name"));
                link.setDiscordId(entry.getString("discord-id"));
                link.setDiscordName(entry.getString("discord-name"));
                link.setLinkedAt(entry.getLong("linked-at", System.currentTimeMillis()));
                if (link.getDiscordId() != null) links.put(minecraftId, link);
            } catch (IllegalArgumentException ignored) {
                // one unreadable entry costs one link, not the list
            }
        }
        System.out.println("Loaded " + links.size() + " account links from " + file.getName());
    }

    private void write(AccountLink link) {
        String path = "links." + link.getMinecraftId();
        config.set(path + ".minecraft-name", link.getMinecraftName());
        config.set(path + ".discord-id", link.getDiscordId());
        config.set(path + ".discord-name", link.getDiscordName());
        config.set(path + ".linked-at", link.getLinkedAt());
    }

    public synchronized void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            System.out.println("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }

    /* ------------------------------------------------------------------ handing out a code */

    /**
     * Starts a link and hands out the code that finishes it.
     * <p>
     * A second attempt replaces the first: somebody who typed the wrong name and tries again should not be
     * left guessing which of two codes is the live one.
     *
     * @param discordId     the discord account asking
     * @param discordName   how it is called, for the list
     * @param minecraftId   the minecraft account it claims to be
     * @param minecraftName that account's name
     * @return the code to type in on the server
     */
    public synchronized String startLink(String discordId, String discordName, UUID minecraftId,
                                         String minecraftName) {
        pending.values().removeIf(entry -> entry.discordId.equals(discordId));
        expire();
        String code = generateCode();
        pending.put(code, new Pending(discordId, discordName, minecraftId, minecraftName,
                System.currentTimeMillis() + CODE_VALID_MS));
        return code;
    }

    /**
     * @return a code that is not in use
     */
    private String generateCode() {
        for (int attempt = 0; attempt < 100; attempt++) {
            StringBuilder code = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            }
            String candidate = code.toString();
            if (!pending.containsKey(candidate)) return candidate;
        }
        throw new IllegalStateException("No free code left");
    }

    private void expire() {
        long now = System.currentTimeMillis();
        pending.values().removeIf(entry -> entry.expiresAt <= now);
    }

    /* ------------------------------------------------------------------ using it */

    /**
     * Finishes a link with the code that was handed out for it.
     *
     * @param player the account typing the code in
     * @param name   its current name, which is written into the link
     * @param code   what they typed
     * @return what happened
     */
    public synchronized Result confirm(UUID player, String name, String code) {
        expire();
        if (player == null || code == null || code.isBlank()) {
            return new Result(false, "Dazu fehlt der Code.", null);
        }
        Pending entry = pending.get(code.trim().toUpperCase(Locale.ROOT));
        if (entry == null) {
            return new Result(false, "Diesen Code gibt es nicht (mehr). "
                    + "Fordere auf Discord mit /verify einen neuen an.", null);
        }
        if (!player.equals(entry.minecraftId)) {
            // the code belongs to another account: this is the check the whole two step dance exists for
            return new Result(false, "Dieser Code gehört zu " + entry.minecraftName + ", nicht zu dir.", null);
        }
        pending.remove(code.trim().toUpperCase(Locale.ROOT));
        AccountLink link = new AccountLink(entry.discordId, entry.discordName, player,
                name == null ? entry.minecraftName : name);
        links.put(player, link);
        write(link);
        save();
        return new Result(true, "Dein Account ist jetzt mit " + link.describeDiscord() + " verknüpft.", link);
    }

    /**
     * @param player a minecraft account
     * @return the discord account behind it, or {@code null}
     */
    public AccountLink get(UUID player) {
        return player == null ? null : links.get(player);
    }

    /**
     * @param discordId a discord account
     * @return the minecraft account behind it, or {@code null}
     */
    public AccountLink byDiscord(String discordId) {
        if (discordId == null) return null;
        for (AccountLink link : links.values()) {
            if (discordId.equals(link.getDiscordId())) return link;
        }
        return null;
    }

    /**
     * @param name a minecraft name, in any capitalisation
     * @return the link, or {@code null}
     */
    public AccountLink byName(String name) {
        if (name == null) return null;
        String wanted = name.toLowerCase(Locale.ROOT);
        for (AccountLink link : links.values()) {
            if (link.getMinecraftName() != null
                    && link.getMinecraftName().toLowerCase(Locale.ROOT).equals(wanted)) {
                return link;
            }
        }
        return null;
    }

    /**
     * @return every link, as a fresh list the caller may keep
     */
    public ArrayList<AccountLink> all() {
        return new ArrayList<>(links.values());
    }

    /**
     * Removes a link.
     *
     * @param player the minecraft account
     * @return whether there was one
     */
    public synchronized boolean unlink(UUID player) {
        if (player == null || links.remove(player) == null) return false;
        config.set("links." + player, null);
        save();
        return true;
    }

    /**
     * What became of a code.
     *
     * @param successful whether the accounts are now linked
     * @param message    what to tell whoever typed it
     * @param link       the link, when one was made
     */
    public record Result(boolean successful, String message, AccountLink link) {
    }

    /**
     * A code that has been handed out and not used yet.
     */
    private record Pending(String discordId, String discordName, UUID minecraftId, String minecraftName,
                           long expiresAt) {
    }
}

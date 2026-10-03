package de.hems.utils.webconsole.auth;

import de.hems.utils.YamlFiles;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Where the logins of the website wait out a restart of the launcher.
 * <p>
 * Only the hash of each token is written, never the token: a copy of this file shows who is logged in and
 * until when, but it is not a way in. Somebody holding the file could still change it - which is why a
 * session from disk is also checked against the account's current password on every use.
 */
public class SessionStore {

    private final File file;

    public SessionStore(File file) {
        this.file = file;
    }

    /**
     * @return every session in the file that has not run out yet
     */
    public List<Session> load() {
        List<Session> sessions = new ArrayList<>();
        if (!file.isFile()) return sessions;
        YamlConfiguration config;
        try {
            config = YamlFiles.load(file);
        } catch (IllegalStateException e) {
            // a broken file costs everybody their login, nothing more - YamlFiles kept a copy of it
            System.out.println("Could not read " + file.getName() + ", everybody has to log in again: "
                    + e.getMessage());
            return sessions;
        }
        ConfigurationSection section = config.getConfigurationSection("sessions");
        if (section == null) return sessions;
        for (String id : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(id);
            if (entry == null) continue;
            String username = entry.getString("user");
            String csrf = entry.getString("csrf");
            String stamp = entry.getString("stamp");
            if (username == null || csrf == null || stamp == null) continue;
            Session session = new Session(id, csrf, username, entry.getLong("created"), entry.getLong("expires"),
                    entry.getBoolean("remember", false), stamp);
            if (!session.isExpired()) sessions.add(session);
        }
        return sessions;
    }

    /**
     * Writes the sessions there are now, replacing what the file held.
     *
     * @param sessions the sessions
     */
    public synchronized void save(Collection<Session> sessions) {
        YamlConfiguration config = new YamlConfiguration();
        for (Session session : sessions) {
            if (session.isExpired()) continue;
            String path = "sessions." + session.getId();
            config.set(path + ".user", session.getUsername());
            config.set(path + ".csrf", session.getCsrfToken());
            config.set(path + ".created", session.getCreatedAt());
            config.set(path + ".expires", session.getExpiresAt());
            config.set(path + ".remember", session.isRemember());
            config.set(path + ".stamp", session.getAccountStamp());
        }
        YamlFiles.saveOrLog(config, file);
    }
}

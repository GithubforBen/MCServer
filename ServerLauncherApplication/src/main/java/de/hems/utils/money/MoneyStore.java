package de.hems.utils.money;

import de.hems.types.money.BalanceResult;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where the money of the network lives.
 * <p>
 * Bits used to be a config file next to the survival server. That tied them to that one server: the lobby
 * could not read them, nothing but survival could spend them, and a wiped survival directory took the whole
 * economy with it. They now belong to the launcher, like the teams and the events.
 * <p>
 * Every change is a delta and is applied here, under a lock, which is the only reason two servers may touch
 * the same account at the same time. Nobody else writes.
 */
public class MoneyStore {

    /** The old file next to the survival server, imported once so nobody loses their money. */
    public static final String LEGACY_FILE = "./servers/SURVIVAL/configs/money-config.yml";

    private final File file;
    private final YamlConfiguration config;
    /** Account name to balance. A player is their uuid as text, a team is its name. */
    private final Map<String, Integer> balances = new ConcurrentHashMap<>();

    public MoneyStore() {
        this(new File("./money.yml"), new File(LEGACY_FILE));
    }

    public MoneyStore(File file, File legacy) {
        this.file = file;
        boolean fresh = !file.exists();
        if (fresh) {
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
        // only ever on the first start: importing again later would resurrect balances that were spent
        if (balances.isEmpty() && legacy != null && legacy.isFile()) importLegacy(legacy);
    }

    private void load() {
        ConfigurationSection section = config.getConfigurationSection("balances");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            balances.put(key, section.getInt(key, 0));
        }
        System.out.println("Loaded " + balances.size() + " balances from " + file.getName());
    }

    /**
     * Takes over the money the survival server used to keep on its own.
     * <p>
     * The old file wrote every account as {@code <holder>.money}, holders being player uuids and team
     * names side by side. The new file keeps exactly those keys, so a team that is renamed later behaves
     * the same as it did before.
     *
     * @param legacy the survival money file
     */
    private void importLegacy(File legacy) {
        YamlConfiguration old = YamlConfiguration.loadConfiguration(legacy);
        int imported = 0;
        for (String key : old.getKeys(false)) {
            if (!old.contains(key + ".money")) continue;
            balances.put(key, old.getInt(key + ".money"));
            imported++;
        }
        if (imported == 0) return;
        writeAll();
        save();
        System.out.println("Imported " + imported + " balances from " + legacy.getPath()
                + " - the money of the network now lives in " + file.getName());
    }

    private void writeAll() {
        config.set("balances", null);
        for (Map.Entry<String, Integer> entry : balances.entrySet()) {
            config.set("balances." + entry.getKey(), entry.getValue());
        }
    }

    public synchronized void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            System.out.println("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }

    /**
     * @param holder an account
     * @return what is on it
     */
    public int get(String holder) {
        if (holder == null) return 0;
        Integer amount = balances.get(holder);
        return amount == null ? 0 : amount;
    }

    /**
     * @return every account, as a fresh map the caller may keep
     */
    public HashMap<String, Integer> all() {
        return new HashMap<>(balances);
    }

    /**
     * Moves money on one account.
     *
     * @param holder       the account
     * @param delta        how much to add, negative to take away
     * @param requireCover whether taking away must fail rather than go below zero
     * @return what happened, including the balance afterwards
     */
    public synchronized BalanceResult change(String holder, int delta, boolean requireCover) {
        if (holder == null || holder.isBlank()) {
            return BalanceResult.failed(holder, 0, "Kein Konto angegeben.");
        }
        int current = get(holder);
        if (delta == 0) return BalanceResult.ok(holder, current);
        long next = (long) current + delta;
        if (next < 0) {
            if (requireCover) {
                return BalanceResult.failed(holder, current, "Nicht genug Bits: " + current
                        + " vorhanden, " + (-delta) + " gebraucht.");
            }
            // without cover required the account may not go negative either, it just stops at zero
            next = 0;
        }
        int updated = (int) Math.min(Integer.MAX_VALUE, next);
        balances.put(holder, updated);
        config.set("balances." + holder, updated);
        save();
        return BalanceResult.ok(holder, updated);
    }
}

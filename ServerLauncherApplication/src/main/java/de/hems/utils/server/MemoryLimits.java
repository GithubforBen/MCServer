package de.hems.utils.server;

import de.hems.Main;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * How much memory a server on this machine may have.
 * <p>
 * The templates say what a server would like - four gigabytes for survival, two for a bedwars round - and
 * those numbers are written for a machine that has them to give. A development box does not, and handing
 * out more heap than there is physical memory is how a whole network starts swapping and everything lags
 * at once. So the machine gets the last word, through an environment variable that belongs to the machine
 * rather than to the repository.
 * <p>
 * <pre>{@code
 * MCSERVER_MAX_MEMORY_MB=1024   # no server gets more than a gigabyte
 * MCSERVER_MEMORY_PERCENT=50    # and every request is halved first
 * }</pre>
 */
public final class MemoryLimits {

    /** The cap per server, in megabytes. */
    public static final String MAX_ENV = "MCSERVER_MAX_MEMORY_MB";
    /** What share of the requested memory a server actually gets, in percent. */
    public static final String PERCENT_ENV = "MCSERVER_MEMORY_PERCENT";

    /** No server is started with less than this, because below it paper does not come up at all. */
    public static final int FLOOR_MB = 512;

    private MemoryLimits() {
    }

    /**
     * Applies this machine's limits to what a template asked for.
     *
     * @param name      the server, for the message
     * @param requested the memory the template or the caller wants
     * @return the memory the server is actually started with
     */
    public static int apply(Object name, int requested) {
        int percent = read(PERCENT_ENV, "memory-percent", 100);
        int max = read(MAX_ENV, "max-memory-mb", 0);

        int granted = requested;
        if (percent > 0 && percent < 100) granted = granted * percent / 100;
        if (max > 0) granted = Math.min(granted, max);
        granted = Math.max(FLOOR_MB, granted);

        if (granted != requested) {
            System.out.println("Memory for " + name + ": " + requested + " MB requested, " + granted
                    + " MB granted (" + MAX_ENV + "/" + PERCENT_ENV + ")");
        }
        return granted;
    }

    /**
     * Reads a limit. The environment wins over the config file, because it is the machine's answer and the
     * config travels with the repository.
     *
     * @param variable     the environment variable
     * @param configKey    the key in the launcher config
     * @param fallback     what to use when neither says anything
     * @return the limit, or the fallback
     */
    public static int read(String variable, String configKey, int fallback) {
        String environment = System.getenv(variable);
        if (environment != null && !environment.isBlank()) {
            try {
                return Integer.parseInt(environment.trim());
            } catch (NumberFormatException e) {
                System.out.println(variable + "='" + environment + "' is not a number and is ignored.");
            }
        }
        Main main = Main.getInstance();
        if (main == null || main.getConfiguration() == null) return fallback;
        YamlConfiguration config = main.getConfiguration().getConfig();
        return config.getInt("memory." + configKey, fallback);
    }
}

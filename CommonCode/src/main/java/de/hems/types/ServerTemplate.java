package de.hems.types;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A blueprint for a new server: which software it runs, how much memory it gets by default and which
 * plugins belong to it.
 * <p>
 * Templates are what makes creating servers automatable - the UI and {@link de.hems.api.ServerApi} both
 * describe a server as "this template plus these extra plugins", so an event server can be created with a
 * single call without knowing anything about jars or ports.
 */
public enum ServerTemplate implements Serializable {

    /** The proxy everybody connects through. */
    PROXY(FileType.SERVER.VELOCITY, 1024, false,
            FileType.PLUGIN.VELOCITY, FileType.PLUGIN.SIMPLE_VOICECHAT_VELOCITY),

    /** The hub players land on. */
    LOBBY(FileType.SERVER.PAPER, 2048, true,
            FileType.PLUGIN.LOBBY),

    /** The main survival world. */
    SURVIVAL(FileType.SERVER.PAPER, 4096, true,
            FileType.PLUGIN.SURVIVAL, FileType.PLUGIN.BACKPACK, FileType.PLUGIN.SIMPLE_VOICECHAT_PAPER),

    /** A bedwars round. */
    BEDWARS(FileType.SERVER.PAPER, 2048, true,
            FileType.PLUGIN.BEDWARS),

    /** A plain paper server without a game mode - the base for custom events. */
    EVENT(FileType.SERVER.PAPER, 2048, true);

    /** Plugins that are installed on every paper server, no matter which template is used. */
    public static final List<FileType.PLUGIN> BASE_PAPER_PLUGINS = List.of(
            FileType.PLUGIN.CORE_PROTECT,
            FileType.PLUGIN.WORLDEDIT,
            FileType.PLUGIN.CHUNKY,
            FileType.PLUGIN.WORLD_GUARD);

    private final FileType.SERVER software;
    private final int defaultMemoryMB;
    private final boolean basePlugins;
    private final List<FileType.PLUGIN> templatePlugins;

    ServerTemplate(FileType.SERVER software, int defaultMemoryMB, boolean basePlugins, FileType.PLUGIN... templatePlugins) {
        this.software = software;
        this.defaultMemoryMB = defaultMemoryMB;
        this.basePlugins = basePlugins;
        this.templatePlugins = List.of(templatePlugins);
    }

    public FileType.SERVER getSoftware() {
        return software;
    }

    public int getDefaultMemoryMB() {
        return defaultMemoryMB;
    }

    /**
     * @return the plugins this template always installs and that can not be deselected
     */
    public Set<FileType.PLUGIN> getRequiredPlugins() {
        Set<FileType.PLUGIN> plugins = new LinkedHashSet<>(templatePlugins);
        if (basePlugins) plugins.addAll(BASE_PAPER_PLUGINS);
        return plugins;
    }

    /**
     * @return the plugins that are preselected when a server of this template is created
     */
    public Set<FileType.PLUGIN> getDefaultPlugins() {
        return getRequiredPlugins();
    }

    /**
     * Merges the required plugins of this template with a freely chosen selection.
     *
     * @param selected the plugins that were picked on top of the template, may be {@code null}
     * @return every plugin that has to be installed
     */
    public Set<FileType.PLUGIN> resolvePlugins(Iterable<FileType.PLUGIN> selected) {
        Set<FileType.PLUGIN> plugins = getRequiredPlugins();
        if (selected != null) {
            for (FileType.PLUGIN plugin : selected) {
                if (plugin != null && plugin.supports(software)) plugins.add(plugin);
            }
        }
        return plugins;
    }

    /**
     * Merges the required plugins of this template with a freely chosen selection.
     *
     * @param selected the plugins that were picked on top of the template, may be {@code null}
     * @return every plugin that has to be installed
     */
    public Set<FileType.PLUGIN> resolvePlugins(FileType.PLUGIN[] selected) {
        return resolvePlugins(selected == null ? null : Arrays.asList(selected));
    }

    public String getDisplayName() {
        return switch (this) {
            case PROXY -> "Proxy";
            case LOBBY -> "Lobby";
            case SURVIVAL -> "Survival";
            case BEDWARS -> "Bedwars";
            case EVENT -> "Event (leer)";
        };
    }

    public String getDescription() {
        return switch (this) {
            case PROXY -> "Der Velocity Proxy des Netzwerks";
            case LOBBY -> "Hub mit Parkour und Server Manager";
            case SURVIVAL -> "Survival mit Teams, Geld und Shops";
            case BEDWARS -> "Fertige Bedwars Runde";
            case EVENT -> "Leerer Paper Server fuer eigene Events";
        };
    }

    /**
     * Guesses the template a server name belongs to, so servers that were started before templates existed
     * (or through the old API) still get their plugins.
     *
     * @param serverName the name of the server
     * @return the matching template, {@link #EVENT} if nothing matches
     */
    public static ServerTemplate forServerName(String serverName) {
        if (serverName == null) return EVENT;
        String normalized = serverName.toUpperCase(Locale.ROOT);
        for (ServerTemplate template : values()) {
            if (normalized.equals(template.name())) return template;
        }
        for (ServerTemplate template : values()) {
            if (template != EVENT && normalized.startsWith(template.name())) return template;
        }
        return EVENT;
    }

    /**
     * @param name the name of a template, case insensitive
     * @return the template, or {@code null} if there is none with that name
     */
    public static ServerTemplate find(String name) {
        if (name == null) return null;
        for (ServerTemplate template : values()) {
            if (template.name().equalsIgnoreCase(name.trim())) return template;
        }
        return null;
    }
}

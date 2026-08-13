package de.hems.paper.servermanager;

import de.hems.types.FileType;
import de.hems.types.ServerTemplate;
import org.bukkit.entity.Player;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What a player configured so far while creating a server: name, template, memory and the plugins they
 * picked. Kept per player, so several admins can build different servers at the same time.
 */
public class ServerDraft {

    private static final Map<UUID, ServerDraft> drafts = new ConcurrentHashMap<>();

    /** Smallest amount of memory a server can be created with. */
    public static final int MIN_MEMORY_MB = 512;
    /** Largest amount of memory the UI hands out. */
    public static final int MAX_MEMORY_MB = 16384;
    /** How much one click adds or removes. */
    public static final int MEMORY_STEP_MB = 512;

    private String name;
    private ServerTemplate template;
    private int memoryMB;
    private final Set<FileType.PLUGIN> selectedPlugins = new LinkedHashSet<>();

    private ServerDraft(String name, ServerTemplate template) {
        this.name = name;
        this.template = template;
        this.memoryMB = template.getDefaultMemoryMB();
        this.selectedPlugins.addAll(template.getDefaultPlugins());
    }

    /**
     * Starts a new draft for a player, replacing anything they configured before.
     *
     * @param player   the player creating a server
     * @param name     the suggested name
     * @param template the blueprint to start from
     * @return the fresh draft
     */
    public static ServerDraft start(Player player, String name, ServerTemplate template) {
        ServerDraft draft = new ServerDraft(name, template);
        drafts.put(player.getUniqueId(), draft);
        return draft;
    }

    /**
     * @param player the player creating a server
     * @return the draft they are working on, or {@code null} if they have none
     */
    public static ServerDraft of(Player player) {
        return drafts.get(player.getUniqueId());
    }

    public static void clear(Player player) {
        drafts.remove(player.getUniqueId());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ServerTemplate getTemplate() {
        return template;
    }

    /**
     * Switches the blueprint and resets the plugin selection to the defaults of the new one.
     *
     * @param template the new blueprint
     */
    public void setTemplate(ServerTemplate template) {
        this.template = template;
        this.memoryMB = template.getDefaultMemoryMB();
        selectedPlugins.clear();
        selectedPlugins.addAll(template.getDefaultPlugins());
    }

    public int getMemoryMB() {
        return memoryMB;
    }

    /**
     * Changes the memory, staying inside the allowed range.
     *
     * @param deltaMB how much memory to add, negative to remove
     */
    public void addMemory(int deltaMB) {
        memoryMB = Math.max(MIN_MEMORY_MB, Math.min(MAX_MEMORY_MB, memoryMB + deltaMB));
    }

    /**
     * @return every plugin that will be installed
     */
    public Set<FileType.PLUGIN> getSelectedPlugins() {
        return template.resolvePlugins(selectedPlugins);
    }

    /**
     * @return the plugins that were picked on top of the template
     */
    public Set<FileType.PLUGIN> getExtraPlugins() {
        Set<FileType.PLUGIN> extra = new LinkedHashSet<>(selectedPlugins);
        extra.removeAll(template.getRequiredPlugins());
        return extra;
    }

    /**
     * @param plugin the plugin to check
     * @return whether it will be installed
     */
    public boolean isSelected(FileType.PLUGIN plugin) {
        return selectedPlugins.contains(plugin) || template.getRequiredPlugins().contains(plugin);
    }

    /**
     * @param plugin the plugin to check
     * @return whether the template needs it, which means it can not be deselected
     */
    public boolean isRequired(FileType.PLUGIN plugin) {
        return template.getRequiredPlugins().contains(plugin);
    }

    /**
     * Turns a plugin on or off. Plugins the template needs stay on.
     *
     * @param plugin the plugin to toggle
     * @return whether the plugin is selected afterwards
     */
    public boolean toggle(FileType.PLUGIN plugin) {
        if (isRequired(plugin)) return true;
        if (!plugin.supports(template.getSoftware())) return false;
        if (selectedPlugins.contains(plugin)) {
            selectedPlugins.remove(plugin);
            return false;
        }
        selectedPlugins.add(plugin);
        return true;
    }
}

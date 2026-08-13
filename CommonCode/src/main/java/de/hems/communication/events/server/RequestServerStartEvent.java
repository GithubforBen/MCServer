package de.hems.communication.events.server;

import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventFoundationData;
import de.hems.types.FileType;
import de.hems.types.ServerTemplate;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

/**
 * Asks the host to start a server. The server does not have to exist yet - if the name is unknown, the host
 * creates it, assigns a free port and announces it to the network.
 */
public class RequestServerStartEvent extends EventFoundationData implements Event, Serializable {
    private static final long serialVersionUID = 5L;
    private ListenerAdapter.ServerName serverName;
    private FileType.SERVER type;
    private Integer memory;
    private FileType.PLUGIN[] plugins;
    private ServerTemplate template;

    public RequestServerStartEvent(ListenerAdapter.ServerName receiver, ListenerAdapter.ServerName serverName, FileType.SERVER type, Integer memory, FileType.PLUGIN[] plugins) {
        this(receiver, serverName, type, memory, plugins, ServerTemplate.forServerName(serverName.toString()));
    }

    public RequestServerStartEvent(ListenerAdapter.ServerName receiver, ListenerAdapter.ServerName serverName,
                                   FileType.SERVER type, Integer memory, FileType.PLUGIN[] plugins,
                                   ServerTemplate template) {
        super(receiver);
        this.serverName = serverName;
        this.type = type;
        this.memory = memory;
        this.plugins = plugins;
        this.template = template;
    }

    /**
     * Creates a start request from a template, which is all that is needed to spin up an event server.
     *
     * @param receiver      the node that starts the server, normally {@link ListenerAdapter.ServerName#HOST}
     * @param serverName    the name of the server to start
     * @param template      the blueprint of the server
     * @param memory        the memory in MB, or {@code null} for the default of the template
     * @param extraPlugins  plugins that are installed on top of the template, may be {@code null}
     */
    public RequestServerStartEvent(ListenerAdapter.ServerName receiver, ListenerAdapter.ServerName serverName,
                                   ServerTemplate template, Integer memory, Collection<FileType.PLUGIN> extraPlugins) {
        super(receiver);
        this.serverName = serverName;
        this.template = template;
        this.type = template.getSoftware();
        this.memory = memory == null ? template.getDefaultMemoryMB() : memory;
        Set<FileType.PLUGIN> resolved = template.resolvePlugins(extraPlugins);
        this.plugins = resolved.toArray(new FileType.PLUGIN[0]);
    }

    public RequestServerStartEvent() {
    }

    public ListenerAdapter.ServerName getServerName() {
        return serverName;
    }

    public FileType.SERVER getType() {
        return type == null ? getTemplate().getSoftware() : type;
    }

    public Integer getMemory() {
        return memory == null ? getTemplate().getDefaultMemoryMB() : memory;
    }

    public FileType.PLUGIN[] getPlugins() {
        return plugins == null ? new FileType.PLUGIN[0] : plugins;
    }

    public ServerTemplate getTemplate() {
        return template == null ? ServerTemplate.forServerName(serverName == null ? null : serverName.toString()) : template;
    }
}

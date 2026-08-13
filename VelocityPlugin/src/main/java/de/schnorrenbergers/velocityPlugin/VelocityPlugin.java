package de.schnorrenbergers.velocityPlugin;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import de.hems.communication.ListenerAdapter;
import de.hems.communication.events.server.ServerRegisteredEvent;
import de.hems.communication.events.server.ServerUnregisteredEvent;
import de.hems.communication.events.types.Event;
import de.hems.communication.events.types.EventHandler;
import de.hems.types.Server;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * Keeps the proxy in sync with the launcher.
 * <p>
 * Without this, only the servers that existed when velocity.toml was written could be reached, which is why
 * warping used to work for the lobby and survival only. The plugin joins the network, listens for servers
 * that come and go and registers them on the running proxy, so every server - including the ones created
 * for an event a minute ago - can be warped to right away.
 */
@Plugin(
    id = "velocityplugin",
    name = "VelocityPlugin",
    version = "1.0-SNAPSHOT"
)
public class VelocityPlugin {

    @Inject private Logger logger;
    @Inject private ProxyServer proxy;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            new ListenerAdapter(ListenerAdapter.ServerName.VELOCITY);
        } catch (Exception e) {
            logger.error("Could not join the MCServer network - new servers will not show up automatically", e);
            return;
        }
        ListenerAdapter.register(ServerRegisteredEvent.class, new EventHandler<ServerRegisteredEvent>() {
            @Override
            public void onEvent(Event received) {
                if (!(received instanceof ServerRegisteredEvent registered)) return;
                registerServer(registered.getServer());
            }
        });
        ListenerAdapter.register(ServerUnregisteredEvent.class, new EventHandler<ServerUnregisteredEvent>() {
            @Override
            public void onEvent(Event received) {
                if (!(received instanceof ServerUnregisteredEvent unregistered)) return;
                unregisterServer(unregistered.getServerName());
            }
        });
        logger.info("Connected to the MCServer network, servers are now registered automatically");
    }

    /**
     * Adds a server to the running proxy, replacing an older entry with the same name.
     *
     * @param server the server that was started
     */
    private void registerServer(Server server) {
        if (server == null || server.name == null || server.port <= 0) return;
        ServerInfo info = new ServerInfo(server.name, new InetSocketAddress("localhost", server.port));
        Optional<RegisteredServer> existing = proxy.getServer(server.name);
        if (existing.isPresent()) {
            if (existing.get().getServerInfo().getAddress().getPort() == server.port) return;
            proxy.unregisterServer(existing.get().getServerInfo());
        }
        proxy.registerServer(info);
        logger.info("Registered server {} on port {}", server.name, server.port);
    }

    /**
     * Removes a server from the running proxy.
     *
     * @param serverName the server that was stopped
     */
    private void unregisterServer(ListenerAdapter.ServerName serverName) {
        if (serverName == null) return;
        proxy.getServer(serverName.toString())
                .ifPresent((registered) -> {
                    proxy.unregisterServer(registered.getServerInfo());
                    logger.info("Unregistered server {}", serverName);
                });
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
    }
}

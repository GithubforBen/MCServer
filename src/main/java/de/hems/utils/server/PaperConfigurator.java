package de.hems.utils.server;

public class PaperConfigurator extends ServerConfigurator {

    private final String ip;
    private final int port;
    private final boolean isProxyed;
    private final String[] ops;
    private final String[] whitelist;

    public PaperConfigurator(String ip, int port, boolean isProxyed, String[] ops, String[] whitelist, String directory) {
        super(directory);
        this.ip = ip;
        this.port = port;
        this.isProxyed = isProxyed;
        this.ops = ops;
        this.whitelist = whitelist;
    }

    public void configure() throws Exception {
        writeToFile("server.properties", "server-ip=" +ip);
        writeToFile("server.properties", "server-port="+port);
        if (isProxyed) writeToFile("server.properties", "online-mode=false");
    }
}

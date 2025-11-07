package de.hems.types;

import java.io.Serializable;

public class Server implements Serializable {
    private static final long serialVersionUID = 1930L;
    public String name;
    public int port;
    public int memory;

    public Server(String name, int port, int memory) {
        this.name = name;
        this.port = port;
        this.memory = memory;
    }

    public Server() {

    }
}

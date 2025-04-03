package org.atcraftmc.updater.server.service;

import org.atcraftmc.updater.server.MCUpdaterServer;

public abstract class Service implements Runnable {
    private final MCUpdaterServer server;

    protected Service(MCUpdaterServer server) {
        this.server = server;
    }

    public final MCUpdaterServer server() {
        return server;
    }

    public void start() {
        new Thread(this, getName()).start();
    }

    public abstract String getName();

    public abstract void stop();
}

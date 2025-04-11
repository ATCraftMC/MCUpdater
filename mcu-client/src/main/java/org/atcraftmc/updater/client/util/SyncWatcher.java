package org.atcraftmc.updater.client.util;

import io.netty.channel.ChannelFuture;

import java.util.function.Consumer;

public final class SyncWatcher implements Runnable {
    private final ChannelFuture future;
    private final Consumer<Object> callback;

    public SyncWatcher(ChannelFuture future, Consumer<Object> callback) {
        this.future = future;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            this.callback.accept(this.future.sync());
        } catch (InterruptedException e) {
            this.callback.accept(e);
        }
    }
}

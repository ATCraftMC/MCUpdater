package org.atcraftmc.updater.client.handler;

import io.netty.channel.ChannelHandlerContext;
import me.gb2022.simpnet.packet.Packet;
import me.gb2022.simpnet.packet.PacketInboundHandler;
import org.atcraftmc.updater.channel.UpdateChannelMeta;
import org.atcraftmc.updater.client.util.Log;
import org.atcraftmc.updater.protocol.P22_UpdateChannelDataRequest;
import org.atcraftmc.updater.protocol.P23_UpdateChannelList;

import java.util.Set;
import java.util.function.Consumer;

public final class VersionChannelRequestHandler extends PacketInboundHandler {
    private final Consumer<Set<UpdateChannelMeta>> callback;

    public VersionChannelRequestHandler(Consumer<Set<UpdateChannelMeta>> callback) {
        this.callback = callback;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        Log.info("requesting remote config...");
        ctx.writeAndFlush(new P22_UpdateChannelDataRequest());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) {
        if (packet instanceof P23_UpdateChannelList data) {
            Log.info("received remote config...");
            this.callback.accept(data.getMetas());
            ctx.pipeline().remove(this);
        }
    }
}

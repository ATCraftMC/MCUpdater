package org.atcraftmc.updater.client.handler;

import io.netty.channel.ChannelHandlerContext;
import me.gb2022.simpnet.packet.Packet;
import org.atcraftmc.updater.client.Event;
import org.atcraftmc.updater.client.util.DeferredTaskManager;
import org.atcraftmc.updater.protocol.P02_ClientConversationEnd;
import org.atcraftmc.updater.protocol.P0F_ServerProgressUpdate;
import org.atcraftmc.updater.protocol.P10_VersionInfo;

public final class NetworkHandler extends MCUNetHandler {

    @Override
    public void handlePacket(Packet packet, ChannelHandlerContext ctx) {
        if (packet instanceof P10_VersionInfo p) {
            runTask(() -> {
                ctx.writeAndFlush(new P02_ClientConversationEnd());
                ctx.disconnect();
                DeferredTaskManager.batch();
                callEvent(Event.RECEIVE_VERSION, p);
            });
        }
        if (packet instanceof P0F_ServerProgressUpdate p) {
            callEvent(Event.PROGRESS, "[服务器]" + p.getData());
        }
    }
}

package org.atcraftmc.updater.protocol;

import me.gb2022.simpnet.channel.NettyChannelInitializer;
import me.gb2022.simpnet.packet.PacketRegistry;

public interface MCUProtocol {
    int IDENTIFIER = 114514;
    PacketRegistry PACKETS = new PacketRegistry(512, (i) -> {
        i.register(0x00, C00LoginRequest.class);
        i.register(0x0F, P0F_ServerProgressUpdate.class);

        i.register(0x10, P10_VersionInfo.class);
        i.register(0x11, P11_FileExpand.class);
        i.register(0x12, P12_FileDelete.class);
        i.register(0x1F, P1F_UpdateProgressPredict.class);

        i.register(0x20, P20_VersionLogRequest.class);
        i.register(0x21, P21_VersionLogInfo.class);
    });

    static NettyChannelInitializer initializer() {
        return new NettyChannelInitializer().config((i) -> {
            i.lengthFrame();
            i.compression(1024, 1);
            i.packet(PACKETS);
        });
    }
}

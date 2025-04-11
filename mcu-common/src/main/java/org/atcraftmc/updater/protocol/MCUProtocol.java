package org.atcraftmc.updater.protocol;

import me.gb2022.simpnet.channel.NettyChannelInitializer;
import me.gb2022.simpnet.packet.PacketRegistry;

public interface MCUProtocol {
    int IDENTIFIER = 114514;

    PacketRegistry PACKETS = new PacketRegistry(512, (i) -> {
        i.register(0x00, P00_LoginRequest.class);
        i.register(0x02, P02_ClientConversationEnd.class);
        i.register(0x0F, P0F_ServerProgressUpdate.class);

        i.register(0x10, P10_VersionInfo.class);
        i.register(0x11, P11_FileExpand.class);
        i.register(0x12, P12_FileDelete.class);
        i.register(0x13, P13_PatchFileInfo.class);
        i.register(0x14, P14_PatchFileSlice.class);
        i.register(0x1F, P1F_UpdateProgressPredict.class);

        i.register(0x20, P20_VersionLogRequest.class);
        i.register(0x21, P21_VersionLogInfo.class);
        i.register(0x22, P22_UpdateChannelDataRequest.class);
        i.register(0x23, P23_UpdateChannelList.class);
    });

    static NettyChannelInitializer initializer() {
        return new NettyChannelInitializer().config((i) -> {
            i.lengthFrame();
            i.compression(1024, 1);
            i.packet(PACKETS);
        });
    }
}

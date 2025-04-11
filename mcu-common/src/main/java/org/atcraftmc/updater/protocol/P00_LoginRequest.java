package org.atcraftmc.updater.protocol;

import io.netty.buffer.ByteBuf;
import me.gb2022.simpnet.packet.DeserializedConstructor;
import me.gb2022.simpnet.packet.Packet;

public class P00_LoginRequest implements Packet {
    @Override
    public void write(ByteBuf byteBuf) {

    }

    @DeserializedConstructor
    public P00_LoginRequest(ByteBuf byteBuf) {
        super();
    }
}

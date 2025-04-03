package org.atcraftmc.updater.protocol;

import io.netty.buffer.ByteBuf;
import me.gb2022.simpnet.packet.DeserializedConstructor;
import me.gb2022.simpnet.packet.Packet;

public class C00LoginRequest implements Packet {
    @Override
    public void write(ByteBuf byteBuf) {

    }

    @DeserializedConstructor
    public C00LoginRequest(ByteBuf byteBuf) {
        super();
    }
}

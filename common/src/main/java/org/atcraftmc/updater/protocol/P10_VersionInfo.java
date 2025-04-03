package org.atcraftmc.updater.protocol;

import io.netty.buffer.ByteBuf;
import me.gb2022.simpnet.packet.DeserializedConstructor;
import me.gb2022.simpnet.packet.Packet;
import me.gb2022.simpnet.util.BufferUtil;

public final class P10_VersionInfo implements Packet {
    private final long timeStamp;
    private final String name;
    private final String desc;

    public P10_VersionInfo(long timeStamp, String name, String desc) {
        this.timeStamp = timeStamp;
        this.name = name;
        this.desc = desc;
    }

    @DeserializedConstructor
    public P10_VersionInfo(ByteBuf buf) {
        this.timeStamp = buf.readLong();
        this.name = BufferUtil.readString(buf);
        this.desc = BufferUtil.readString(buf);
    }

    @Override
    public void write(ByteBuf buffer) {
        buffer.writeLong(this.timeStamp);
        BufferUtil.writeString(buffer, this.name);
        BufferUtil.writeString(buffer, this.desc);
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }
}

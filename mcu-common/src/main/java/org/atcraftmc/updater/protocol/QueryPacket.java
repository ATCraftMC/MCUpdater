package org.atcraftmc.updater.protocol;

import me.gb2022.simpnet.packet.Packet;

public interface QueryPacket extends Packet {
    String getQueryId();
}

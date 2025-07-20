package org.atcraftmc.updater.protocol.packet;

import io.netty.buffer.ByteBuf;
import me.gb2022.simpnet.packet.DeserializedConstructor;
import me.gb2022.simpnet.packet.Packet;
import me.gb2022.simpnet.util.BufferUtil;

import java.util.HashSet;
import java.util.Set;

public final class P53_CDNDownloadRequest implements Packet {
    private final String repo;
    private final Set<String> targets;

    public P53_CDNDownloadRequest(String repo, Set<String> targets) {
        this.repo = repo;
        this.targets = targets;
    }

    @DeserializedConstructor
    public P53_CDNDownloadRequest(ByteBuf buffer) {
        this.repo = BufferUtil.readString(buffer);
        this.targets = new HashSet<>();
        var len = buffer.readShort();

        for (var i = 0; i < len; i++) {
            this.targets.add(BufferUtil.readString(buffer));
        }
    }

    @Override
    public void write(ByteBuf buffer) {
        BufferUtil.writeString(buffer, this.repo);
        buffer.writeShort(this.targets.size());
        for (var target : this.targets) {
            BufferUtil.writeString(buffer, target);
        }
    }

    public String getRepo() {
        return repo;
    }

    public Set<String> getTargets() {
        return targets;
    }
}

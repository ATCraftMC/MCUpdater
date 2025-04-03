package org.atcraftmc.updater.server.service;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import me.gb2022.simpnet.packet.Packet;
import me.gb2022.simpnet.packet.PacketInboundHandler;
import org.atcraftmc.updater.protocol.MCUProtocol;
import org.atcraftmc.updater.protocol.P10_VersionInfo;
import org.atcraftmc.updater.protocol.P20_VersionLogRequest;
import org.atcraftmc.updater.protocol.P21_VersionLogInfo;
import org.atcraftmc.updater.server.MCUpdaterServer;

public final class NetworkService extends Service {
    private final NioEventLoopGroup bossGroup = new NioEventLoopGroup();
    private final NioEventLoopGroup workerGroup = new NioEventLoopGroup();

    public NetworkService(MCUpdaterServer server) {
        super(server);
    }

    @Override
    public void run() {
        try {
            ServerBootstrap sbs = new ServerBootstrap();
            sbs.group(this.bossGroup, this.workerGroup);
            sbs.channel(NioServerSocketChannel.class);
            sbs.option(ChannelOption.SO_BACKLOG, 128);
            sbs.childOption(ChannelOption.SO_KEEPALIVE, true);
            sbs.handler(new LoggingHandler(LogLevel.INFO));
            sbs.childHandler(MCUProtocol.initializer().handler(NetHandler::new));

            var cf = sbs.bind(65320).sync();

            cf.channel().closeFuture().sync();
        } catch (Exception e) {
            MCUpdaterServer.LOGGER.catching(e);
            MCUpdaterServer.LOGGER.info("网络管线出现错误, 正在关闭服务器...");
            server().stop();
        }
    }

    @Override
    public String getName() {
        return "network-service";
    }

    public void stop() {
        MCUpdaterServer.LOGGER.info("正在关闭网络管线...");
        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
    }

    private class NetHandler extends PacketInboundHandler {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Packet packet) {

            if (packet instanceof P20_VersionLogRequest p) {
                ctx.writeAndFlush(new P21_VersionLogInfo(server().versionLog(p.getVersion())));
            }

            if (packet instanceof P10_VersionInfo vi) {
                server().getExecutor().submit(() -> server().handleVersion(vi, ctx));
                return;
            }
            ctx.disconnect();
        }
    }
}

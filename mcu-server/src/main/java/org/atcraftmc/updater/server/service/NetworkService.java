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
import org.atcraftmc.updater.protocol.*;
import org.atcraftmc.updater.server.MCUpdaterServer;
import org.atcraftmc.updater.server.file.FileSource;

import java.util.stream.Collectors;

import static org.atcraftmc.updater.server.MCUpdaterServer.LOGGER;

public final class NetworkService extends Service {
    private final NioEventLoopGroup bossGroup = new NioEventLoopGroup();
    private final NioEventLoopGroup workerGroup = new NioEventLoopGroup();

    public NetworkService(MCUpdaterServer server) {
        super(server);
    }

    @Override
    public void run() {
        var config = server().config();

        var address = config.getString("server-address");
        var port = config.getInt("server-port");

        LOGGER.info("正在启动网络服务...");

        try {
            ServerBootstrap sbs = new ServerBootstrap();
            sbs.group(this.bossGroup, this.workerGroup);
            sbs.channel(NioServerSocketChannel.class);
            sbs.option(ChannelOption.SO_BACKLOG, 128);
            sbs.childOption(ChannelOption.SO_KEEPALIVE, true);
            sbs.handler(new LoggingHandler(LogLevel.INFO));
            sbs.childHandler(MCUProtocol.initializer().handler(NetHandler::new));

            var cf = sbs.bind(port).sync();

            LOGGER.info("成功启动网络服务于 {}:{}", address, port);

            cf.channel().closeFuture().sync();
        } catch (Exception e) {
            LOGGER.catching(e);
            LOGGER.info("网络管线出现错误, 正在关闭服务器...");
            server().stop();
        }
    }

    @Override
    public String name() {
        return "network-service";
    }

    public void stop() {
        LOGGER.info("正在关闭网络管线...");
        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
    }

    private class NetHandler extends PacketInboundHandler {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Packet packet) {
            if (packet instanceof P22_UpdateChannelDataRequest) {
                LOGGER.info("received client setting request: {}", ctx.channel().remoteAddress());
                ctx.writeAndFlush(new P23_UpdateChannelList(server().fileService()
                                                                    .sources()
                                                                    .values()
                                                                    .stream()
                                                                    .map(FileSource::meta)
                                                                    .collect(Collectors.toSet())));
                LOGGER.info("sent setting to client: {}", ctx.channel().remoteAddress());
                return;
            }

            if (packet instanceof P20_VersionLogRequest p) {
                LOGGER.info("received client version log request: {}", ctx.channel().remoteAddress());
                ctx.writeAndFlush(new P21_VersionLogInfo(server().versionLog(p.getVersion())));
                LOGGER.info("sent version log to client: {}", ctx.channel().remoteAddress());
                return;
            }

            if (packet instanceof P10_VersionInfo vi) {
                LOGGER.info("received client update request: {}", ctx.channel().remoteAddress());
                server().getExecutor().submit(() -> server().sendUpdateDataResponse(vi, ctx));
                return;
            }

            ctx.disconnect();
        }
    }
}

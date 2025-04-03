package org.atcraftmc.updater.client;

import com.google.gson.JsonParser;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import me.gb2022.commons.http.HttpMethod;
import me.gb2022.commons.http.HttpRequest;
import me.gb2022.simpnet.packet.Packet;
import me.gb2022.simpnet.packet.PacketInboundHandler;
import org.atcraftmc.updater.FilePath;
import org.atcraftmc.updater.client.ui.ErrorUI;
import org.atcraftmc.updater.client.ui.MainWindowUI;
import org.atcraftmc.updater.client.ui.UpdateViewingUI;
import org.atcraftmc.updater.command.VersionInfo;
import org.atcraftmc.updater.protocol.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class MCUpdaterClient {
    private final ExecutorService executor = Executors.newFixedThreadPool(4);


    static long getTimeStamp() {
        var file = new File(FilePath.updater() + "/version.dat");

        if (!file.exists() || file.length() <= 0) {
            return -1;
        }

        try (var i = new FileInputStream(file)) {
            return Long.parseLong(new String(i.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void setTimeStamp(long time) {
        var file = new File(FilePath.updater() + "/version.dat");

        if (!file.exists() || file.length() <= 0) {
            file.getParentFile().mkdirs();
        }
        try (var o = new FileOutputStream(file)) {
            o.write(String.valueOf(time).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    /*
    static void showVersionLog(VersionInfo info) {
        var version = HttpRequest.http(HttpMethod.GET, ClientBootstrap.SERVICE.get())
                .path("/log")
                .param("version", info.getVersion())
                .build()
                .request();

        var text = JsonParser.parseString(version).getAsJsonObject().get("text").getAsString();
        UpdateViewingUI.view(info.getVersion(), text, info.getTimeStamp());
    }

    static List<VersionInfo> getVersionsForUpdate() {
        var version = HttpRequest.http(HttpMethod.GET, ClientBootstrap.SERVICE.get())
                .path("/version")
                .param("version", "_query")
                .param("time", String.valueOf(getTimeStamp()))
                .build()
                .request();

        return JsonParser.parseString(version)
                .getAsJsonArray()
                .asList()
                .stream()
                .map((e) -> new VersionInfo(e.getAsJsonObject()))
                .sorted(Comparator.comparingLong(VersionInfo::getTimeStamp))
                .toList();
    }

    static VersionInfo getLatestVersion() {
        var version = HttpRequest.http(HttpMethod.GET, ClientBootstrap.SERVICE.get())
                .path("/version")
                .param("version", "_latest")
                .param("time", String.valueOf(getTimeStamp()))
                .build()
                .request();

        return new VersionInfo(JsonParser.parseString(version).getAsJsonArray().get(0).getAsJsonObject());
    }

    static void run(UIHandle<MainWindowUI> handle) {
        var ctx = handle.ui();

        ctx.setCommentMessage("正在下载版本信息...");
        ctx.setProgress(100);

        if (getTimeStamp() == -1) {
            new PatchOperation(Set.of("installer.zip")).accept(ctx);

            ClientBootstrap.notify("客户端初始化完成", "客户端资源已下载并安装。");
            showVersionLog(getLatestVersion());
            setTimeStamp(getLatestVersion().getTimeStamp());
            handle.close();
            return;
        }

        var versions = getVersionsForUpdate();

        if (versions.isEmpty()) {
            ClientBootstrap.notify("客户端已经为最新版本", "当前客户端版本已为最新, 游戏进程准备启动。");
            handle.close();
            return;
        }

        for (var i = 0; i < versions.size(); i++) {
            handle.frame().setTitle("MCUpdater - 正在安装第 (%d/%d) 个版本更新".formatted(i + 1, versions.size()));

            for (var operations : versions.get(i).getOperations()) {
                try {
                    operations.accept(ctx);
                    setTimeStamp(versions.get(i).getTimeStamp());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            handle.close();

            showVersionLog(versions.get(versions.size() - 1));
            ClientBootstrap.notify("客户端更新完成", "当前客户端版本已为最新, 游戏进程准备启动。");
        }
    }
    */

    void run() {
        var lock = new ArrayBlockingQueue<>(1);

        var address = ClientBootstrap.config().service().split(":");

        MainWindowUI.open((handle) -> this.executor.submit(new WorkingThread(address[0], Integer.parseInt(address[1]), handle.ui(), (o) -> {
            if (o instanceof Throwable t) {
                handle.close();
                ErrorUI.open(t);
                lock.add(new Object());
                ClientBootstrap.notify("客户端更新异常", "发生了一些错误。");
                handle.close();
                return;
            }
            if (o instanceof P10_VersionInfo v) {
                if (getTimeStamp() != v.getTimeStamp()) {
                    setTimeStamp(v.getTimeStamp());
                    UpdateViewingUI.view(v.getName(), v.getDesc(), v.getTimeStamp());
                }
                ClientBootstrap.notify("客户端更新完成", "当前版本: " + v.getName());
                handle.close();
                lock.add(new Object());
            }
        })));

        try {
            lock.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleFileExpand(P11_FileExpand p) {
        try {
            for (var entry : p.getList().entrySet()) {
                var path = entry.getKey();
                var data = entry.getValue();

                var file = new File(FilePath.runtime() + path);

                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }

                try (var out = new FileOutputStream(file)) {
                    out.write(data);
                    out.flush();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleFileDelete(P12_FileDelete p) {
        for (var path : p.getPaths()) {
            var file = new File(FilePath.runtime() + path);
            var dest = new File(FilePath.runtime() + "/.updater/removed/" + path);

            if (!file.exists()) {
                continue;
            }

            try {
                dest.getParentFile().mkdirs();
                dest.createNewFile();

                Files.move(Path.of(file.getAbsolutePath()), Path.of(dest.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    class WorkingThread extends Thread {
        private final String ip;
        private final int port;
        private final Consumer<Object> callback;
        private final MainWindowUI ui;

        public WorkingThread(String ip, int port, MainWindowUI ui, Consumer<Object> callback) {
            this.ip = ip;
            this.port = port;
            this.callback = callback;
            this.ui = ui;
        }

        @Override
        public void run() {
            var group = new NioEventLoopGroup();
            var bs = new Bootstrap();

            try {
                bs.group(group)
                        .channel(NioSocketChannel.class)
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .handler(MCUProtocol.initializer().handler(() -> new NetHandler(this.callback, this.ui)));

                this.callback.accept("正在连接到更新服务器...");
                var cf = bs.connect(this.ip, this.port).sync();
                this.callback.accept("正在下载更新...");
                cf.channel().closeFuture().sync();
            } catch (Exception e) {
                this.callback.accept(e);
            } finally {
                group.shutdownGracefully();
            }
        }
    }

    class NetHandler extends PacketInboundHandler {
        private final Consumer<Object> callback;
        private final MainWindowUI ui;
        private long totalCount;
        private long currentCount;

        NetHandler(Consumer<Object> callback, MainWindowUI ui) {
            this.callback = callback;
            this.ui = ui;
        }

        public void update(long work) {
            float progress = (float) work / (float) totalCount;

            this.ui.setCommentMessage("正在下载更新数据包 (%s/%sMB)".formatted(work / 1048576, totalCount / 1048576));
            this.ui.setProgress((int) (progress * 100));
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            ctx.writeAndFlush(new P10_VersionInfo(getTimeStamp(), "", ""));
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, Packet packet) {
            if (packet instanceof P1F_UpdateProgressPredict p) {
                this.totalCount = p.getSize();
            }

            if (packet instanceof P0F_ServerProgressUpdate p) {
                this.ui.setCommentMessage("[服务器]" + p.getData());
                this.ui.setProgress(100);
            }

            if (packet instanceof P10_VersionInfo p) {
                ctx.disconnect();
                executor.shutdown();
                this.callback.accept(p);
            }

            if (packet instanceof P11_FileExpand p) {
                this.currentCount += p.getList().values().stream().mapToLong((b) -> b.length).sum();
                update(this.currentCount);
                executor.submit(() -> handleFileExpand(p));
            }
            if (packet instanceof P12_FileDelete p) {
                executor.submit(() -> handleFileDelete(p));
            }
        }
    }
}

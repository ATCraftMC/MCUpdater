package org.atcraftmc.updater.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atcraftmc.updater.FilePath;
import org.atcraftmc.updater.data.FileManager;
import org.atcraftmc.updater.data.Repository;
import org.atcraftmc.updater.data.diff.GitIgnoreFilter;
import org.atcraftmc.updater.protocol.*;
import org.atcraftmc.updater.server.service.ConsoleService;
import org.atcraftmc.updater.server.service.NetworkService;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MCUpdaterServer {
    public static final Logger LOGGER = LogManager.getLogger("");
    private final ConsoleService console = new ConsoleService(this);
    private final NetworkService network = new NetworkService(this);


    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final FileManager fileManager = new FileManager();
    private final VersionManager versionManager = new VersionManager();
    private final HttpServer server;

    private final FileConfiguration config = new YamlConfiguration();


    public MCUpdaterServer() {
        try {
            this.server = HttpServer.create();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean loadConfiguration() {
        var file = new File(FilePath.runtime() + "/config.yml");

        if (!file.exists() || file.length() == 0) {
            LOGGER.warn("没有找到默认的配置文件，正在覆盖生成...");

            try (var out = new FileOutputStream(file); var in = this.getClass().getResourceAsStream("/config.yml")) {
                out.write(Objects.requireNonNull(in).readAllBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            LOGGER.info("配置文件生成于 {}", file.getAbsolutePath());

            return false;
        }

        try {
            this.config.load(file);
            return true;
        } catch (IOException | InvalidConfigurationException e) {
            LOGGER.warn("读取配置文件时发生错误!");
            LOGGER.catching(e);

            return false;
        }
    }


    public void init() {
        this.console.start();
        this.console.waitFor();

        if (!loadConfiguration()) {
            LOGGER.warn("读取配置文件时遇到了一些问题，请重新配置并启动服务器。");
            System.exit(1);
        }

        LOGGER.info("正在加载版本信息...");
        this.versionManager.load();
        LOGGER.info("共加载了 {} 个版本", this.versionManager.getVersions().size());

        LOGGER.info("正在加载配置");
        var serverConfig = this.loadConfig().getAsJsonObject("server");

        this.fileManager.init();

        var ip = serverConfig.get("bind").getAsString();
        var port = serverConfig.get("port").getAsInt();

        setupServer(port);

        LOGGER.info("成功启动TCP服务于 {}:{}", ip, port);
        LOGGER.info("启动完成!");
    }

    public void stop() {
        this.server.stop(0);
        this.console.stop();
    }

    public JsonObject loadConfig() {
        var config = loadConfigDOM();
        config.getAsJsonObject("source").asMap().forEach((key, value) -> {
            var node = value.getAsJsonObject();
            var pathNode = node.get("path").getAsString();
            var regexNode = node.get("ignore").getAsJsonArray();

            var path = pathNode.startsWith(".") ? FilePath.runtime() + pathNode.substring(1) : pathNode;
            var regex = regexNode.asList().stream().map(JsonElement::getAsString).distinct().toArray(String[]::new);
            var repo = new Repository(new File(path), new GitIgnoreFilter(regex));

            this.fileManager.getRegisteredSources().put(key, repo);

            LOGGER.info("添加数据源: {} -> {}", key, path);
        });

        return config;
    }

    public JsonObject loadConfigDOM() {
        var file = new File(FilePath.updater() + "/server-config.json");

        if (!file.exists() || file.length() == 0) {
            file.getParentFile().mkdirs();
            try (var i = getClass().getResourceAsStream("/server-config.json"); var o = new FileOutputStream(file)) {
                o.write(Objects.requireNonNull(i).readAllBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try (var i = new FileInputStream(file)) {
            return JsonParser.parseReader(new InputStreamReader(i)).getAsJsonObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public HttpServer getServer() {
        return server;
    }

    public void setupServer(int port) {

    }

    public void makeUpdate(String[] args) {
        if (args.length < 2) {
            LOGGER.error("请指定版本号");
            return;
        }

        var version = args[1];

        if (this.versionManager.hasVersion(version)) {
            LOGGER.error("版本号 {} 已经存在。", version);
            return;
        }

        var path = FilePath.versions() + "/" + version + ".txt";
        var file = new File(path);

        if (!file.exists() || file.length() == 0) {
            System.out.printf("警告: 版本 %s 对应的日志文件不存在或为空%n", version);
            System.out.printf("警告: 请创建文件 %s 并写入更新日志%n", path);
        }

        try {
            var vi = this.fileManager.generate(version);
            this.versionManager.createVersion(vi);
        } catch (IllegalStateException ignored) {
            System.out.println("没有更改发生, 版本生成终止。");
        }
    }

    public void cmd_build(String[] command) {
        if (command.length < 2) {
            LOGGER.error("请指定版本号");
            return;
        }

        var version = command[1];

        if (this.versionManager.hasVersion(version)) {
            LOGGER.error("版本号 {} 已经存在。", version);
            return;
        }

        try {
            var vi = this.fileManager.createVersion(version);
            this.versionManager.registerVersion(vi);
            this.versionManager.registerVersion(this.fileManager.createInstallDummyVersion());
        } catch (IllegalStateException ignored) {
            LOGGER.error("没有文件更改，生成撤销。");
        }
    }

    public String versionLog(String id) {
        var f = new File(FilePath.versions() + "/" + id + ".txt");

        if (!f.exists() || f.length() == 0) {
            return "没有版本信息 :(";
        }


        try (var in = new FileInputStream(f)) {
            return new String(in.readAllBytes());
        } catch (Exception e) {
            return "没有版本信息 :(";
        }
    }

    public void handleVersion(P10_VersionInfo vi, ChannelHandlerContext ctx) {
        var t = vi.getTimeStamp();

        ctx.writeAndFlush(new P0F_ServerProgressUpdate("正在合并版本信息..."));

        var version = versionManager.mergeFrom(t);

        ctx.writeAndFlush(new P12_FileDelete(version.getRemoveList().toArray(new String[0])));

        ctx.writeAndFlush(new P0F_ServerProgressUpdate("正在准备文件..."));
        var totalSize = version.getAddList().values().stream().map((fd) -> fd.file(this.fileManager)).mapToLong(File::length).sum();

        ctx.writeAndFlush(new P1F_UpdateProgressPredict(totalSize));
        ctx.writeAndFlush(new P0F_ServerProgressUpdate("准备开始下载..."));

        var size = 0;
        var packet = new P11_FileExpand();

        for (var data : version.getAddList().entrySet()) {
            var file = data.getValue().file(this.fileManager);

            try (var in = new FileInputStream(file)) {
                var payload = in.readAllBytes();
                size += payload.length;
                packet.add(data.getKey(), payload);

                if (size > 524288) {
                    ctx.writeAndFlush(packet);
                    packet = new P11_FileExpand();
                    size = 0;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        ctx.writeAndFlush(packet);
        ctx.writeAndFlush(new P10_VersionInfo(version.getTimestamp(), version.getVersion(), versionLog(version.getVersion())));
    }

    public ExecutorService getExecutor() {
        return this.executor;
    }

}

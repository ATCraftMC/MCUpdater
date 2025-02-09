package org.atcraftmc.updater.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.atcraftmc.updater.FilePath;
import org.atcraftmc.updater.server.diffcheck.FileManager;
import org.atcraftmc.updater.server.diffcheck.GitIgnoreFilter;
import org.atcraftmc.updater.server.diffcheck.Repository;
import org.atcraftmc.updater.server.http.PatchFileProvider;
import org.atcraftmc.updater.server.http.VersionHistoryProvider;
import org.atcraftmc.updater.server.http.VersionInfoProvider;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.Scanner;

public class MCUpdaterServer {
    private final FileManager fileManager = new FileManager();
    private final VersionManager versionManager = new VersionManager();
    private final HttpServer server;

    public MCUpdaterServer() {
        try {
            this.server = HttpServer.create();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void init() {
        var scanner = new Scanner(System.in);

        System.out.println("======[MCUpdater Server v1.0.0]======");
        System.out.println(" - 'make <version>': 生成更新");
        System.out.println(" - 'stop': 停止服务器");
        System.out.println(" - 'reload': 重新加载配置(网络服务器修改需要重启)");
        System.out.println(" - 'list': 列出全部版本");

        System.out.println("正在加载版本信息...");
        this.versionManager.load();
        System.out.printf("共加载了 %s 个版本%n", this.versionManager.getVersions().size());


        System.out.println("正在加载配置");
        var serverConfig = this.loadConfig().getAsJsonObject("server");

        this.fileManager.init();

        var ip = serverConfig.get("bind").getAsString();
        var port = serverConfig.get("port").getAsInt();

        try {
            this.server.bind(new InetSocketAddress(ip, port), 1048576);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.server.createContext("/patch", new PatchFileProvider());
        this.server.createContext("/log", new VersionHistoryProvider());
        this.server.createContext("/version", new VersionInfoProvider(this.versionManager));

        this.server.start();

        System.out.printf("成功启动HTTP服务于 %s:%s%n", ip, port);
        System.out.println("启动完成!");

        // 持续监听用户输入，直到输入 "exit" 为止
        while (true) {
            var line = scanner.nextLine();
            if (line.isEmpty() || line.isBlank()) {
                continue;
            }

            var input = line.split(" ");

            System.out.println(">> " + line);

            if ("stop".equalsIgnoreCase(input[0])) {
                this.stop();
                break;
            }

            if ("make".equalsIgnoreCase(input[0])) {
                if (input.length < 2) {
                    System.out.println("错误: 请指定版本号");
                    continue;
                }

                var version = input[1];

                if (this.versionManager.hasVersion(version)) {
                    System.out.println("错误: 版本号 %s 已经存在。");
                    continue;
                }

                var path = FilePath.versions() + "/" + version + ".txt";
                var file = new File(path);

                if (!file.exists() || file.length() == 0) {
                    System.out.printf("警告: 版本 %s 对应的日志文件不存在或为空", version);
                    System.out.printf("警告: 请创建文件 %s 并写入更新日志%n", path);
                }

                try {
                    var vi = this.fileManager.generate(version);
                    this.versionManager.createVersion(vi);
                } catch (IllegalStateException ignored) {
                    System.out.println("没有更改发生, 版本生成终止。");
                }
            }

            if ("reload".equalsIgnoreCase(input[0])) {
                System.out.println("正在重新加载文件...");
                this.loadConfig();
                System.out.println("配置文件已经更新。HTTP服务需要重启才能应用修改。");
            }
        }

        // 关闭 Scanner
        scanner.close();
    }

    public void stop() {
        System.out.println("正在关闭HTTP服务...");
        this.server.stop(0);
    }

    public JsonObject loadConfig() {
        var config = loadConfigDOM();
        var sources = this.fileManager.getSources();
        config.getAsJsonObject("source").asMap().forEach((key, value) -> {
            var path = key.startsWith(".") ? FilePath.runtime() + key.substring(1) : key;
            var regex = value.getAsJsonArray().asList().stream().map(JsonElement::getAsString).distinct().toArray(String[]::new);
            sources.add(new Repository(new File(path), new GitIgnoreFilter(regex)));

            System.out.println("> 添加数据源: " + path);
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
}

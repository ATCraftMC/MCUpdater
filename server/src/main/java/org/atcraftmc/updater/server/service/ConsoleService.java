package org.atcraftmc.updater.server.service;

import org.atcraftmc.updater.ProductInfo;
import org.atcraftmc.updater.server.MCUpdaterServer;

import java.util.Scanner;

public final class ConsoleService extends Service {
    private boolean running = true;
    private boolean initialized = false;

    public ConsoleService(MCUpdaterServer server) {
        super(server);
    }

    public void stop() {
        this.running = false;
    }


    @Override
    public void run() {
        var scanner = new Scanner(System.in);
        this.init();
        while (this.running) {
            var line = scanner.nextLine();
            if (line.isEmpty() || line.isBlank()) {
                continue;
            }
            MCUpdaterServer.LOGGER.info(">> {}", line);
            this.handleCommand(line.split(" "));
        }
        scanner.close();
    }

    public void init() {
        for (var s : ProductInfo.logo("Server", ProductInfo.VERSION).split("\n")) {
            MCUpdaterServer.LOGGER.info(s);
        }
        help();
        this.initialized = true;
    }

    public void waitFor() {
        while (!this.initialized) {
            Thread.yield();
        }
    }

    public void help() {
        MCUpdaterServer.LOGGER.info(" - stop: 停止服务器");
        MCUpdaterServer.LOGGER.info(" - reload: 重新加载服务器配置");
        MCUpdaterServer.LOGGER.info(" - help: 显示帮助信息");
        MCUpdaterServer.LOGGER.info(" - build <版本>: 构建版本");
    }

    @Override
    public String getName() {
        return "console-service";
    }

    public void handleCommand(String[] input) {
        switch (input[0]) {
            case "stop" -> server().stop();
            case "build" -> server().cmd_build(input);
            case "help" -> {
                MCUpdaterServer.LOGGER.info("==========[帮助信息]==========");
                help();
            }
            case "reload" -> {
                MCUpdaterServer.LOGGER.info("正在重新加载文件...");
                server().loadConfig();
                MCUpdaterServer.LOGGER.info("配置文件已经更新。网络服务需要重启才能应用修改。");
            }
        }
    }
}

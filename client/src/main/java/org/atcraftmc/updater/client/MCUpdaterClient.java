package org.atcraftmc.updater.client;

import com.google.gson.JsonParser;
import me.gb2022.commons.http.HttpMethod;
import me.gb2022.commons.http.HttpRequest;
import org.atcraftmc.updater.FilePath;
import org.atcraftmc.updater.client.ui.ErrorUI;
import org.atcraftmc.updater.client.ui.ProgressUI;
import org.atcraftmc.updater.client.ui.UpdateViewingUI;
import org.atcraftmc.updater.client.util.UIHandle;
import org.atcraftmc.updater.command.UpdateOperationListener;
import org.atcraftmc.updater.command.VersionInfo;
import org.atcraftmc.updater.command.operation.PatchOperation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

public interface MCUpdaterClient {

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

    static void showVersionLog(VersionInfo info) {
        var version = HttpRequest.http(HttpMethod.GET, ClientBootstrap.SERVICE.get())
                .path("/log")
                .param("version", info.getVersion())
                .build()
                .request();

        var text = JsonParser.parseString(version).getAsJsonObject().get("text").getAsString();
        UpdateViewingUI.view(info.getVersion(), text, info.getTimeStamp());
    }

    static void run() {
        var lock = new ArrayBlockingQueue<>(1);

        ProgressUI.open((handle) -> {
            try {
                run(handle);
            } catch (Exception e) {
                handle.close();
                ErrorUI.open(e);
            }
            lock.add(new Object());
        });

        try {
            lock.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static void run(UIHandle<ProgressUI> handle) {
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
            ClientBootstrap.notify("客户端已经更新完成", "当前客户端版本已为最新, 游戏进程准备启动。");
        }
    }
}

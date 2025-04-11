package org.atcraftmc.updater.data;

import me.gb2022.commons.container.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atcraftmc.updater.FilePath;
import org.atcraftmc.updater.PatchFile;
import org.atcraftmc.updater.command.VersionInfo;
import org.atcraftmc.updater.command.operation.DeleteOperation;
import org.atcraftmc.updater.command.operation.PatchOperation;
import org.atcraftmc.updater.data.diff.DiffCheck;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public final class FileManager {
    public static final Logger LOGGER = LogManager.getLogger("文件服务");

    private final Map<String, Repository> sources = new HashMap<>();

    public Collection<Repository> getSources() {
        return sources.values();
    }

    public void init() {
        var zip = new File(FilePath.packs() + "/installer.zip");
        if (!zip.exists() || zip.length() == 0) {
            System.out.println("> 生成安装资源包");
            createInstallerZip();
        }
    }

    public Map<String, Pair<FileData, FileModifyStatus>> checkFiles() {
        var checkList = new HashSet<String>();
        var map = new HashMap<String, Pair<FileData, FileModifyStatus>>();

        for (var rid : this.sources.keySet()) {
            var repo = this.sources.get(rid);
            for (var file : repo.collect()) {
                var resPath = repo.pathOf(file);
                var state = DiffCheck.checkFile(file, repo, checkList);

                map.put(resPath, new Pair<>(new FileData(rid, resPath), state));
            }
        }

        for (var file : DiffCheck.files()) {
            if (checkList.contains(file.getAbsolutePath())) {
                continue;
            }

            try (var in = new FileInputStream(file)) {
                in.readNBytes(32);
                map.put(
                        FilePath.normalize(new String(in.readAllBytes(), StandardCharsets.UTF_8)),
                        new Pair<>(null, FileModifyStatus.DELETE)
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            file.delete();
        }

        return map;
    }

    public ModernVersionInfo createVersion(String id) {
        var time = System.currentTimeMillis();

        LOGGER.info("开始生成版本 {} (打包时间戳 {} / {})", id, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), time);
        LOGGER.info("开始分析文件差异....");

        var fileStatus = checkFiles();

        var deleteFiles = new HashSet<String>();
        var addFiles = new HashMap<String, FileData>();

        LOGGER.info("检查完成，源内共 {} 个文件", fileStatus.size());

        if (fileStatus.values().stream().allMatch((p) -> p.getRight() == FileModifyStatus.NONE)) {
            LOGGER.error("没有任何变更的文件!");
            throw new IllegalStateException("no changes!");
        }

        for (var path : fileStatus.keySet()) {
            var pair = fileStatus.get(path);
            var file = pair.getLeft();
            var state = pair.getRight();

            if (state == FileModifyStatus.DELETE) {
                LOGGER.info("[D]{}", path);
                deleteFiles.add(path);
            }
            if (state == FileModifyStatus.ADD) {
                LOGGER.info("[A]{}", path);
                addFiles.put(path, file);
            }
            if (state == FileModifyStatus.UPDATE) {
                LOGGER.info("[U]{}", path);
                addFiles.put(path, file);
            }
        }

        LOGGER.info("完成!");

        return new ModernVersionInfo(id, time, addFiles, deleteFiles);
    }

    public ModernVersionInfo createInstallDummyVersion(){
        var map = new HashMap<String, FileData>();

        for (var rid : this.sources.keySet()) {
            var repo = this.sources.get(rid);
            for (var file : repo.collect()) {
                var resPath = repo.pathOf(file);

                map.put(resPath, new FileData(rid, resPath));
            }
        }

        return new ModernVersionInfo("_install", 0, map, new HashSet<>());
    }

    @Deprecated
    public VersionInfo generate(String version) {
        long last = System.currentTimeMillis();

        System.out.println("> 分析文件差异");
        var fileStatus = checkFiles();
        var command = new VersionInfo(version, System.currentTimeMillis());

        var deleteFiles = new HashSet<String>();
        var addFiles = new HashMap<String, File>();

        System.out.printf("检查完成，源内共 %s 个文件%n", fileStatus.size());

        System.out.println("> 检查文件状态");
        for (var path : fileStatus.keySet()) {
            var pair = fileStatus.get(path);
            var file = pair.getLeft();
            var state = pair.getRight();

            if (state == FileModifyStatus.DELETE) {
                System.out.println("[D]" + path);
                deleteFiles.add(path);
            }
            if (state == FileModifyStatus.ADD) {
                System.out.println("[A]" + path);
                addFiles.put(path, file.file(this));
            }
            if (state == FileModifyStatus.UPDATE) {
                System.out.println("[U]" + path);
                addFiles.put(path, file.file(this));
            }
        }

        if (fileStatus.values().stream().allMatch((p) -> p.getRight() == FileModifyStatus.NONE)) {
            throw new IllegalStateException("no changes!");
        }

        System.out.println("> 压缩资源包");
        if (!addFiles.isEmpty()) {
            var file = new File(FilePath.packs() + "/" + UUID.randomUUID() + ".zip");

            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            System.out.println("压缩包文件生成于: " + file.getAbsolutePath());

            PatchFile.zip(file, addFiles);
            command.addOperation(new PatchOperation(Collections.singleton(file.getName())));

            System.out.printf("压缩完成，生成 %d 个文件%n", addFiles.size());
        } else {
            System.out.println("没有文件需要添加或更改，资源包生成跳过");
        }

        System.out.println("> 添加文件修正信息");
        if (!deleteFiles.isEmpty()) {
            command.addOperation(new DeleteOperation(deleteFiles));
        }

        System.out.println("> 重新构建安装资源包");
        createInstallerZip();

        System.out.printf("> 完成! (%d ms)%n", System.currentTimeMillis() - last);

        return command;
    }

    @Deprecated
    public void createInstallerZip() {
        var zip = new File(FilePath.packs() + "/installer.zip");
        var addFiles = new HashMap<String, File>();

        for (var repo : this.getSources()) {
            for (var file : repo.collect()) {
                addFiles.put(repo.pathOf(file), file);
            }
        }

        PatchFile.zip(zip, addFiles);
    }

    public Map<String, Repository> getRegisteredSources() {
        return sources;
    }
}

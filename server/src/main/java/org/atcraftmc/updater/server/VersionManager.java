package org.atcraftmc.updater.server;

import com.google.gson.JsonParser;
import org.atcraftmc.updater.FilePath;
import org.atcraftmc.updater.command.VersionInfo;
import org.atcraftmc.updater.data.FileData;
import org.atcraftmc.updater.data.ModernVersionInfo;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class VersionManager {
    private final Map<String, VersionInfo> legacyVersions = new HashMap<>();
    private final Map<String, ModernVersionInfo> versions = new HashMap<>();

    public void load0() {
        this.legacyVersions.clear();

        var folder = new File(FilePath.versions() + "/info");
        var files = folder.listFiles();

        if (files == null) {
            return;
        }

        for (var file : files) {
            try (var i = new FileInputStream(file)) {
                var version = new VersionInfo(JsonParser.parseReader(new InputStreamReader(i)).getAsJsonObject());
                this.legacyVersions.put(version.getVersion(), version);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void load() {
        this.versions.clear();

        var folder = new File(FilePath.versions() + "/data");

        var files = folder.listFiles();

        if (files == null) {
            return;
        }

        for (var file : files) {
            try (var i = new FileInputStream(file)) {
                var version = new ModernVersionInfo(JsonParser.parseReader(new InputStreamReader(i)).getAsJsonObject());
                this.versions.put(version.getVersion(), version);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public boolean hasVersion(String version) {
        return this.versions.containsKey(version);
    }

    public void registerVersion(ModernVersionInfo version) {
        this.versions.put(version.getVersion(), version);

        var f = new File(FilePath.versions() + "/data/" + version.getVersion() + ".json");

        f.getParentFile().mkdirs();

        try (var o = new FileOutputStream(f)) {
            o.write(version.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void createVersion(VersionInfo version) {
        this.legacyVersions.put(version.getVersion(), version);

        var f = new File(FilePath.versions() + "/info/" + version.getVersion() + ".json");

        f.getParentFile().mkdirs();

        try (var o = new FileOutputStream(f)) {
            o.write(version.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, ModernVersionInfo> getVersions() {
        return versions;
    }

    public VersionInfo getVersion(String version) {
        return this.legacyVersions.get(version);
    }


    public List<ModernVersionInfo> getSortedVersions() {
        return this.versions.values().stream().sorted(Comparator.comparingLong(ModernVersionInfo::getTimestamp)).toList();
    }

    public ModernVersionInfo mergeFrom(long timeStamp) {
        var add = new HashMap<String, FileData>();
        var remove = new HashSet<String>();

        var data = getSortedVersions();
        var latest = data.get(data.size() - 1);

        if (data.get(0).getTimestamp() > timeStamp) { //version too old
            return ofInstall().copyOfInformation(latest);
        }

        for (var v : data) {
            if (v.getTimestamp() <= timeStamp) {
                continue;
            }

            add.putAll(v.getAddList());
            remove.addAll(v.getRemoveList());
        }



        return new ModernVersionInfo(latest.getVersion(), latest.getTimestamp(), add, remove);
    }

    public ModernVersionInfo ofInstall() {
        return this.versions.get("_install");
    }
}

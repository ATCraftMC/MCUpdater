package org.atcraftmc.updater.server;

import com.google.gson.JsonParser;
import org.atcraftmc.updater.FilePath;
import org.atcraftmc.updater.command.VersionInfo;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class VersionManager {
    private final Map<String, VersionInfo> versions = new HashMap<>();

    public void load() {
        this.versions.clear();

        var folder = new File(FilePath.versions() + "/info");
        var files = folder.listFiles();

        if (files == null) {
            return;
        }

        for (var file : files) {
            try (var i = new FileInputStream(file)) {
                var version = new VersionInfo(JsonParser.parseReader(new InputStreamReader(i)).getAsJsonObject());
                this.versions.put(version.getVersion(), version);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public boolean hasVersion(String version) {
        return this.versions.containsKey(version);
    }

    public void createVersion(VersionInfo version) {
        this.versions.put(version.getVersion(), version);

        var f = new File(FilePath.versions() + "/info/" + version.getVersion() + ".json");

        f.getParentFile().mkdirs();

        try (var o = new FileOutputStream(f)) {
            o.write(version.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, VersionInfo> getVersions() {
        return versions;
    }

    public VersionInfo getVersion(String version) {
        return this.versions.get(version);
    }
}

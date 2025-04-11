package org.atcraftmc.updater.data;

import java.io.File;

public record FileData(String repo, String path) {
    static FileData decode(String value) {
        return new FileData(value.split("://")[0], value.split("://")[1]);
    }

    public File file(FileManager fm) {
        return new File(fm.getRegisteredSources().get(this.repo).getBase() + this.path);
    }

    @Override
    public String toString() {
        return repo + "://" + this.path;
    }
}

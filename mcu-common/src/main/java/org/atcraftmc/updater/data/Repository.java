package org.atcraftmc.updater.data;


import org.atcraftmc.updater.data.diff.GitIgnoreFilter;

import java.io.File;
import java.util.Collection;

public final class Repository {
    private final File base;
    private final GitIgnoreFilter gitIgnoreFilter;

    public Repository(File base, GitIgnoreFilter gitIgnoreFilter) {
        this.base = base;
        this.gitIgnoreFilter = gitIgnoreFilter;
    }

    public Collection<File> collect() {
        return this.gitIgnoreFilter.listByFolder(this.base);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Repository s2)) {
            return false;
        }

        return s2.base.getAbsolutePath().equals(this.base.getAbsolutePath());
    }

    public File getBase() {
        return base;
    }

    public GitIgnoreFilter getGitIgnoreFilter() {
        return gitIgnoreFilter;
    }

    @Override
    public int hashCode() {
        return this.base.getAbsolutePath().hashCode();
    }

    public String pathOf(File file) {
        return file.getAbsolutePath().substring(this.base.getAbsolutePath().length());
    }
}
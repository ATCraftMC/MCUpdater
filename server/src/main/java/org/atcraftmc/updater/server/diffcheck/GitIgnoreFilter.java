package org.atcraftmc.updater.server.diffcheck;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

public class GitIgnoreFilter {
    private final String[] patterns;

    public GitIgnoreFilter(String... patterns) {
        this.patterns = patterns;
    }

    public List<File> listByFolder(File folder) {
        List<File> ignoredFiles = new ArrayList<>();
        filter(folder.getAbsolutePath(), folder, ignoredFiles);
        return ignoredFiles;
    }

    private boolean match(String basePath, File file) {
        var path = file.getAbsolutePath().substring(basePath.length()).replace("\\", "/");

        for (var pattern : this.patterns) {
            if (pattern.isBlank() || pattern.isEmpty()) {
                continue;
            }

            try {
                if (path.matches(pattern) || path.startsWith(pattern)) {
                    return true;
                }
            } catch (PatternSyntaxException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    private void filter(String base, File folder, List<File> target) {
        var files = folder.listFiles();

        if (files == null) {
            return;
        }

        for (var file : files) {
            if (file.isDirectory()) {
                filter(base, file, target);
            } else {
                if (match(base, file)) {
                    continue;
                }

                target.add(file);
            }
        }
    }
}
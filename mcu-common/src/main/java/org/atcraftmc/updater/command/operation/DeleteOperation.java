package org.atcraftmc.updater.command.operation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.atcraftmc.updater.FilePath;
import org.atcraftmc.updater.command.UpdateOperationListener;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class DeleteOperation extends Operation {
    private final Set<String> files;

    public DeleteOperation(JsonElement json) {
        this.files = json.getAsJsonArray().asList().stream().map(JsonElement::getAsString).collect(Collectors.toSet());
    }

    public DeleteOperation(String base, Set<File> files) {
        super(base);

        this.files = new HashSet<>();

        for (var file : files) {
            this.files.add(file.getAbsolutePath().substring(base.length()));
        }
    }

    public DeleteOperation(Set<String> paths) {
        this.files = paths;
    }

    @Override
    public void accept(UpdateOperationListener updateOperationListener) {
        var i = 0;

        for (var file : this.files) {
            var f = new File(FilePath.runtime() + file);

            if (!f.exists()) {
                continue;
            }

            updateOperationListener.setCommentMessage("正在删除废弃文件 - " + file);
            updateOperationListener.setProgress(i / this.files.size() * 100);

            f.delete();

            i++;
        }
    }

    @Override
    public JsonElement json() {
        var dom = new JsonArray();

        for (var file : this.files) {
            dom.add(file);
        }

        return dom;
    }
}

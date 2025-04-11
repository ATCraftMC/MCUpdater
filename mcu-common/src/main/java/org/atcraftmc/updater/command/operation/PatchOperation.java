package org.atcraftmc.updater.command.operation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.atcraftmc.updater.FilePath;
import org.atcraftmc.updater.MultiThreadDownloader;
import org.atcraftmc.updater.PatchFile;
import org.atcraftmc.updater.command.UpdateOperationListener;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


public final class PatchOperation extends Operation {
    private final Set<String> packs;

    public PatchOperation(Set<String> packs) {
        this.packs = packs;
    }

    public PatchOperation(JsonElement json) {
        this.packs = json.getAsJsonArray().asList().stream().map(JsonElement::getAsString).collect(Collectors.toSet());
    }

    @Override
    public void accept(UpdateOperationListener listener) {
        for (var id : this.packs) {
            var url = FilePath.server() + "/patch?file=" + id;
            var cache = new File(FilePath.updater() + "/cache/" + id + ".zip");

            cache.getParentFile().mkdirs();
            try {
                cache.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            var prefix = "正在下载更新资源包: " + id;

            listener.setCommentMessage(prefix);

            try {
                MultiThreadDownloader.download(url, cache.getAbsolutePath(), 1, (d, a) -> {
                    var dm = d / 1048576f;
                    var am = a / 1048576f;

                    listener.setCommentMessage(prefix + " - " + dm + " / " + am + "MiB");
                    listener.setProgress((int) (dm/am*100));
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            listener.setCommentMessage("正在解压更新资源包: " + id);
            PatchFile.unzip(cache, FilePath.runtime());
        }
    }

    @Override
    public JsonElement json() {
        var dom = new JsonArray();

        for (var pack : this.packs) {
            dom.add(pack);
        }

        return dom;
    }
}

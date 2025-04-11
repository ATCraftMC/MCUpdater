package org.atcraftmc.updater.server.http;

import org.atcraftmc.updater.FilePath;

import java.io.File;
import java.io.FileInputStream;

public final class VersionHistoryProvider implements HTTPHandler {

    @Override
    public void handle(HttpHandlerContext ctx) {
        var id = ctx.getParam("version");
        var f = new File(FilePath.versions() + "/" + id + ".txt");

        ctx.setResponseCode(200);
        ctx.contentType(ContentType.JSON);

        var json = ctx.createJsonReturn();
        json.addProperty("version", id);

        if (!f.exists() || f.length() == 0) {
            json.addProperty("text", "没有版本信息 :(");
            return;
        }



        try (var in = new FileInputStream(f)) {
            json.addProperty("text", new String(in.readAllBytes()));
        } catch (Exception e) {
            json.addProperty("text", "没有版本信息 :(");
        }
    }
}

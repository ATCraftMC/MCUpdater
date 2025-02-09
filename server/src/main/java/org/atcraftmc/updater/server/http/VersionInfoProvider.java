package org.atcraftmc.updater.server.http;

import org.atcraftmc.updater.command.VersionInfo;
import org.atcraftmc.updater.server.VersionManager;

import java.util.Comparator;
import java.util.Objects;

public final class VersionInfoProvider implements HTTPHandler {
    private final VersionManager versionManager;

    public VersionInfoProvider(VersionManager versionManager) {
        this.versionManager = versionManager;
    }

    @Override
    public void handle(HttpHandlerContext ctx) {
        var id = ctx.getParam("version");

        ctx.setResponseCode(200);
        ctx.contentType(ContentType.JSON);
        var json = ctx.createJsonArrayReturn();

        if (Objects.equals(id, "_latest")) {
            json.add(this.versionManager.getVersions()
                             .values()
                             .stream()
                             .sorted(Comparator.comparingLong(VersionInfo::getTimeStamp))
                             .toList()
                             .getLast()
                             .json());
            return;
        }

        if (Objects.equals(id, "_query")) {
            var time = Long.parseLong(ctx.getParam("time"));

            this.versionManager.getVersions()
                    .values()
                    .stream()
                    .filter((v) -> v.getTimeStamp() > time)
                    .sorted(Comparator.comparingLong(VersionInfo::getTimeStamp))
                    .map(VersionInfo::json)
                    .forEach(json::add);
            return;
        }

        if (Objects.equals(id, "_all")) {
            this.versionManager.getVersions()
                    .values()
                    .stream()
                    .sorted(Comparator.comparingLong(VersionInfo::getTimeStamp))
                    .map(VersionInfo::json)
                    .forEach(json::add);

            return;
        }


        if (!this.versionManager.hasVersion(id)) {
            ctx.error(404, "File not found");
            return;
        }

        json.add(this.versionManager.getVersion(id).json());
    }
}

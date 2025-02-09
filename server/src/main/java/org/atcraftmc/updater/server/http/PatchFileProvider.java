package org.atcraftmc.updater.server.http;

import com.sun.net.httpserver.HttpExchange;
import org.atcraftmc.updater.FilePath;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PatchFileProvider implements HTTPHandler {
    private byte[] latest = null;
    private String latestHash = null;

    @Override
    public void handle(HttpHandlerContext ctx) throws IOException {
        var id = ctx.getParam("file");
        var f = new File(FilePath.packs() + "/" + id);

        if (!f.exists() || f.length() == 0) {
            ctx.error(404, "File not found");
            return;
        }

        if (!Objects.equals(this.latestHash, id)) {
            try (var fis = new FileInputStream(f)) {
                this.latest = fis.readAllBytes();
                this.latestHash = id;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        var exchange = ctx.getExchange();

        var rangeHeader = exchange.getRequestHeaders().getFirst("Range");
        if (rangeHeader == null) {
            sendFullFile(exchange);
        } else {
            // 处理 Range 请求
            handleRangeRequest(exchange, rangeHeader);
        }
    }

    private void sendFullFile(HttpExchange exchange) throws IOException {
        // 返回整个文件
        exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
        exchange.sendResponseHeaders(200, latest.length); // 发送完整文件长度
        OutputStream os = exchange.getResponseBody();
        os.write(this.latest);
        os.close();
    }

    private void handleRangeRequest(HttpExchange exchange, String rangeHeader) throws IOException {
        // 解析 Range 头
        Pattern pattern = Pattern.compile("bytes=(\\d+)-(\\d+)?");
        Matcher matcher = pattern.matcher(rangeHeader);
        if (!matcher.matches()) {
            sendError(exchange, 400, "Invalid Range header");
            return;
        }

        long startByte = Long.parseLong(matcher.group(1));
        long endByte = matcher.group(2) != null ? Long.parseLong(matcher.group(2)) : -1;

        long fileSize = latest.length;

        if (endByte == -1) {
            endByte = fileSize - 1;
        }

        if (endByte > fileSize || startByte > endByte) {
            sendError(exchange, 416, "Requested Range Not Satisfiable");
            return;
        }

        var contentLength = endByte - startByte;

        exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
        exchange.getResponseHeaders().set("Content-Range", "bytes " + startByte + "-" + endByte + "/" + fileSize);
        exchange.sendResponseHeaders(206, contentLength); // 206 Partial Content

        // 返回内存中的文件部分
        OutputStream os = exchange.getResponseBody();
        os.write(latest, (int) startByte, (int) contentLength); // 从latest数组中返回指定部分
        os.close();
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        byte[] response = message.getBytes();
        exchange.sendResponseHeaders(statusCode, response.length);
        OutputStream os = exchange.getResponseBody();
        os.write(response);
        os.close();
    }
}

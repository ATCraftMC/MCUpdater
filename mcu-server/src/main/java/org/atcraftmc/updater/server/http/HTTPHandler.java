package org.atcraftmc.updater.server.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public interface HTTPHandler extends HttpHandler {
    @Override
    default void handle(HttpExchange exchange) throws IOException {
        var ctx = new HttpHandlerContext(exchange);

        this.handle(ctx);

        var data = ctx.getData();

        try {
            if (data == null) {
                exchange.sendResponseHeaders(502, 0);
                OutputStream os = exchange.getResponseBody();
                os.write("ERR_EMPTY_RESPONSE".getBytes(StandardCharsets.UTF_8));
                os.close();
                return;
            }
            exchange.getResponseHeaders().add("Content-Type", ctx.contentType().toString());
            //exchange.getResponseHeaders().add("Content-Length", String.valueOf(data.length));

            exchange.sendResponseHeaders(ctx.getResponseCode(), data.length);

            OutputStream os = exchange.getResponseBody();
            os.write(data);
            os.close();
        } catch (IOException ignored) {

        }
    }

    void handle(HttpHandlerContext ctx) throws IOException;
}

package org.atcraftmc.updater.server.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class HttpHandlerContext {
    private final Map<String, String> params = new HashMap<>();
    private final HttpExchange exchange;

    private ContentType contentType = ContentType.TEXT;
    private int responseCode = 200;
    private JsonElement json = null;
    private byte[] data = null;

    public HttpHandlerContext(HttpExchange exchange) {
        this.exchange = exchange;
        if (exchange.getRequestURI().getQuery() == null) {
            return;
        }
        for (String s : exchange.getRequestURI().getQuery().split("&")) {
            this.params.put(s.split("=")[0], s.split("=")[1]);
        }
    }

    public HttpExchange getExchange() {
        return this.exchange;
    }

    public byte[] getData() {
        if (this.data == null) {
            return this.json.toString().getBytes(StandardCharsets.UTF_8);
        }
        return this.data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public JsonObject createJsonReturn() {
        this.contentType = ContentType.JSON;
        this.json = new JsonObject();
        return this.json.getAsJsonObject();
    }

    public JsonArray createJsonArrayReturn() {
        this.contentType = ContentType.JSON;
        this.json = new JsonArray();
        return this.json.getAsJsonArray();
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public void error(int code, String err) {
        this.setResponseCode(code);
    }

    public Map<String, String> getParams() {
        return params;
    }

    public String getParam(String id) {
        return this.params.get(id);
    }

    public JsonElement getJson() {
        return json;
    }

    public ContentType contentType() {
        return contentType;
    }

    public void contentType(ContentType contentType) {
        this.contentType = contentType;
    }
}

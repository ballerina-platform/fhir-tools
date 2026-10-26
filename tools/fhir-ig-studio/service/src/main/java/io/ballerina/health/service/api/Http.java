/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.health.service.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thin request/response helpers over {@code com.sun.net.httpserver}.
 */
public final class Http {

    public static final String JSON = "application/json; charset=utf-8";

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private final String corsAllowOrigin;

    public Http(String corsAllowOrigin) {
        this.corsAllowOrigin = corsAllowOrigin;
    }

    public void writeJson(HttpExchange exchange, int status, Object body) throws IOException {
        writeBytes(exchange, status, JSON, GSON.toJson(body).getBytes(StandardCharsets.UTF_8));
    }

    public void writeError(HttpExchange exchange, int status, String message) throws IOException {
        writeJson(exchange, status, Map.of("error", message == null ? "Unexpected error" : message));
    }

    public void writeBytes(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        applyCommonHeaders(exchange, contentType);
        exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        if (body.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        } else {
            exchange.close();
        }
    }

    /**
     * Starts a streamed response; the caller writes the body and closes the returned stream.
     */
    public OutputStream beginStream(HttpExchange exchange, int status, String contentType,
                                    Map<String, String> extraHeaders) throws IOException {
        applyCommonHeaders(exchange, contentType);
        extraHeaders.forEach((key, value) -> exchange.getResponseHeaders().set(key, value));
        exchange.sendResponseHeaders(status, 0);
        return exchange.getResponseBody();
    }

    private void applyCommonHeaders(HttpExchange exchange, String contentType) {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", corsAllowOrigin);
        // X-Client-Id travels on every request, so a cross-origin UI has to be allowed to send it.
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, X-Client-Id");
        // A download is fetched rather than linked, so the name the service chose has to be readable.
        exchange.getResponseHeaders().set("Access-Control-Expose-Headers", "Content-Disposition");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
    }

    /** Answers a CORS preflight. Returns {@code true} when the request was a preflight and is now complete. */
    public boolean handlePreflight(HttpExchange exchange) throws IOException {
        if (!"OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            return false;
        }
        writeBytes(exchange, 204, JSON, new byte[0]);
        return true;
    }

    public <T> T readJson(HttpExchange exchange, Class<T> type) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (body.isBlank()) {
                return null;
            }
            try {
                return GSON.fromJson(body, type);
            } catch (JsonSyntaxException e) {
                throw new IllegalArgumentException("Request body is not valid JSON: " + e.getMessage());
            }
        }
    }

    public static Map<String, String> queryParams(HttpExchange exchange) {
        Map<String, String> params = new LinkedHashMap<>();
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isEmpty()) {
            return params;
        }
        for (String pair : query.split("&")) {
            int separator = pair.indexOf('=');
            if (separator < 0) {
                params.put(decode(pair), "");
            } else {
                params.put(decode(pair.substring(0, separator)), decode(pair.substring(separator + 1)));
            }
        }
        return params;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    public static int intParam(Map<String, String> params, String name, int defaultValue) {
        String raw = params.get(name);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean boolParam(Map<String, String> params, String name, boolean defaultValue) {
        String raw = params.get(name);
        return raw == null || raw.isBlank() ? defaultValue : Boolean.parseBoolean(raw.trim());
    }
}

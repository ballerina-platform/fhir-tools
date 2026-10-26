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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Serves the built UI bundle and falls back to {@code index.html} so client side routes resolve on reload.
 */
public class StaticHandler implements HttpHandler {

    private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
            Map.entry("html", "text/html; charset=utf-8"),
            Map.entry("js", "text/javascript; charset=utf-8"),
            Map.entry("mjs", "text/javascript; charset=utf-8"),
            Map.entry("css", "text/css; charset=utf-8"),
            Map.entry("json", "application/json; charset=utf-8"),
            Map.entry("svg", "image/svg+xml"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("ico", "image/x-icon"),
            Map.entry("woff", "font/woff"),
            Map.entry("woff2", "font/woff2"),
            Map.entry("ttf", "font/ttf"),
            Map.entry("map", "application/json; charset=utf-8"));

    private final Path uiDir;
    private final int apiPort;

    public StaticHandler(Path uiDir, int apiPort) {
        this.uiDir = uiDir.toAbsolutePath().normalize();
        this.apiPort = apiPort;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String requestPath = exchange.getRequestURI().getPath();
            Path index = uiDir.resolve("index.html");
            if (!Files.isRegularFile(index)) {
                writePlaceholder(exchange);
                return;
            }
            Path resolved = resolve(requestPath);
            if (resolved == null || !Files.isRegularFile(resolved)) {
                // unknown path: let the single page app router decide
                write(exchange, index);
                return;
            }
            write(exchange, resolved);
        } finally {
            exchange.close();
        }
    }

    private Path resolve(String requestPath) {
        String relative = requestPath.startsWith("/") ? requestPath.substring(1) : requestPath;
        if (relative.isEmpty()) {
            return uiDir.resolve("index.html");
        }
        Path resolved = uiDir.resolve(relative).normalize();
        return resolved.startsWith(uiDir) ? resolved : null;
    }

    private void write(HttpExchange exchange, Path file) throws IOException {
        byte[] body = Files.readAllBytes(file);
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
        exchange.getResponseHeaders().set("Content-Type",
                CONTENT_TYPES.getOrDefault(extension, "application/octet-stream"));
        boolean immutable = file.getParent() != null
                && "assets".equals(file.getParent().getFileName().toString());
        exchange.getResponseHeaders().set("Cache-Control",
                immutable ? "public, max-age=31536000, immutable" : "no-store");
        exchange.sendResponseHeaders(200, body.length);
        try (var out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    /** Shown when the service runs without a built UI, so the operator knows what to do next. */
    private void writePlaceholder(HttpExchange exchange) throws IOException {
        String html = """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8" />
                  <title>FHIR IG Studio service</title>
                  <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                           margin: 0; padding: 3rem; color: #1d1d20; background: #f7f7f9; }
                    main { max-width: 42rem; margin: 0 auto; background: #fff; padding: 2rem 2.5rem;
                           border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,.08); }
                    code { background: #f0f0f3; padding: .15rem .4rem; border-radius: 4px; }
                    pre { background: #f0f0f3; padding: 1rem; border-radius: 8px; overflow-x: auto; }
                  </style>
                </head>
                <body>
                  <main>
                    <h1>FHIR IG Studio service is running</h1>
                    <p>The REST API is available at <code>/api</code>, but no built UI bundle was found at
                       <code>%s</code>.</p>
                    <p>Build the UI and restart, or point the service at an existing build:</p>
                    <pre>cd ui
                npm install
                npm run build</pre>
                    <p>During UI development run <code>npm run dev</code> instead; the dev server proxies
                       <code>/api</code> to port %d.</p>
                    <p>Try the API directly: <a href="/api/meta">/api/meta</a></p>
                  </main>
                </body>
                </html>
                """.formatted(uiDir, apiPort);
        byte[] body = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, body.length);
        try (var out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}

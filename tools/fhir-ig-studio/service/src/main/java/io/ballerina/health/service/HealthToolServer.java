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

package io.ballerina.health.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.ballerina.health.service.api.ApiHandler;
import io.ballerina.health.service.api.Http;
import io.ballerina.health.service.api.StaticHandler;
import io.ballerina.health.service.job.JobManager;
import io.ballerina.health.service.registry.IgCatalogService;
import io.ballerina.health.service.registry.PackageWorkspaceService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.Map;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.logging.LogManager;

/**
 * Standalone HTTP server that exposes FHIR package and template generation as a service, and hosts the UI that
 * drives it.
 *
 * <p>Start with {@code java -jar health-service.jar}. Configuration is read from system properties or the
 * matching environment variables; see {@link ServiceConfig}.</p>
 */
public final class HealthToolServer {

    private static final String API_PATH = "/api";

    private HealthToolServer() {
    }

    public static void main(String[] args) throws IOException {
        // The codegen libraries reconfigure java.util.logging; keep their output off the server console.
        LogManager.getLogManager().reset();

        ServiceConfig config = ServiceConfig.load();
        Files.createDirectories(config.getSpecsRoot());
        Files.createDirectories(config.getJobsRoot());
        Files.createDirectories(config.getIgCacheDir());

        IgCatalogService igCatalog = new IgCatalogService();
        JobManager jobManager = new JobManager(config, igCatalog);
        Http http = new Http(config.getCorsAllowOrigin());
        String version = resolveVersion();

        HttpServer server = HttpServer.create(new InetSocketAddress(config.getHost(), config.getPort()), 0);
        PackageWorkspaceService workspaces = new PackageWorkspaceService(config, igCatalog);
        server.createContext(API_PATH,
                new ApiHandler(config, jobManager, igCatalog, workspaces, http, version));
        server.createContext("/healthz", exchange -> writeHealth(exchange, http, version));
        server.createContext("/", new StaticHandler(config.getUiDir(), config.getPort()));
        server.setExecutor(Executors.newFixedThreadPool(Math.max(4, config.getWorkerThreads() * 4)));

        // A working copy per browser per package means abandoned ones have to be reclaimed, or the disk only
        // grows. Swept at startup and daily after that.
        ScheduledExecutorService sweeper = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "workspace-sweeper");
            thread.setDaemon(true);
            return thread;
        });
        if (config.getWorkspaceTtlDays() > 0) {
            Duration keepFor = Duration.ofDays(config.getWorkspaceTtlDays());
            sweeper.scheduleAtFixedRate(() -> workspaces.sweepAbandoned(keepFor), 0, 1, TimeUnit.DAYS);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            sweeper.shutdownNow();
            jobManager.shutdown();
            server.stop(0);
        }));

        server.start();
        printBanner(config, version);
    }

    private static void writeHealth(HttpExchange exchange, Http http, String version) throws IOException {
        try {
            http.writeJson(exchange, 200, Map.of("status", "UP", "version", version));
        } finally {
            exchange.close();
        }
    }

    private static String resolveVersion() {
        String implementationVersion = HealthToolServer.class.getPackage().getImplementationVersion();
        return implementationVersion == null ? "dev" : implementationVersion;
    }

    private static void printBanner(ServiceConfig config, String version) {
        String host = "0.0.0.0".equals(config.getHost()) ? "localhost" : config.getHost();
        StringBuilder banner = new StringBuilder(System.lineSeparator());
        banner.append("FHIR codegen service ").append(version).append(System.lineSeparator());
        banner.append("  UI        http://").append(host).append(':').append(config.getPort()).append('/')
                .append(System.lineSeparator());
        banner.append("  API       http://").append(host).append(':').append(config.getPort()).append(API_PATH)
                .append("/meta").append(System.lineSeparator());
        // Only worth a line when something is wrong: without a built UI the address above serves nothing.
        if (!Files.isRegularFile(config.getUiDir().resolve("index.html"))) {
            banner.append("  [WARN] No UI found at ").append(config.getUiDir())
                    .append("; build it with 'npm run build' in ui/, or set fhir.tool.service.uiDir.")
                    .append(System.lineSeparator());
        }
        System.out.print(banner);
        System.out.flush();
    }
}

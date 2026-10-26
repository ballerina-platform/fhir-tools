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

package io.ballerina.health.service.worker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Turns generated sources into something someone can actually run, and says how.
 *
 * <p>What the codegen produces is a Ballerina project. Downloading it and finding out that using it needs a
 * Ballerina distribution installed first is a poor way to meet a tool, so the project is built here and the
 * executable travels in the archive beside its sources. A README at the root of the archive says how to run
 * that jar, how to rebuild after a change, and how to open the project in WSO2 Integrator.
 *
 * <p>Building is best-effort. A missing {@code bal} or a compile error is reported and the job still succeeds:
 * the sources are the substantive output, and withholding them because the convenience step failed would be
 * the wrong trade.
 */
final class FacadePackager {

    /** Where the Ballerina CLI is, when it is not simply on the PATH. */
    private static final String BAL_COMMAND_ENV = "BAL_COMMAND";

    private FacadePackager() {
    }

    /**
     * @param outputDir the job's output directory, which is also the root of the downloadable archive
     * @param input     what was asked for, for naming the guide in the README
     * @param out       the worker's stdout, which the server drains into the job log
     */
    static void packageFacade(Path outputDir, WorkerInput input, PrintStream out) {
        List<Path> projects = ballerinaProjects(outputDir);
        if (projects.isEmpty()) {
            // Minimal mode ships loose sources with no Ballerina.toml; there is no project to build.
            out.println("[INFO] No Ballerina project in the output, so nothing to build.");
            return;
        }
        Map<Path, String> built = new LinkedHashMap<>();
        for (Path project : projects) {
            String jar = build(project, out);
            if (jar != null) {
                built.put(project, jar);
            }
        }
        writeReadme(outputDir, projects, built, input, out);
    }

    /** A Ballerina project is a directory with a Ballerina.toml: the output root, or a child of it. */
    private static List<Path> ballerinaProjects(Path outputDir) {
        List<Path> projects = new ArrayList<>();
        if (Files.isRegularFile(outputDir.resolve("Ballerina.toml"))) {
            projects.add(outputDir);
            return projects;
        }
        try (Stream<Path> children = Files.list(outputDir)) {
            children.filter(Files::isDirectory)
                    // The scratch copy of the IG package is a duplicate of the module already inside the
                    // project; building it again would double the time for nothing anyone downloads it for.
                    .filter(child -> !child.getFileName().toString().startsWith("."))
                    .filter(child -> Files.isRegularFile(child.resolve("Ballerina.toml")))
                    .sorted()
                    .forEach(projects::add);
        } catch (IOException e) {
            // An unreadable output directory will have failed the generation already.
        }
        return projects;
    }

    /**
     * Runs {@code bal build} and returns the name of the jar it produced, or null when it could not.
     */
    private static String build(Path project, PrintStream out) {
        String bal = balCommand();
        out.println("[INFO] Building " + project.getFileName() + " with '" + bal + " build' ...");
        try {
            ProcessBuilder builder = new ProcessBuilder(bal, "build");
            builder.directory(project.toFile());
            builder.redirectErrorStream(true);
            Process process = builder.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.strip();
                    // The pull progress bar repaints the same line hundreds of times; it is noise in a log.
                    if (trimmed.isEmpty() || trimmed.contains("[==") || trimmed.endsWith("%")) {
                        continue;
                    }
                    out.println("[BUILD] " + trimmed);
                }
            }
            int exit = process.waitFor();
            if (exit != 0) {
                out.println("[WARN] '" + bal + " build' exited with " + exit
                        + ". The sources are complete; build them yourself once the errors above are dealt with.");
                return null;
            }
        } catch (IOException e) {
            out.println("[WARN] Could not run '" + bal + "': " + e.getMessage()
                    + ". Skipping the build -- install Ballerina to get a runnable jar in the download.");
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            out.println("[WARN] The build was interrupted.");
            return null;
        }
        String jar = jarIn(project);
        if (jar == null) {
            out.println("[WARN] The build reported success but produced no executable.");
        } else {
            out.println("[INFO] Built target/bin/" + jar);
            pruneBuildScratch(project, out);
        }
        return jar;
    }

    /**
     * Keeps {@code target/bin} and drops the rest of {@code target}.
     *
     * <p>A Ballerina build leaves a backup of the previous executable and a dependency cache beside the new one
     * -- together larger than the jar itself, and worth nothing to someone downloading a facade. Left in, they
     * roughly doubled the size of the archive.
     */
    private static void pruneBuildScratch(Path project, PrintStream out) {
        Path target = project.resolve("target");
        try (Stream<Path> children = Files.list(target)) {
            for (Path child : children.toList()) {
                if (child.getFileName().toString().equals("bin")) {
                    continue;
                }
                deleteRecursively(child);
            }
        } catch (IOException e) {
            out.println("[WARN] Could not tidy the build directory: " + e.getMessage());
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(path)) {
            for (Path entry : walk.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(entry);
            }
        }
    }

    private static String balCommand() {
        String configured = System.getenv(BAL_COMMAND_ENV);
        return configured == null || configured.isBlank() ? "bal" : configured.trim();
    }

    private static String jarIn(Path project) {
        Path bin = project.resolve("target").resolve("bin");
        if (!Files.isDirectory(bin)) {
            return null;
        }
        try (Stream<Path> files = Files.list(bin)) {
            return files.filter(f -> f.getFileName().toString().endsWith(".jar"))
                    .map(f -> f.getFileName().toString())
                    .findFirst().orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ readme

    private static void writeReadme(Path outputDir, List<Path> projects, Map<Path, String> built,
                                    WorkerInput input, PrintStream out) {
        Path project = projects.get(0);
        String jar = built.get(project);
        String template = readTemplate();
        if (template == null) {
            return;
        }
        Map<String, String> values = new LinkedHashMap<>();
        values.put("label", displayName(input));
        values.put("ig", igReference(input));
        values.put("project", outputDir.equals(project) ? "." : project.getFileName().toString());
        values.put("module", moduleIn(project));
        values.put("jar", jar == null ? packageName(project) + ".jar" : jar);
        values.put("distribution", distribution(project));
        values.put("builtNote", jar == null
                ? " — **not built**, see below" : "");
        String rendered = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            rendered = rendered.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        if (jar == null) {
            rendered = rendered.replace("## Run it",
                    "## Run it\n\n> This download has **no jar**: the build did not run or did not succeed. "
                            + "Follow \"Change it, then rebuild\" below first, then come back here.");
        }
        try {
            Files.writeString(outputDir.resolve("README.md"), rendered, StandardCharsets.UTF_8);
            out.println("[INFO] Wrote README.md describing how to run and modify the facade.");
        } catch (IOException e) {
            out.println("[WARN] Could not write README.md: " + e.getMessage());
        }
        // Names only, never paths: enough for the page to print a runnable command for this particular build.
        out.println(GenerationWorker.MARKER_FACADE + values.get("project") + "|" + (jar == null ? "" : jar));
    }

    private static String readTemplate() {
        try (InputStream in = FacadePackager.class.getResourceAsStream("/facade-readme.md")) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    /** The first module under modules/, which for an embedded guide is the guide's own types. */
    private static String moduleIn(Path project) {
        Path modules = project.resolve("modules");
        if (!Files.isDirectory(modules)) {
            return "";
        }
        try (Stream<Path> children = Files.list(modules)) {
            return children.filter(Files::isDirectory).map(p -> p.getFileName().toString())
                    .sorted().findFirst().orElse("");
        } catch (IOException e) {
            return "";
        }
    }

    private static String packageName(Path project) {
        String value = tomlValue(project, "name");
        return value == null ? "service" : value;
    }

    private static String distribution(Path project) {
        String value = tomlValue(project, "distribution");
        return value == null ? "Swan Lake" : value;
    }

    /** Reads one quoted top-level key out of Ballerina.toml, which is enough for a name and a version. */
    private static String tomlValue(Path project, String key) {
        try {
            for (String line : Files.readAllLines(project.resolve("Ballerina.toml"), StandardCharsets.UTF_8)) {
                String trimmed = line.strip();
                if (trimmed.startsWith(key) && trimmed.contains("=")) {
                    String value = trimmed.substring(trimmed.indexOf('=') + 1).strip();
                    return value.replace("\"", "").strip();
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    private static String displayName(WorkerInput input) {
        if (input.igName != null && !input.igName.isBlank()) {
            return input.igName;
        }
        return input.packageName == null || input.packageName.isBlank() ? "FHIR" : input.packageName;
    }

    private static String igReference(WorkerInput input) {
        if (input.igLabel != null && !input.igLabel.isBlank()) {
            return input.igLabel;
        }
        if (input.igName == null || input.igName.isBlank()) {
            return "the supplied FHIR definitions";
        }
        return input.igVersion == null || input.igVersion.isBlank()
                ? input.igName : input.igName + "@" + input.igVersion;
    }
}

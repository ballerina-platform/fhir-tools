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

import com.google.gson.Gson;
import io.ballerina.health.cmd.core.config.IgRegistryConfig;
import io.ballerina.health.cmd.core.utils.IgRegistryConfigLoader;
import io.ballerina.health.cmd.core.utils.SpecificationPathResolver;
import io.ballerina.health.cmd.handler.Handler;
import io.ballerina.health.cmd.handler.HandlerFactory;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entry point of the forked generation JVM.
 *
 * <p>Generation runs in its own process rather than inside the server for two reasons: the codegen tools keep
 * JVM-wide mutable state (for example the {@code DataTypesRegistry} singleton) that is never reset between
 * runs, and the CLI code paths reachable from the handlers can call {@code System.exit}. A dedicated process
 * gives every job a clean slate and keeps a failing job from taking the server down.</p>
 */
public final class GenerationWorker {

    /** Prefix for machine readable lines the server parses out of the worker output. */
    public static final String MARKER_PREFIX = "[SERVICE] ";
    public static final String MARKER_RESOLVED_SPEC = MARKER_PREFIX + "resolved-spec=";
    public static final String MARKER_MAPPED_PACKAGE = MARKER_PREFIX + "mapped-dependent-package=";
    public static final String MARKER_RESOLVED_IG = MARKER_PREFIX + "resolved-ig=";
    /** "&lt;project folder&gt;|&lt;jar name&gt;" -- what the download contains, so the page can say how to run it. */
    public static final String MARKER_FACADE = MARKER_PREFIX + "facade=";

    /** Exit code used when the generator completes without producing artifacts. */
    public static final int EXIT_GENERATION_INCOMPLETE = 2;

    private GenerationWorker() {
    }

    public static void main(String[] args) {
        PrintStream out = System.out;
        if (args.length < 1) {
            out.println("[ERROR] Usage: GenerationWorker <worker-input.json>");
            System.exit(1);
        }
        try {
            WorkerInput input = readInput(Path.of(args[0]));
            int exitCode = run(input, out);
            out.flush();
            System.exit(exitCode);
        } catch (Throwable e) {
            out.println("[ERROR] " + describe(e));
            for (StackTraceElement element : e.getStackTrace()) {
                out.println("\tat " + element);
            }
            out.flush();
            System.exit(1);
        }
    }

    private static WorkerInput readInput(Path inputFile) throws Exception {
        String json = Files.readString(inputFile, StandardCharsets.UTF_8);
        WorkerInput input = new Gson().fromJson(json, WorkerInput.class);
        if (input == null || input.mode == null || input.mode.isBlank()) {
            throw new IllegalArgumentException("Worker input is missing the generation mode");
        }
        return input;
    }

    /**
     * The resolver takes npm-style {@code <name>[@version]} references, so the service's separate igName/igVersion
     * fields are folded back into that grammar here. The service drives one IG per job; an absent IG name means
     * the job is resolving from a local spec path or the default core package instead.
     */
    private static List<String> igReferences(WorkerInput input) {
        if (input.igName == null || input.igName.isBlank()) {
            return List.of();
        }
        String name = input.igName.trim();
        return (input.igVersion == null || input.igVersion.isBlank())
                ? List.of(name) : List.of(name + "@" + input.igVersion.trim());
    }

    private static int run(WorkerInput input, PrintStream out) {
        createWorkingDirectory(Path.of(input.executionPath));

        IgRegistryConfig registryConfig = IgRegistryConfigLoader.load();
        out.println("[INFO] Resolving FHIR specification for " + input.mode + " generation ...");
        SpecificationPathResolver.ResolvedSpecification resolved;
        try {
            resolved = SpecificationPathResolver.resolve(new SpecificationPathResolver.ResolveRequest(
                    input.mode,
                    input.executionPath,
                    input.specPath,
                    igReferences(input),
                    input.registryUrl,
                    input.igCacheDir,
                    input.forceDownload,
                    true,
                    input.fhirVersion,
                    registryConfig,
                    out));
        } catch (Exception e) {
            out.println("[ERROR] Unable to resolve the FHIR specification: " + describe(e));
            return 1;
        }

        out.println(MARKER_RESOLVED_SPEC + resolved.specificationPath());
        // The service drives one IG per job, so the single-element case is the only one reported here.
        SpecificationPathResolver.ResolvedIg resolvedIg = resolved.resolvedIgs() == null
                || resolved.resolvedIgs().isEmpty() ? null : resolved.resolvedIgs().get(0);
        String resolvedIgName = resolvedIg == null ? null : resolvedIg.igName();
        String mappedDependentPackage = resolvedIg == null ? null : resolvedIg.mappedDependentPackage();
        if (resolvedIgName != null) {
            out.println(MARKER_RESOLVED_IG + resolvedIgName);
        }
        if (mappedDependentPackage != null) {
            out.println(MARKER_MAPPED_PACKAGE + mappedDependentPackage);
        }
        if ("template".equals(input.mode) && !input.explicitDependentPackage
                && mappedDependentPackage != null) {
            out.println("[INFO] IG " + resolvedIgName + " maps to published package "
                    + mappedDependentPackage
                    + ". Embedding IG resources in the template instead of importing it.");
        }

        String specificationPath = resolved.specificationPath().toString();
        Handler handler;
        try {
            out.println("[INFO] Parsing FHIR definitions from " + specificationPath + " ...");
            handler = HandlerFactory.createHandler("fhir", input.mode, out, specificationPath);
        } catch (Exception e) {
            out.println("[ERROR] Unable to initialise the " + input.mode + " generator: " + describe(e));
            return 1;
        }

        handler.setArgs(toArgsMap(input));
        out.println("[INFO] Generating artifacts into " + input.outputPath + " ...");
        boolean generated = handler.execute(specificationPath, input.outputPath);
        if (!generated) {
            out.println("[ERROR] The generator did not produce any artifacts.");
            return EXIT_GENERATION_INCOMPLETE;
        }
        out.println("[INFO] Generation completed successfully.");
        // Building here rather than leaving it to whoever downloads it: the archive then carries something
        // runnable, and the README beside it can name the jar it actually produced.
        FacadePackager.packageFacade(Path.of(input.outputPath), input, out);
        return 0;
    }

    /**
     * Builds the argument map understood by the CLI handlers. Keys and value types mirror
     * {@code FhirSubCmd#engageSubCommand}; handlers read some entries without a null check, so every key is set.
     */
    private static Map<String, Object> toArgsMap(WorkerInput input) {
        Map<String, Object> argsMap = new HashMap<>();
        argsMap.put("--package-name", blankToNull(input.packageName));
        argsMap.put("--org-name", blankToNull(input.orgName));
        argsMap.put("--package-version", blankToNull(input.packageVersion));
        argsMap.put("--included-profile", toArray(input.includedProfiles));
        argsMap.put("--excluded-profile", toArray(input.excludedProfiles));
        argsMap.put("--explicit-dependent-package", input.explicitDependentPackage);
        argsMap.put("--dependent-package", input.explicitDependentPackage
                ? blankToNull(input.dependentPackage) : null);
        argsMap.put("--ig-module-name", blankToNull(input.igModuleName));
        argsMap.put("--dependent-ig", toArray(input.dependentIgs));
        argsMap.put("--aggregate", input.aggregate);
        argsMap.put("--resources", blankToNull(input.resources));
        argsMap.put("--minimal", input.minimal);
        return argsMap;
    }

    private static String[] toArray(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.toArray(new String[0]);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static void createWorkingDirectory(Path path) {
        try {
            Files.createDirectories(path);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create the worker working directory " + path, e);
        }
    }

    private static String describe(Throwable e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getName() : message;
    }
}

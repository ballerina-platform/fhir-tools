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

package io.ballerina.health.service.job;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.ballerina.health.service.ServiceConfig;
import io.ballerina.health.service.registry.IgCatalogService;
import io.ballerina.health.service.util.PathUtils;
import io.ballerina.health.service.worker.GenerationWorker;
import io.ballerina.health.service.worker.WorkerInput;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Accepts generation requests, runs them in forked worker JVMs and keeps their state and logs available for the
 * UI to poll.
 */
public class JobManager {

    /** SLF4J prints this bootstrap notice when no logging provider is bound; it is noise in the job log. */
    private static final List<String> SUPPRESSED_LOG_PREFIXES = List.of(
            "SLF4J: No SLF4J providers were found",
            "SLF4J: Defaulting to no-operation",
            "SLF4J: See https://www.slf4j.org/codes.html#noProviders");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final ServiceConfig config;
    private final IgCatalogService igCatalog;
    private final ExecutorService executor;
    private final Map<String, Job> jobs = new ConcurrentHashMap<>();
    private final AtomicInteger jobCounter = new AtomicInteger();

    public JobManager(ServiceConfig config, IgCatalogService igCatalog) {
        this.config = config;
        this.igCatalog = igCatalog;
        this.executor = Executors.newFixedThreadPool(config.getWorkerThreads(), runnable -> {
            Thread thread = new Thread(runnable, "fhir-codegen-worker");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Validates and enqueues a request. The returned job is already persisted on disk and will start as soon as
     * a worker slot frees up.
     */
    public Job submit(GenerationRequest request) throws ValidationException {
        RequestValidator.validate(request);
        GenerationRequest.SpecSource source = request.getSource();

        String pinnedVersion = null;
        if ("registry".equals(source.getType())) {
            try {
                pinnedVersion = igCatalog.pinVersion(source.getIgName(), source.getIgVersion(),
                        source.getRegistryUrl());
            } catch (IgCatalogService.PackageNotFoundException e) {
                throw new ValidationException(e.getMessage());
            } catch (Exception e) {
                throw new ValidationException("Unable to resolve version for IG '" + source.getIgName()
                        + "': " + messageOf(e));
            }
            source.setIgVersion(pinnedVersion);
        }
        if ("upload".equals(source.getType())) {
            Path specDir = config.getSpecsRoot().resolve(source.getSpecId());
            if (!Files.isDirectory(specDir)) {
                throw new ValidationException("Unknown uploaded specification '" + source.getSpecId() + "'.");
            }
        }

        String id = nextJobId(request.getMode());
        Path jobDir = config.getJobsRoot().resolve(id);
        Job job = new Job(id, request, jobDir, buildLabel(request));
        try {
            Files.createDirectories(job.getOutputDir());
            writeJson(jobDir.resolve("request.json"), request);
        } catch (IOException e) {
            throw new ValidationException("Unable to prepare the job directory: " + e.getMessage());
        }
        jobs.put(id, job);
        executor.submit(() -> execute(job));
        return job;
    }

    public Optional<Job> find(String id) {
        return Optional.ofNullable(jobs.get(id));
    }

    /** Newest job first. */
    public List<Job> list() {
        List<Job> all = new ArrayList<>(jobs.values());
        all.sort(Comparator.comparing(Job::getCreatedAt).reversed());
        return all;
    }

    public boolean cancel(String id) {
        Job job = jobs.get(id);
        if (job == null || job.isTerminal()) {
            return false;
        }
        job.setStatus(JobStatus.CANCELLED);
        Process process = job.getProcess();
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
        job.appendLog("[INFO] Job cancelled by the user.");
        job.setCompletedAt(Instant.now());
        return true;
    }

    /**
     * Removes a job's generated files once they have been handed over and left behind, keeping the job itself.
     *
     * <p>Refuses to do anything until the archive has actually been downloaded. The files are the only copy: a
     * transfer that failed, or a page someone opened and left without downloading, must still be retryable.
     */
    public void collect(Job job) {
        if (!job.isDownloaded() || job.isCollected()) {
            return;
        }
        try {
            PathUtils.deleteRecursively(job.getOutputDir());
        } catch (IOException e) {
            job.appendLog("[WARN] Could not remove the generated files: " + e.getMessage());
            return;
        }
        job.setCollectedAt(Instant.now());
        job.appendLog("[INFO] Archive downloaded and the page closed; the generated files have been removed "
                + "from the server. Generate again if another copy is needed.");
        persistState(job);
    }

    public boolean delete(String id) throws IOException {
        Job job = jobs.remove(id);
        if (job == null) {
            return false;
        }
        Process process = job.getProcess();
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
        PathUtils.deleteRecursively(job.getJobDir());
        return true;
    }

    public void shutdown() {
        executor.shutdownNow();
        for (Job job : jobs.values()) {
            Process process = job.getProcess();
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private void execute(Job job) {
        if (job.getStatus() == JobStatus.CANCELLED) {
            return;
        }
        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        try {
            WorkerInput input = buildWorkerInput(job);
            Path inputFile = job.getJobDir().resolve("worker-input.json");
            writeJson(inputFile, input);

            Process process = startWorker(job, inputFile);
            job.setProcess(process);
            drainOutput(job, process);

            boolean finished = process.waitFor(config.getJobTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                fail(job, "Generation timed out after " + config.getJobTimeoutSeconds() + " seconds.");
                return;
            }
            if (job.getStatus() == JobStatus.CANCELLED) {
                return;
            }
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                job.setStatus(JobStatus.SUCCEEDED);
                job.completeProgress();
                job.setCompletedAt(Instant.now());
            } else if (exitCode == GenerationWorker.EXIT_GENERATION_INCOMPLETE) {
                fail(job, "The generator completed without producing artifacts. See the log for details.");
            } else {
                fail(job, "Generation failed (worker exit code " + exitCode + "). See the log for details.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(job, "Generation was interrupted.");
        } catch (Exception e) {
            fail(job, "Generation failed: " + messageOf(e));
        } finally {
            persistState(job);
        }
    }

    private void fail(Job job, String message) {
        // Redacted on the way in: a failure message is the most likely place a server path would escape.
        String safe = JobProgress.redact(message);
        job.appendLog("[ERROR] " + safe);
        job.setErrorMessage(safe);
        job.setStatus(JobStatus.FAILED);
        job.setCompletedAt(Instant.now());
    }

    private Process startWorker(Job job, Path inputFile) throws IOException {
        List<String> command = new ArrayList<>();
        command.add(javaExecutable());
        command.add("-cp");
        command.add(workerClasspath());
        command.add(GenerationWorker.class.getName());
        command.add(inputFile.toString());

        job.enterStage(JobProgress.stage("resolve"));
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(job.getJobDir().toFile());
        builder.redirectErrorStream(true);
        return builder.start();
    }

    /**
     * Copies worker output into the in-memory log buffer and the on-disk log file as it arrives, so the UI can
     * follow a long running generation.
     */
    private void drainOutput(Job job, Process process) throws IOException {
        Path logFile = job.getLogFile();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (isSuppressed(line)) {
                    continue;
                }
                // The raw line goes to disk for whoever runs the service; the reader gets the stage it
                // announces. Forwarding it as-is would hand out this server's directory layout.
                Files.writeString(logFile, line + System.lineSeparator(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                if (line.startsWith(GenerationWorker.MARKER_FACADE)) {
                    String[] parts = line.substring(GenerationWorker.MARKER_FACADE.length()).split("\\|", -1);
                    job.setFacade(parts.length > 0 ? parts[0] : null, parts.length > 1 ? parts[1] : null);
                    continue;
                }
                job.enterStage(JobProgress.stageFor(line));
                if (line.startsWith("[ERROR]") || line.startsWith("[WARN]")) {
                    // Something went wrong is always worth saying, with the paths taken out of it.
                    job.appendLog(line);
                }
            }
        }
    }

    private static boolean isSuppressed(String line) {
        for (String prefix : SUPPRESSED_LOG_PREFIXES) {
            if (line.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private WorkerInput buildWorkerInput(Job job) {
        GenerationRequest request = job.getRequest();
        GenerationRequest.SpecSource source = request.getSource();
        WorkerInput input = new WorkerInput();
        input.igLabel = job.getLabel();
        input.mode = request.getMode();
        input.outputPath = job.getOutputDir().toString();
        input.igCacheDir = config.getIgCacheDir().toString();
        input.forceDownload = source.isForceDownload();
        input.registryUrl = source.getRegistryUrl();

        switch (source.getType()) {
            case "upload" -> {
                input.executionPath = job.getJobDir().toString();
                input.specPath = config.getSpecsRoot().resolve(source.getSpecId()).toString();
            }
            case "default" -> {
                input.fhirVersion = source.getFhirVersion();
                // The resolver downloads the configured core IG under <executionPath>/spec/international; keying
                // the directory by FHIR version lets concurrent jobs share one extraction.
                input.executionPath = specCacheDir("core-" + source.getFhirVersion());
            }
            default -> {
                input.igName = source.getIgName();
                input.igVersion = source.getIgVersion();
                // The resolver extracts into <executionPath>/spec/<ig-name> without the version, so the version
                // has to be part of the parent directory to keep two versions of one IG from colliding.
                input.executionPath = specCacheDir(
                        PathUtils.slug(source.getIgName() + "-" + source.getIgVersion(), "ig"));
            }
        }

        input.packageName = request.getPackageName();
        input.orgName = request.getOrgName();
        input.packageVersion = request.getPackageVersion();
        input.dependentPackage = request.getDependentPackage();
        input.explicitDependentPackage = request.getDependentPackage() != null
                && !request.getDependentPackage().isBlank();
        input.igModuleName = request.getIgModuleName();
        input.dependentIgs = request.getDependentIgs();
        input.includedProfiles = request.getIncludedProfiles();
        input.excludedProfiles = request.getExcludedProfiles();
        input.aggregate = request.isAggregate();
        input.resources = request.getResources().isEmpty() ? null : String.join(",", request.getResources());
        input.minimal = request.isMinimal();
        return input;
    }

    private String specCacheDir(String key) {
        Path dir = config.getWorkspaceRoot().resolve("spec-cache").resolve(key);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return dir.toString();
    }

    /**
     * The worker runs the same code as the server, so reusing the server classpath keeps the two in step.
     *
     * <p>Entries are absolutised against the server working directory because the worker is started inside its
     * own job directory, where a relative classpath entry would no longer resolve. When the server itself was
     * started with {@code java -jar}, the classpath is the single service jar and its dependencies come from the
     * {@code Class-Path} manifest entry, which is resolved relative to the (now absolute) jar location.</p>
     */
    private static String workerClasspath() {
        String raw = System.getProperty("java.class.path", "");
        Path workingDirectory = Path.of(System.getProperty("user.dir", "."));
        List<String> entries = new ArrayList<>();
        for (String entry : raw.split(File.pathSeparator)) {
            if (entry.isBlank()) {
                continue;
            }
            Path path = Path.of(entry);
            entries.add((path.isAbsolute() ? path : workingDirectory.resolve(path)).normalize().toString());
        }
        return String.join(File.pathSeparator, entries);
    }

    private static String javaExecutable() {
        Path java = Path.of(System.getProperty("java.home"), "bin", "java");
        return Files.isExecutable(java) ? java.toString() : "java";
    }

    private String nextJobId(String mode) {
        String sequence = String.format(Locale.ROOT, "%04d", jobCounter.incrementAndGet());
        return mode + "-" + sequence + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String buildLabel(GenerationRequest request) {
        if (request.getLabel() != null && !request.getLabel().isBlank()) {
            return request.getLabel().trim();
        }
        GenerationRequest.SpecSource source = request.getSource();
        return switch (source.getType()) {
            case "upload" -> "Uploaded specification";
            case "default" -> "HL7 " + source.getFhirVersion().toUpperCase(Locale.ROOT) + " core";
            default -> source.getIgName() + "@" + source.getIgVersion();
        };
    }

    private void persistState(Job job) {
        try {
            Map<String, Object> state = new LinkedHashMap<>();
            state.put("id", job.getId());
            state.put("mode", String.valueOf(job.getMode()));
            state.put("label", String.valueOf(job.getLabel()));
            state.put("status", job.getStatus().name());
            state.put("stage", job.getStageKey());
            state.put("progress", job.getProgressPercent());
            state.put("createdAt", job.getCreatedAt().toString());
            state.put("completedAt", job.getCompletedAt() == null ? "" : job.getCompletedAt().toString());
            state.put("error", job.getErrorMessage() == null ? "" : job.getErrorMessage());
            state.put("downloadedAt", job.getDownloadedAt() == null ? "" : job.getDownloadedAt().toString());
            state.put("collectedAt", job.getCollectedAt() == null ? "" : job.getCollectedAt().toString());
            state.put("facadeProject", job.getFacadeProject() == null ? "" : job.getFacadeProject());
            state.put("facadeJar", job.getFacadeJar() == null ? "" : job.getFacadeJar());
            writeJson(job.getJobDir().resolve("job.json"), state);
        } catch (IOException ignored) {
            // job state is also held in memory; a failed snapshot must not fail the job
        }
    }

    private static void writeJson(Path target, Object value) throws IOException {
        Files.createDirectories(target.getParent());
        Files.writeString(target, GSON.toJson(value), StandardCharsets.UTF_8);
    }

    private static String messageOf(Throwable e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    /**
     * Raised when a submitted request cannot be turned into a runnable job.
     */
    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }
}

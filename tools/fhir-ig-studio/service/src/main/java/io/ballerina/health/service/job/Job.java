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

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * State of a single generation job. Log lines are held in memory for streaming and mirrored to
 * {@code logs.txt} inside the job directory.
 */
public class Job {

    private static final int MAX_RETAINED_LOG_LINES = 20_000;

    private final String id;
    private final String mode;
    private final String label;
    private final Instant createdAt;
    private final GenerationRequest request;
    private final Path jobDir;
    private final List<String> logLines = new ArrayList<>();

    private volatile JobStatus status = JobStatus.QUEUED;
    private volatile Instant startedAt;
    private volatile Instant completedAt;
    private volatile String errorMessage;
    private volatile Process process;
    private volatile int truncatedLogLines;
    /** The download's project folder and executable, for telling someone how to start what they just built. */
    private volatile String facadeProject;
    private volatile String facadeJar;

    /** Set when the archive has been streamed in full at least once. */
    /** The stage last reported to whoever is watching, and how far along it is. */
    private volatile String stageKey = "queued";
    private volatile int progressPercent;

    private volatile Instant downloadedAt;

    /**
     * Set once the generated files have been removed from the server.
     *
     * <p>A built facade is around 115 MB, nearly all of it the executable, and the service keeps every job it
     * has ever run, so the files cannot stay indefinitely. They go when two things are true: the archive has
     * been downloaded, and the person has moved on from the job page. Either alone is too eager -- a download
     * that arrives broken deserves a second try, and someone still on the page may not have downloaded yet.
     * The job itself stays; its logs are small and still worth reading.
     */
    private volatile Instant collectedAt;

    public Job(String id, GenerationRequest request, Path jobDir, String label) {
        this.id = id;
        this.request = request;
        this.mode = request.getMode();
        this.jobDir = jobDir;
        this.label = label;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getMode() {
        return mode;
    }

    public String getLabel() {
        return label;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public GenerationRequest getRequest() {
        return request;
    }

    public Path getJobDir() {
        return jobDir;
    }

    public Path getOutputDir() {
        return jobDir.resolve("output");
    }

    public Path getLogFile() {
        return jobDir.resolve("logs.txt");
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Process getProcess() {
        return process;
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    public boolean isTerminal() {
        return status == JobStatus.SUCCEEDED || status == JobStatus.FAILED || status == JobStatus.CANCELLED;
    }

    public String getFacadeProject() {
        return facadeProject;
    }

    public String getFacadeJar() {
        return facadeJar;
    }

    public void setFacade(String project, String jar) {
        this.facadeProject = project == null || project.isBlank() ? null : project;
        this.facadeJar = jar == null || jar.isBlank() ? null : jar;
    }

    public String getStageKey() {
        return stageKey;
    }

    public String getStageTitle() {
        return JobProgress.stage(stageKey).title();
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    /**
     * Moves to a stage, and records it in the visible log.
     *
     * <p>Never moves backwards: the generator repeats itself, and a bar that retreats reads as a fault.
     */
    public void enterStage(JobProgress.Stage stage) {
        if (stage == null || JobProgress.indexOf(stage.key()) <= JobProgress.indexOf(stageKey)) {
            return;
        }
        stageKey = stage.key();
        progressPercent = stage.percent();
        appendLog(stage.title());
    }

    /** Called when the job ends, so the bar finishes rather than stopping wherever it happened to be. */
    public void completeProgress() {
        stageKey = "done";
        progressPercent = 100;
    }

    public Instant getDownloadedAt() {
        return downloadedAt;
    }

    public void setDownloadedAt(Instant downloadedAt) {
        this.downloadedAt = downloadedAt;
    }

    public boolean isDownloaded() {
        return downloadedAt != null;
    }

    public Instant getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(Instant collectedAt) {
        this.collectedAt = collectedAt;
    }

    /** Whether the artifacts have been handed over and deleted. */
    public boolean isCollected() {
        return collectedAt != null;
    }

    /**
     * Adds a line to what the reader sees. Raw generator output does not come through here -- only stage titles
     * and messages the service itself writes -- and even those are stripped of anything path-shaped.
     */
    public void appendLog(String line) {
        synchronized (logLines) {
            logLines.add(JobProgress.redact(line));
            if (logLines.size() > MAX_RETAINED_LOG_LINES) {
                logLines.remove(0);
                truncatedLogLines++;
            }
        }
    }

    /**
     * Returns log lines starting at the absolute index {@code from}. Lines dropped by in-memory truncation are
     * skipped, so callers may observe a jump in the returned start index.
     */
    public LogSlice logsFrom(int from) {
        synchronized (logLines) {
            int start = Math.max(from - truncatedLogLines, 0);
            int absoluteStart = start + truncatedLogLines;
            if (start >= logLines.size()) {
                return new LogSlice(absoluteStart, truncatedLogLines + logLines.size(), List.of());
            }
            List<String> slice = List.copyOf(logLines.subList(start, logLines.size()));
            return new LogSlice(absoluteStart, truncatedLogLines + logLines.size(), slice);
        }
    }

    public List<String> allLogs() {
        synchronized (logLines) {
            return Collections.unmodifiableList(new ArrayList<>(logLines));
        }
    }

    /**
     * A window of log lines plus the absolute index a follow-up poll should resume from.
     */
    public record LogSlice(int from, int nextIndex, List<String> lines) {
    }
}

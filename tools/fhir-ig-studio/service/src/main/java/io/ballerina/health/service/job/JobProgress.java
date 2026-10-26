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

import java.util.List;
import java.util.regex.Pattern;

/**
 * Turns the generator's own output into something worth showing someone.
 *
 * <p>The raw log is a tool talking to its author: it names the directory a package was extracted to, the
 * directory it wrote into, and the temporary paths in between. None of that is the reader's business, and on a
 * hosted service it is somebody else's server's filesystem. So the log is not forwarded. Each line is matched
 * against the handful of stages a generation actually goes through, and it is the stage that is reported.
 *
 * <p>Stages only ever move forward. Generators are chatty and repeat themselves, and a progress bar that goes
 * backwards reads as a fault even when nothing is wrong.
 */
public final class JobProgress {

    /** One stage of a generation, and how far along it is when reached. */
    public record Stage(String key, String title, int percent) {
    }

    private static final List<Stage> STAGES = List.of(
            new Stage("queued", "Queued", 0),
            new Stage("resolve", "Resolving the implementation guide", 10),
            new Stage("parse", "Reading the FHIR definitions", 25),
            new Stage("generate", "Generating the service", 45),
            new Stage("compile", "Compiling the facade", 70),
            new Stage("package", "Preparing the download", 95),
            new Stage("done", "Ready", 100));

    /**
     * What each stage is recognised by. Matched against the raw line in order; the first hit wins.
     *
     * <p>Deliberately matched on wording the generator controls rather than on paths, so a change of directory
     * layout cannot quietly stop the progress bar from moving.
     */
    private static final List<String[]> MARKERS = List.of(
            new String[]{"Resolving FHIR specification", "resolve"},
            new String[]{"Downloading", "resolve"},
            new String[]{"Parsing FHIR definitions", "parse"},
            new String[]{"Generating artifacts", "generate"},
            new String[]{"Generation completed", "compile"},
            new String[]{"Building", "compile"},
            new String[]{"Compiling source", "compile"},
            new String[]{"Generating executable", "compile"},
            new String[]{"Built target", "package"},
            new String[]{"Wrote README", "package"});

    /** Anything that looks like an absolute path, on either kind of host. */
    private static final Pattern PATHS = Pattern.compile("(/[\\w.\\-~]+){2,}/?|[A-Za-z]:\\\\[^\\s\"]+");

    private JobProgress() {
    }

    public static List<Stage> stages() {
        return STAGES;
    }

    public static Stage stage(String key) {
        return STAGES.stream().filter(s -> s.key().equals(key)).findFirst().orElse(STAGES.get(0));
    }

    /** How far through the list a stage is, so progress can be kept monotonic. */
    public static int indexOf(String key) {
        for (int i = 0; i < STAGES.size(); i++) {
            if (STAGES.get(i).key().equals(key)) {
                return i;
            }
        }
        return 0;
    }

    /** The stage a raw line announces, or null when it announces nothing. */
    public static Stage stageFor(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        for (String[] marker : MARKERS) {
            if (line.contains(marker[0])) {
                return stage(marker[1]);
            }
        }
        return null;
    }

    /**
     * Replaces anything path-shaped with a placeholder.
     *
     * <p>A backstop rather than the main defence: the stages above are what gets reported, and this exists so
     * that a message which does reach the reader -- an error, most likely -- cannot carry a server path with it.
     */
    public static String redact(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        return PATHS.matcher(text).replaceAll("<path>");
    }
}

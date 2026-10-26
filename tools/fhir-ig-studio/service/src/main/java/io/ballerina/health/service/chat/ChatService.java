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

package io.ballerina.health.service.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ballerina.health.service.ServiceConfig;
import io.ballerina.health.service.registry.PackageWorkspaceService;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Answers questions about an open package, and drafts changes to it, using a model the user runs themselves.
 *
 * <p>The model is reached over the OpenAI-compatible chat API, which Ollama, LM Studio, llama.cpp and vLLM all
 * speak, so which one is running is a matter of configuration rather than code. Nothing leaves the machine
 * unless the configured address points off it.
 *
 * <p>It never edits anything. A change comes back as a proposal describing the element and the new values, and
 * is applied only when someone presses Apply -- which puts it through the same profiling-rule validation as an
 * edit made by hand. A model that misreads a profile therefore costs a glance, not a corrupted package.
 *
 * <p>What the model is told is deliberately small. A guide runs to hundreds of definitions and several
 * megabytes; what fits, and what nearly every question is actually about, is the definition currently open --
 * its identity, what it constrains, and its elements -- plus the names of its siblings so it can say what else
 * is in the package.
 */
public class ChatService {

    /** Nulls are kept, so a proposal's "before" always has every field and the diff can show an empty one. */
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    /** Elements sent for the open definition. Enough for any real question, short of filling the context. */
    private static final int MAX_ELEMENTS = 120;
    /** Sibling definitions listed by name, so "what else is here" is answerable without sending them. */
    private static final int MAX_SIBLINGS = 200;
    /** A ceiling on the reply. Generous for prose plus a proposal, short of letting a model run away. */
    private static final int MAX_REPLY_TOKENS = 900;
    /** Long enough for a first request to load several gigabytes of weights before it answers. */
    private static final Duration REPLY_TIMEOUT = Duration.ofMinutes(8);
    /** How many times the agent may look something up before it has to answer. */
    private static final int MAX_LOOKUPS = 3;
    /** The scope gate answers in one word; a little room for the model to wrap it in punctuation. */
    private static final int GATE_REPLY_TOKENS = 5;

    /**
     * The fenced block the model is asked to end with, lifted out before the reply is shown.
     *
     * <p>Matched on the fence rather than on balanced braces. A small model drops a closing brace often enough
     * that requiring one means the block is neither parsed nor hidden, and the reader is shown raw JSON with an
     * apology attached -- the worst of both.
     */
    private static final Pattern PROPOSAL_BLOCK =
            Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```");

    private static final String SYSTEM_PROMPT = """
            You are a FHIR profiling assistant built into a tool that edits FHIR implementation guides.

            You help with two things:
            1. Explaining the open package: what a profile constrains, what its elements mean, what an
               extension is for, how it differs from the base FHIR definition.
            2. Drafting changes to the open profile when asked.

            Answer briefly and concretely, and refer to elements by their full path. If the question is about a
            definition that is not the open one, say which definition it should be asked against.

            """ + ChatScope.RULES;

    /**
     * How to propose a change, and which of this tool's controls does what, as the last thing the model reads.
     *
     * <p>Said once at the top, behind a dozen kilobytes of element listing, a 7B model ignores it and writes
     * out prose instructions for editing the file by hand instead. Said again immediately before it answers, it
     * complies. The rules live here rather than in the opening prompt so there is one copy of them.
     */
    private static final String FORMAT_PROMPT = """
            Reminder, and this overrides any habit of explaining how to edit files by hand:

            You cannot change anything yourself, and you must never give step-by-step editing instructions.
            If the user asked for a change, reply with at most two sentences, then a fenced json block:

            ```json
            {"proposals":[{"path":"AllergyIntolerance.clinicalStatus","sliceName":null,
              "changes":{"min":1,"mustSupport":true},"reason":"why this change"}]}
            ```

            - "path" must be an element path that appears in the element list above, copied exactly.
            - "changes" may set exactly these and nothing else: min (number), max (string),
              mustSupport (boolean), short (string), types (array of type codes),
              bindingStrength (required|extensible|preferred|example), bindingValueSet (url).
            - Include only the fields you are changing.
            - FHIR profiling only narrows: cardinality may tighten but never widen, types may be removed from
              the base set but never added, a binding may only be strengthened. Do not propose otherwise.
            - If the user asked a question rather than for a change, answer in prose and emit no json block.

            To add an extension to an element, do not describe how -- propose it, with "kind":"extension":

            ```json
            {"proposals":[{"kind":"extension","path":"AllergyIntolerance.category",
              "sliceName":"categorySource","min":0,"max":"1","mustSupport":false,
              "extension":{"id":"allergy-category-source","title":"Allergy category source",
                "description":"Where the category came from","valueTypes":["CodeableConcept"],
                "required":false,"repeats":false},
              "reason":"why this extension"}]}
            ```

            - "path" is the element the extension hangs off, exactly as listed above.
            - "sliceName" names the slice: lower camel case, letters and digits only.
            - "extension" describes a new extension to author. Its "valueTypes" must come from:
              boolean, integer, decimal, string, uri, url, canonical, code, date, dateTime, instant, time,
              markdown, base64Binary, Coding, CodeableConcept, Quantity, Identifier, HumanName, Address,
              ContactPoint, Period, Range, Ratio, Attachment, Annotation, Reference.
            - To bind an extension the package already has, give "extensionUrl" with its url instead of
              "extension".

            If you need to see something before answering -- an element you were not shown, a slice that was
            just added, what extensions exist -- ask for it instead of guessing, with a fenced json block:

            ```json
            {"lookup":{"element":"AllergyIntolerance.category.extension","sliceName":"categorySource"}}
            ```
            ```json
            {"lookup":{"search":"category"}}
            ```
            ```json
            {"lookup":{"extensions":true}}
            ```

            Ask for one thing at a time. The answer comes back and you may then ask again or propose. Never
            put a lookup and a proposal in the same reply.

            When asked how to do something, answer with this tool's own controls, never with generic steps,
            other tools, or commands for editing files by hand:
            - Load a guide: the packages page searches the FHIR package registry, or "Package source" uploads
              your own definitions.
            - Constrain an element: click its row in the element table and edit it in place, or ask you (the
              Dev Bot) and apply what you propose with "Apply". Tag elements with "Edit with Dev Bot" or "@" in
              the chat.
            - Author new definitions: "New profile" and "New extension".
            - Keep the work: "Save as project"; "My packages" downloads the edited guide.
            - Generate a FHIR facade: the "Generate FHIR facade" button at the top of the package page. It
              turns the profiles into an executable FHIR facade: an API per resource with its search
              parameters, and a capability statement at /metadata. When the job finishes, "Download zip",
              unzip it and start it with "java -jar" on the jar it contains (Java 21 or newer). Its resource
              functions are stubs that answer 501 until implemented; the README in the zip explains how.

            Do not reply to or acknowledge this reminder. Answer the user's latest message. A short one --
            "yes", "do it", "go ahead" -- accepts the change you just offered: propose it now.
            """;

    private final ServiceConfig config;
    private final PackageWorkspaceService workspaces;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    public ChatService(ServiceConfig config, PackageWorkspaceService workspaces) {
        this.config = config;
        this.workspaces = workspaces;
    }

    /** Whether a model is actually reachable, so the UI can say so rather than failing at the first question. */
    public JsonObject status() {
        JsonObject status = new JsonObject();
        status.addProperty("configured", config.isChatConfigured());
        if (!config.isChatConfigured()) {
            // Not an error. Nothing is wrong, and the tool does everything else without it.
            status.addProperty("available", false);
            status.addProperty("detail", "The chat is not enabled on this service.");
            return status;
        }
        ModelProvider provider = ModelProvider.of(config.getModelProvider());
        status.addProperty("url", config.getModelUrl());
        status.addProperty("model", config.getModelName());
        status.addProperty("provider", config.getModelProvider());
        // Whether a key is set, never the key itself: enough to tell a missing one from a wrong one.
        status.addProperty("authenticated", !config.getModelApiKey().isBlank());

        String probe = provider.probeUrl(config.getModelUrl());
        if (probe == null) {
            // Nothing cheap to call. Saying "available" here would be a guess, so say what is configured and
            // let the first question be the test.
            status.addProperty("available", true);
            status.addProperty("detail", "Configured; reachability is shown by the first question.");
            return status;
        }
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(probe))
                    .timeout(Duration.ofSeconds(6)).GET();
            // Hosted APIs refuse an unauthenticated listing, which used to read as "no model is answering".
            provider.authenticate(request, config.getModelApiKey());
            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            boolean ok = response.statusCode() >= 200 && response.statusCode() < 300;
            status.addProperty("available", ok);
            if (!ok) {
                status.addProperty("detail", response.statusCode() == 401 || response.statusCode() == 403
                        ? "The model provider rejected the API key."
                        : "The model provider answered " + response.statusCode() + ".");
            }
        } catch (Exception e) {
            status.addProperty("available", false);
            status.addProperty("detail", remote() ? "Could not reach " + config.getModelUrl() + "."
                    : "No model server answered at " + config.getModelUrl() + ".");
        }
        return status;
    }

    /** Whether the configured model is somewhere other than this machine, which changes the advice given. */
    private boolean remote() {
        String url = config.getModelUrl() == null ? "" : config.getModelUrl();
        return !(url.contains("localhost") || url.contains("127.0.0.1") || url.contains("[::1]"));
    }

    /**
     * @param workspaceId the open package
     * @param fileName    the definition currently open, which is what questions are assumed to be about
     * @param history     the conversation so far, as {role, content} objects
     * @return the reply, and any changes it proposed
     */
    /**
     * @param workspaceId the open package
     * @param fileName    the definition currently open, which is what questions are assumed to be about
     * @param history     the conversation so far, as {role, content} objects
     * @param focus       elements the reader picked in the table
     * @param applied     changes already made in this conversation, so a follow-up has something to refer to
     * @return the reply, anything it proposed, and what it looked up on the way
     */
    public JsonObject ask(String workspaceId, String fileName, JsonArray history, JsonArray focus,
                          JsonArray applied) throws IOException, InterruptedException {
        if (!config.isChatConfigured()) {
            throw new IllegalStateException("The chat is not enabled on this service. Set "
                    + "FHIR_TOOL_SERVICE_MODEL_URL to a chat API to switch it on.");
        }
        List<JsonObject> turns = new ArrayList<>();
        for (JsonElement entry : history) {
            if (!entry.isJsonObject()) {
                continue;
            }
            JsonObject turn = entry.getAsJsonObject();
            String role = string(turn, "role");
            String content = string(turn, "content");
            if (content != null && ("user".equals(role) || "assistant".equals(role))) {
                ChatScope.checkMessage(content);
                turns.add(message(role, content));
            }
        }
        turns = ChatScope.recent(turns);

        // Held to scope before anything else is spent on it: a question about the weather never sees the
        // package, the tools, or the main prompt.
        if (outOfScope(turns)) {
            return refused(new JsonArray());
        }

        JsonArray messages = new JsonArray();
        messages.add(message("system", SYSTEM_PROMPT));
        messages.add(message("system", context(workspaceId, fileName, focus)));
        String focused = focused(workspaceId, fileName, focus, applied != null && !applied.isEmpty());
        if (!focused.isEmpty()) {
            messages.add(message("system", focused));
        }
        String done = alreadyApplied(applied);
        if (!done.isEmpty()) {
            messages.add(message("system", done));
        }

        // The rules go late -- said once at the top, behind the element listing, a small model ignores them --
        // but never last. A model answers the most recent turn, so leaving instructions there makes it respond
        // to the instructions instead of the question. Nor directly before the latest question when that
        // question answers the previous reply: "yes, do it" straight after the rules reads as agreeing to the
        // rules, and the model says "Understood" and proposes nothing. So they go before the last exchange.
        int rulesAt = turns.size() - 1;
        if (rulesAt > 0 && "assistant".equals(string(turns.get(rulesAt - 1), "role"))) {
            rulesAt--;
        }
        for (int i = 0; i < turns.size(); i++) {
            if (i == rulesAt) {
                messages.add(message("system", FORMAT_PROMPT));
            }
            messages.add(turns.get(i));
        }
        if (turns.isEmpty()) {
            messages.add(message("system", FORMAT_PROMPT));
        }

        JsonArray lookups = new JsonArray();
        String reply = "";
        for (int round = 0; round <= MAX_LOOKUPS; round++) {
            reply = complete(messages);
            String block = fencedBlock(reply, "lookup");
            if (block == null || round == MAX_LOOKUPS) {
                break;
            }
            // It asked to see something. Answer it and let it think again, rather than have it guess -- which
            // is how a follow-up about a slice ends up proposing a change to the element the slice hangs off.
            JsonObject request;
            try {
                JsonElement parsed = JsonParser.parseString(balance(block));
                request = parsed.isJsonObject() ? parsed.getAsJsonObject().getAsJsonObject("lookup") : null;
            } catch (Exception e) {
                request = null;
            }
            if (request == null) {
                // It asked for something unreadable. Treat the turn as its answer rather than looping.
                break;
            }
            String answer = lookup(workspaceId, fileName, request);
            lookups.add(describeLookup(request));
            messages.add(message("assistant", reply));
            messages.add(message("system", "Result of that lookup:\n" + answer
                    + "\nNow answer the question, or ask for one more thing."));
        }

        if (ChatScope.isRefusal(reply)) {
            // The model declined on its own. Whatever else it wrote alongside -- a proposal included -- goes.
            return refused(lookups);
        }
        JsonObject result = new JsonObject();
        JsonArray proposed = proposals(reply, workspaceId, fileName);
        result.add("proposals", proposed);
        result.add("lookups", lookups);
        String prose = withoutFencedBlocks(reply, !proposed.isEmpty()).strip();
        if (prose.isEmpty() && !proposed.isEmpty()) {
            // Proposals with no words -- usual after a bare "yes, do it". Say what they are: an empty bubble
            // reads as a failure, and the reply goes back as history, where it is the next turn's context.
            prose = describeProposals(proposed);
        }
        result.addProperty("reply", prose);
        return result;
    }

    /**
     * Names what was proposed, for a reply that carried proposals and nothing else. Only the elements: each
     * proposal's card already shows its changes and reason, so repeating them here says it twice.
     */
    static String describeProposals(JsonArray proposals) {
        List<String> targets = new ArrayList<>();
        for (JsonElement entry : proposals) {
            if (!entry.isJsonObject()) {
                continue;
            }
            JsonObject proposal = entry.getAsJsonObject();
            String path = orEmpty(string(proposal, "path"));
            targets.add("extension".equals(string(proposal, "kind"))
                    ? "a " + orEmpty(string(proposal, "sliceName")) + " extension on " + path : path);
        }
        return (targets.size() == 1 ? "Here is a change to " : "Here are changes to ")
                + String.join(", ", targets) + " to review.";
    }

    /**
     * Whether the latest question is outside the Dev Bot's scope, as a separate one-word call decides.
     *
     * <p>An answer the gate cannot be read from counts as in scope: the main prompt still carries the same rules,
     * and turning away a real question because the classifier mumbled would be the worse mistake.
     */
    private boolean outOfScope(List<JsonObject> turns) throws IOException, InterruptedException {
        if (turns.isEmpty() || !"user".equals(string(turns.get(turns.size() - 1), "role"))) {
            return false;
        }
        String question = string(turns.get(turns.size() - 1), "content");
        String previous = turns.size() > 1 ? string(turns.get(turns.size() - 2), "content") : null;
        JsonArray messages = new JsonArray();
        messages.add(message("system", ChatScope.GATE_PROMPT));
        messages.add(message("user", ChatScope.gateInput(previous, question)));
        return ChatScope.verdict(complete(messages, GATE_REPLY_TOKENS, 0.0)) == ChatScope.Verdict.OUT;
    }

    /** The answer to a question outside the Dev Bot's scope: always the same sentence, and nothing proposed. */
    private static JsonObject refused(JsonArray lookups) {
        JsonObject result = new JsonObject();
        result.addProperty("reply", ChatScope.REFUSAL);
        result.add("proposals", new JsonArray());
        result.add("lookups", lookups);
        result.addProperty("outOfScope", true);
        return result;
    }

    /** One call to the model. */
    private String complete(JsonArray messages) throws IOException, InterruptedException {
        // Bounded, because a small model asked for a change will otherwise keep writing: restating the profile,
        // inventing further elements, or repeating the proposal block until something stops it.
        return complete(messages, MAX_REPLY_TOKENS, 0.2);
    }

    private String complete(JsonArray messages, int maxTokens, double temperature)
            throws IOException, InterruptedException {
        ModelProvider provider = ModelProvider.of(config.getModelProvider());
        JsonObject body = provider.body(config.getModelName(), messages, maxTokens, temperature);

        HttpRequest.Builder request = HttpRequest
                .newBuilder(URI.create(provider.completionsUrl(config.getModelUrl())))
                .timeout(REPLY_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
        provider.authenticate(request, config.getModelApiKey());

        HttpResponse<String> response;
        try {
            response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw new IOException("The model did not answer within " + REPLY_TIMEOUT.toMinutes()
                    + " minutes. It is running but slow -- a smaller model, or a shorter question, will help.", e);
        } catch (IOException e) {
            throw new IOException("Could not reach the model at " + config.getModelUrl() + ". "
                    + (remote() ? "Check the address and that this machine can get out to it."
                                : "Start it, or point FHIR_TOOL_SERVICE_MODEL_URL somewhere else."), e);
        }
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new IOException("The model provider rejected the API key. Check "
                    + "FHIR_TOOL_SERVICE_MODEL_API_KEY.");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("The model returned " + response.statusCode() + ": "
                    + withoutKey(brief(response.body())));
        }
        try {
            return provider.replyOf(response.body());
        } catch (Exception e) {
            throw new IOException("The model's answer was not in the expected shape for provider '"
                    + config.getModelProvider() + "': " + withoutKey(brief(response.body())));
        }
    }

    // ------------------------------------------------------------------ lookups

    /**
     * Answers a lookup against the package as it stands right now.
     *
     * <p>Read-only by design. The agent may look at whatever it needs to, and still cannot change anything:
     * every change goes back as a proposal for someone to approve.
     */
    private String lookup(String workspaceId, String fileName, JsonObject request) {
        if (request == null) {
            return "(nothing was asked for)";
        }
        try {
            if (request.has("extensions")) {
                List<PackageWorkspaceService.Definition> all = workspaces.listDefinitions(workspaceId);
                StringBuilder text = new StringBuilder();
                for (PackageWorkspaceService.Definition definition : all) {
                    if ("Extension".equals(definition.resourceType())) {
                        text.append("- ").append(definition.url()).append(" — ")
                                .append(definition.title() == null ? definition.name() : definition.title())
                                .append(" — used on ").append(String.join(", ", definition.contexts()))
                                .append('\n');
                    }
                }
                return text.length() == 0 ? "This package defines no extensions." : text.toString();
            }
            List<Map<String, Object>> elements = workspaces.describeElements(workspaceId, fileName);
            String search = string(request, "search");
            if (search != null) {
                StringBuilder text = new StringBuilder();
                int found = 0;
                for (Map<String, Object> element : elements) {
                    if (str(element.get("path")).toLowerCase().contains(search.toLowerCase())) {
                        if (found++ >= 40) {
                            text.append("... (more matches; be more specific)\n");
                            break;
                        }
                        text.append(describeOne(element));
                    }
                }
                return found == 0 ? "No element path contains '" + search + "'." : text.toString();
            }
            String path = string(request, "element");
            if (path == null) {
                return "(a lookup needs 'element', 'search' or 'extensions')";
            }
            String sliceName = string(request, "sliceName");
            StringBuilder text = new StringBuilder();
            for (Map<String, Object> element : elements) {
                boolean samePath = path.equals(str(element.get("path")));
                boolean sameSlice = sliceName == null || sliceName.equals(str(element.get("sliceName")));
                if (samePath && sameSlice) {
                    text.append(describeOne(element));
                }
            }
            return text.length() == 0 ? "There is no element '" + path + "'"
                    + (sliceName == null ? "" : " with slice '" + sliceName + "'") + "." : text.toString();
        } catch (Exception e) {
            return "(that could not be looked up)";
        }
    }

    /** One element, spelled out the way a proposal needs it. */
    private static String describeOne(Map<String, Object> element) {
        Map<String, Object> current = asMap(element.get("current"));
        Map<String, Object> limits = asMap(element.get("limits"));
        StringBuilder text = new StringBuilder();
        text.append("path: ").append(str(element.get("path")));
        String slice = str(element.get("sliceName"));
        if (!slice.isEmpty()) {
            text.append("\n  sliceName: ").append(slice);
        }
        text.append("\n  currently: ").append(str(current.get("min"))).append("..")
                .append(str(current.get("max")))
                .append(", types ").append(list(current.get("types")))
                .append(", mustSupport ").append(Boolean.TRUE.equals(current.get("mustSupport")))
                .append(", binding ").append(str(current.get("bindingStrength")))
                .append("\n  the base allows: ").append(str(limits.get("minMin"))).append("..")
                .append(str(limits.get("maxMax")))
                .append('\n');
        return text.toString();
    }

    private static JsonObject describeLookup(JsonObject request) {
        JsonObject described = new JsonObject();
        if (request.has("extensions")) {
            described.addProperty("what", "the package's extensions");
        } else if (string(request, "search") != null) {
            described.addProperty("what", "elements matching \"" + string(request, "search") + "\"");
        } else {
            String slice = string(request, "sliceName");
            described.addProperty("what", string(request, "element")
                    + (slice == null ? "" : ":" + slice));
        }
        return described;
    }

    /** What has already been done in this conversation, so "make it mandatory" has something to point at. */
    private static String alreadyApplied(JsonArray applied) {
        if (applied == null || applied.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder(
                "Changes already applied in this conversation. A follow-up such as \"make it mandatory\" or "
                        + "\"make that must-support\" almost certainly refers to the most recent of these, not to "
                        + "the element it hangs off. Look it up before proposing anything.\n");
        for (JsonElement entry : applied) {
            if (entry.isJsonObject()) {
                text.append("- ").append(string(entry.getAsJsonObject(), "summary")).append('\n');
            }
        }
        return text.toString();
    }

    // ------------------------------------------------------------------ context

    /** What the model is told about the package, kept to what a question is plausibly about. */
    private String context(String workspaceId, String fileName, JsonArray focus) throws IOException {
        PackageWorkspaceService.Workspace workspace = workspaces.describe(workspaceId);
        StringBuilder text = new StringBuilder();
        text.append("Open package: ").append(workspace.packageName()).append('@').append(workspace.version())
                .append(" (FHIR ").append(workspace.fhirVersion()).append(", ")
                .append(workspace.definitionCount()).append(" definitions)\n");
        if (workspace.title() != null) {
            text.append("Title: ").append(workspace.title()).append('\n');
        }

        List<PackageWorkspaceService.Definition> definitions = workspaces.listDefinitions(workspaceId);
        if (fileName == null || fileName.isBlank()) {
            text.append("\nNo definition is open. Ask the reader to select one before answering about it.\n");
        } else {
            for (PackageWorkspaceService.Definition definition : definitions) {
                if (definition.file().equals(fileName)) {
                    text.append("\nOpen definition: ").append(definition.name())
                            .append(" (").append(definition.resourceType()).append(", ")
                            .append(definition.kind()).append(")\n")
                            .append("URL: ").append(definition.url()).append('\n')
                            .append("Derived from: ").append(definition.baseDefinition()).append('\n');
                    if (definition.description() != null && !definition.description().isBlank()) {
                        text.append("Description: ").append(brief(definition.description())).append('\n');
                    }
                    break;
                }
            }
            text.append("\nElements of the open definition (path | card | types | flags | binding):\n");
            appendElements(text, workspaceId, fileName, focus);
        }

        List<PackageWorkspaceService.Definition> extensions = definitions.stream()
                .filter(d -> "Extension".equals(d.resourceType())).toList();
        if (!extensions.isEmpty()) {
            text.append("\nExtensions this package already defines (url — title — where it may be used):\n");
            int shown = 0;
            for (PackageWorkspaceService.Definition extension : extensions) {
                if (shown++ >= MAX_SIBLINGS) {
                    break;
                }
                text.append("- ").append(extension.url()).append(" — ")
                        .append(extension.title() == null ? extension.name() : extension.title())
                        .append(" — ").append(String.join(", ", extension.contexts())).append('\n');
            }
        }

        text.append("\nOther definitions in this package (name — resource type):\n");
        int listed = 0;
        for (PackageWorkspaceService.Definition definition : definitions) {
            if (definition.file().equals(fileName) || listed++ >= MAX_SIBLINGS) {
                continue;
            }
            text.append("- ").append(definition.name()).append(" — ").append(definition.resourceType())
                    .append('\n');
        }
        return text.toString();
    }

    private void appendElements(StringBuilder text, String workspaceId, String fileName, JsonArray focus) {
        try {
            List<Map<String, Object>> all = workspaces.describeElements(workspaceId, fileName);
            Set<String> picked = new LinkedHashSet<>();
            if (focus != null) {
                for (JsonElement entry : focus) {
                    if (entry.isJsonObject()) {
                        picked.add(orEmpty(string(entry.getAsJsonObject(), "path")));
                    }
                }
            }
            // Selected elements first, then the ones this guide constrains, then the rest. An expanded profile
            // runs past the cap, and an element that falls off the end may as well not exist: the model is told
            // to copy a path from this list, so it substitutes the nearest one it can see -- which for a deep
            // selection means quietly proposing a change to its parent.
            List<Map<String, Object>> elements = new ArrayList<>();
            all.stream().filter(e -> picked.contains(str(e.get("path")))).forEach(elements::add);
            all.stream().filter(e -> !picked.contains(str(e.get("path"))))
                    .filter(e -> Boolean.TRUE.equals(e.get("constrained"))).forEach(elements::add);
            all.stream().filter(e -> !picked.contains(str(e.get("path"))))
                    .filter(e -> !Boolean.TRUE.equals(e.get("constrained"))).forEach(elements::add);
            int count = 0;
            for (Map<String, Object> element : elements) {
                if (count++ >= MAX_ELEMENTS) {
                    text.append("... (").append(elements.size() - MAX_ELEMENTS)
                            .append(" further base elements, unchanged by this guide)\n");
                    break;
                }
                Map<String, Object> current = asMap(element.get("current"));
                text.append(str(element.get("path")));
                String slice = str(element.get("sliceName"));
                if (!slice.isEmpty()) {
                    text.append(':').append(slice);
                }
                String binding = str(current.get("bindingStrength"));
                text.append(" | ").append(str(current.get("min"))).append("..").append(str(current.get("max")))
                        .append(" | ").append(list(current.get("types")))
                        .append(" | ").append(Boolean.TRUE.equals(current.get("mustSupport")) ? "MS" : "")
                        .append(Boolean.TRUE.equals(element.get("constrained")) ? " constrained" : "")
                        .append(" | ").append(binding.isEmpty() ? ""
                                : binding + " " + str(current.get("bindingValueSet")))
                        .append('\n');
            }
        } catch (Exception e) {
            text.append("(elements unavailable)\n");
        }
    }

    /**
     * The elements the reader has pointed at, spelled out in full.
     *
     * <p>Naming an element in prose leaves the model to find it in a list of a hundred and more, and it does
     * not reliably succeed -- it drifts to whichever element the surrounding text made prominent. Elements
     * picked in the table are therefore repeated on their own, with their current values, and the model is told
     * plainly that these are the ones being asked about.
     */
    private String focused(String workspaceId, String fileName, JsonArray focus, boolean anythingApplied) {
        if (focus == null || focus.isEmpty() || fileName == null) {
            return "";
        }
        List<Map<String, Object>> elements;
        try {
            elements = workspaces.describeElements(workspaceId, fileName);
        } catch (Exception e) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (JsonElement entry : focus) {
            if (!entry.isJsonObject()) {
                continue;
            }
            JsonObject picked = entry.getAsJsonObject();
            Map<String, Object> element =
                    elementAt(elements, string(picked, "path"), string(picked, "sliceName"));
            if (element == null) {
                continue;
            }
            Map<String, Object> current = asMap(element.get("current"));
            Map<String, Object> limits = asMap(element.get("limits"));
            text.append("- ").append(str(element.get("path")));
            String slice = str(element.get("sliceName"));
            if (!slice.isEmpty()) {
                text.append(':').append(slice);
            }
            text.append("\n    currently: ").append(str(current.get("min"))).append("..")
                    .append(str(current.get("max")))
                    .append(", types ").append(list(current.get("types")))
                    .append(", mustSupport ").append(Boolean.TRUE.equals(current.get("mustSupport")))
                    .append(", binding ").append(str(current.get("bindingStrength")))
                    .append("\n    the base allows at most: ").append(str(limits.get("minMin"))).append("..")
                    .append(str(limits.get("maxMax")))
                    .append(", types ").append(list(limits.get("allowedTypes")))
                    .append('\n');
        }
        if (text.length() == 0) {
            return "";
        }
        String rule = anythingApplied
                // Once something has been created in this conversation, a follow-up usually means that thing,
                // not the element it hangs off. Stated absolutely, the selection overrides the far better
                // clue -- and "make it mandatory" lands on the parent.
                ? "The reader has selected these elements in the table. Copy paths exactly and never invent "
                        + "one, but note that a follow-up may well refer to something already applied above "
                        + "rather than to the selection itself. When it is not obvious which is meant, look it "
                        + "up before proposing anything.\n\n"
                : "The reader has selected these elements in the table and is asking about them. Any change "
                        + "they ask for applies to these, and to no others. Copy their paths exactly -- do not "
                        + "substitute a parent, a child, or a similarly named element, however tempting. If "
                        + "what they ask cannot be done to these elements, say so instead of proposing it "
                        + "elsewhere.\n\n";
        return rule + text;
    }

    // ------------------------------------------------------------------ proposals

    /**
     * Lifts the proposal block out of the reply and checks each one against the definition's real elements.
     *
     * <p>A path the model invented is dropped here rather than offered: a proposal that cannot be applied is
     * worse than no proposal, because it looks like one. Whether the change itself is legal is decided by the
     * validator when it is applied, which is the only thing that actually knows.
     */
    private JsonArray proposals(String reply, String workspaceId, String fileName) {
        JsonArray accepted = new JsonArray();
        String block = proposalBlock(reply);
        if (block == null) {
            return accepted;
        }
        List<Map<String, Object>> known;
        try {
            known = workspaces.describeElements(workspaceId, fileName);
        } catch (Exception e) {
            return accepted;
        }
        try {
            for (JsonElement entry : proposalList(block)) {
                JsonObject proposal = entry.getAsJsonObject();
                String kind = string(proposal, "kind") == null ? "constrain" : string(proposal, "kind");
                if ("extension".equals(kind)) {
                    // An extension hangs off an element, so only the element has to exist; the slice is new by
                    // definition, and whether the extension itself is legal is the validator's call on apply.
                    if (elementAt(known, string(proposal, "path"), null) == null) {
                        continue;
                    }
                    if (!proposal.has("extension") && string(proposal, "extensionUrl") == null) {
                        continue;
                    }
                    proposal.addProperty("kind", "extension");
                    proposal.addProperty("file", fileName);
                    if (string(proposal, "sliceName") == null) {
                        // A slice has to be named. Taking it from the extension's own id keeps the two
                        // recognisably the same thing, which a placeholder would not.
                        String id = proposal.has("extension")
                                ? string(proposal.getAsJsonObject("extension"), "id")
                                : lastSegment(string(proposal, "extensionUrl"));
                        proposal.addProperty("sliceName", sliceNameFrom(id));
                    }
                    accepted.add(proposal);
                    continue;
                }
                Map<String, Object> element =
                        elementAt(known, string(proposal, "path"), string(proposal, "sliceName"));
                if (element == null) {
                    continue;
                }
                liftLooseChanges(proposal);
                if (!proposal.has("changes")) {
                    continue;
                }
                proposal.addProperty("kind", "constrain");
                proposal.addProperty("file", fileName);
                // Written back split, whichever way the model expressed it, so the editor gets what it needs.
                String slice = slicePart(string(proposal, "path"), string(proposal, "sliceName"));
                proposal.addProperty("path", pathPart(string(proposal, "path")));
                if (slice == null) {
                    proposal.remove("sliceName");
                } else {
                    proposal.addProperty("sliceName", slice);
                }
                // The values as they stand, so the reader is shown a change rather than only a destination.
                proposal.add("current", GSON.toJsonTree(element.get("current")));
                accepted.add(proposal);
            }
        } catch (Exception e) {
            // A malformed block is treated as no proposal; the prose still stands on its own.
        }
        return accepted;
    }

    /**
     * Finds an element by path and slice.
     *
     * <p>A slice is written two ways: as a separate {@code sliceName}, and as {@code path:sliceName} in one
     * string -- which is how FHIR itself writes it, and so what a model reaches for. Both are accepted, because
     * rejecting the second means silently dropping a proposal that was perfectly well expressed.
     */
    private static Map<String, Object> elementAt(List<Map<String, Object>> elements, String path,
                                                 String sliceName) {
        if (path == null) {
            return null;
        }
        String wantedPath = path;
        String wantedSlice = orEmpty(sliceName);
        int colon = path.indexOf(':');
        if (colon > 0) {
            wantedPath = path.substring(0, colon);
            if (wantedSlice.isEmpty()) {
                wantedSlice = path.substring(colon + 1);
            }
        }
        for (Map<String, Object> element : elements) {
            if (wantedPath.equals(str(element.get("path")))
                    && wantedSlice.equals(str(element.get("sliceName")))) {
                return element;
            }
        }
        return null;
    }

    /** The fields a change may set, which is also what is recognised when written loose. */
    private static final List<String> CHANGE_FIELDS = List.of(
            "min", "max", "mustSupport", "short", "types", "bindingStrength", "bindingValueSet");

    /**
     * Moves change fields written at the top level into "changes", where they belong.
     *
     * <p>Shown the nested shape, a small model half the time writes {@code {"path":..., "max":"1"}} instead of
     * {@code {"path":..., "changes":{"max":"1"}}}. It means the same thing and there is nothing else those keys
     * could be, so it is read rather than discarded.
     */
    private static void liftLooseChanges(JsonObject proposal) {
        if (proposal.has("changes") && proposal.get("changes").isJsonObject()) {
            return;
        }
        JsonObject changes = new JsonObject();
        for (String field : CHANGE_FIELDS) {
            if (proposal.has(field) && !proposal.get(field).isJsonNull()) {
                changes.add(field, proposal.get(field));
            }
        }
        if (changes.size() > 0) {
            proposal.add("changes", changes);
        }
    }

    /** "allergy-category-source" -> "allergyCategorySource", a slice name FHIR will accept. */
    private static String sliceNameFrom(String id) {
        if (id == null || id.isBlank()) {
            return "extension";
        }
        String[] parts = id.trim().split("[^A-Za-z0-9]+");
        StringBuilder name = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            name.append(name.length() == 0
                    ? Character.toLowerCase(part.charAt(0)) : Character.toUpperCase(part.charAt(0)));
            name.append(part.substring(1));
        }
        return name.length() == 0 ? "extension" : name.toString();
    }

    /** The path without its slice suffix, for writing back a proposal the editor can apply. */
    private static String pathPart(String path) {
        int colon = path == null ? -1 : path.indexOf(':');
        return colon > 0 ? path.substring(0, colon) : path;
    }

    /** The slice named in a "path:slice" string, or the one given separately. */
    private static String slicePart(String path, String sliceName) {
        if (sliceName != null && !sliceName.isBlank()) {
            return sliceName;
        }
        int colon = path == null ? -1 : path.indexOf(':');
        return colon > 0 ? path.substring(colon + 1) : null;
    }

    /** The first fenced block carrying the given key, or null when there is none. */
    private static String fencedBlock(String reply, String key) {
        Matcher matcher = PROPOSAL_BLOCK.matcher(reply == null ? "" : reply);
        while (matcher.find()) {
            String body = matcher.group(1);
            if (body != null && body.contains("\"" + key + "\"")) {
                return body;
            }
        }
        return null;
    }

    /**
     * The proposal array inside a block, whatever shape the model reached for.
     *
     * <p>Asked for {@code {"proposals":[...]}} a small model will variously produce a bare array, a single
     * object, or the right thing under the wrong key. The content is what matters and it is unambiguous, so
     * each of those is accepted rather than thrown away over its wrapper.
     */
    private static JsonArray proposalList(String block) {
        JsonElement parsed = JsonParser.parseString(balance(block));
        if (parsed.isJsonArray()) {
            return parsed.getAsJsonArray();
        }
        if (!parsed.isJsonObject()) {
            return new JsonArray();
        }
        JsonObject object = parsed.getAsJsonObject();
        for (String key : List.of("proposals", "proposal", "changes_proposals")) {
            if (object.has(key) && object.get(key).isJsonArray()) {
                return object.getAsJsonArray(key);
            }
            if (object.has(key) && object.get(key).isJsonObject()) {
                JsonArray single = new JsonArray();
                single.add(object.getAsJsonObject(key));
                return single;
            }
        }
        // A lone proposal, written without the wrapper at all.
        if (object.has("path")) {
            JsonArray single = new JsonArray();
            single.add(object);
            return single;
        }
        return new JsonArray();
    }

    /** A block is a proposal block when it carries an element path, however it is wrapped. */
    private static String proposalBlock(String reply) {
        Matcher matcher = PROPOSAL_BLOCK.matcher(reply == null ? "" : reply);
        while (matcher.find()) {
            String body = matcher.group(1);
            if (body != null && body.contains("\"path\"") && !body.contains("\"lookup\"")) {
                return body;
            }
        }
        return null;
    }

    /**
     * Closes brackets the model forgot.
     *
     * <p>Small models truncate JSON: an array closes and the object around it does not. The content is
     * otherwise sound, and throwing the whole proposal away over a missing brace would be a poor trade, so the
     * openers still outstanding are closed in the order they were opened. Anything wrong beyond that still
     * fails to parse and is discarded.
     */
    private static String balance(String json) {
        Deque<Character> open = new ArrayDeque<>();
        boolean inString = false;
        boolean escaped = false;
        for (char c : json.toCharArray()) {
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\' && inString) {
                escaped = true;
            } else if (c == '"') {
                inString = !inString;
            } else if (!inString && (c == '{' || c == '[')) {
                open.push(c);
            } else if (!inString && (c == '}' || c == ']') && !open.isEmpty()) {
                open.pop();
            }
        }
        StringBuilder repaired = new StringBuilder(json.strip());
        if (inString) {
            repaired.append('"');
        }
        while (!open.isEmpty()) {
            repaired.append(open.pop() == '{' ? '}' : ']');
        }
        return repaired.toString();
    }

    /**
     * Removes the blocks that were understood, and leaves any that were not.
     *
     * <p>A lookup is plumbing and a parsed proposal is drawn as a card, so neither belongs in the prose. A
     * block that could not be read is a different matter: stripping it too left the reader with a sentence
     * ending in "propose the following change:" and nothing after it, which reads as the tool being broken.
     */
    private static String withoutFencedBlocks(String reply, boolean proposalsParsed) {
        if (reply == null) {
            return "";
        }
        StringBuilder kept = new StringBuilder();
        Matcher matcher = PROPOSAL_BLOCK.matcher(reply);
        int from = 0;
        while (matcher.find()) {
            String body = matcher.group(1);
            boolean understood = body != null
                    && (body.contains("\"lookup\"") || (proposalsParsed && body.contains("\"path\"")));
            kept.append(reply, from, matcher.start());
            if (!understood) {
                kept.append(matcher.group());
            }
            from = matcher.end();
        }
        kept.append(reply.substring(from));
        return kept.toString();
    }

    // ------------------------------------------------------------------ plumbing

    private static JsonObject message(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content);
        return message;
    }

    private static String replyOf(String body) throws IOException {
        try {
            JsonArray choices = JsonParser.parseString(body).getAsJsonObject().getAsJsonArray("choices");
            return choices.get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString();
        } catch (Exception e) {
            throw new IOException("The model's answer was not in the expected shape: " + brief(body));
        }
    }

    private static String trimSlash(String url) {
        String value = url == null ? "" : url.trim();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String lastSegment(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        return url.substring(url.lastIndexOf('/') + 1);
    }

    /**
     * Removes the API key from anything on its way to the reader.
     *
     * <p>Some providers echo the offending request back in an error, and a 400 is exactly when that text gets
     * shown. The key is configuration, not something to put in a browser.
     */
    private String withoutKey(String text) {
        String key = config.getModelApiKey();
        if (text == null || key == null || key.isBlank()) {
            return text;
        }
        return text.replace(key, "<api key>");
    }

    private static String brief(String text) {
        if (text == null) {
            return "";
        }
        String single = text.replaceAll("\\s+", " ").strip();
        return single.length() > 400 ? single.substring(0, 400) + "..." : single;
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String string(JsonObject json, String member) {
        if (json == null || !json.has(member) || json.get(member).isJsonNull()) {
            return null;
        }
        try {
            return json.get(member).getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Map.of();
    }

    private static String list(Object value) {
        if (!(value instanceof List<?> values)) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        values.forEach(entry -> parts.add(String.valueOf(entry)));
        return String.join(",", parts);
    }
}

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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Locale;

/**
 * The handful of ways a chat API differs, so pointing this tool at a different one is configuration.
 *
 * <p>Most providers speak the OpenAI chat API -- Ollama, LM Studio, llama.cpp, vLLM, Groq, Together,
 * OpenRouter and OpenAI itself -- so that is the default and covers a local model and a hosted one alike.
 * Two things still vary and cannot be papered over: how the key is presented, and, for Anthropic, the shape of
 * the request and the reply.
 */
public enum ModelProvider {

    /** The OpenAI chat API, and everything that imitates it. */
    OPENAI,
    /** Azure OpenAI: the same body, a different header, and the deployment already in the URL. */
    AZURE,
    /** Anthropic: its own endpoint, its own envelope, and the system prompt lifted out of the messages. */
    ANTHROPIC;

    public static ModelProvider of(String name) {
        if (name == null || name.isBlank()) {
            return OPENAI;
        }
        return switch (name.trim().toLowerCase(Locale.ROOT)) {
            case "anthropic", "claude" -> ANTHROPIC;
            case "azure", "azure-openai", "azureopenai" -> AZURE;
            default -> OPENAI;
        };
    }

    /** Where a completion is asked for. Azure carries its deployment and api-version in the configured URL. */
    public String completionsUrl(String baseUrl) {
        String base = trimSlash(baseUrl);
        return switch (this) {
            case ANTHROPIC -> base.contains("/messages") ? base : base + "/messages";
            // An Azure URL is given whole, query string and all, because the deployment is part of the path.
            case AZURE -> base;
            default -> base + "/chat/completions";
        };
    }

    /**
     * Something cheap that proves the key and the address are both right.
     *
     * <p>Anthropic has no listing endpoint worth calling unauthenticated, so its reachability is shown by the
     * first real question rather than guessed at here.
     */
    public String probeUrl(String baseUrl) {
        return this == ANTHROPIC ? null : trimSlash(baseUrl) + "/models";
    }

    /** How the key is presented. Each provider wants its own header, and some want a version alongside. */
    public void authenticate(java.net.http.HttpRequest.Builder request, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return;
        }
        switch (this) {
            case ANTHROPIC -> {
                request.header("x-api-key", apiKey);
                request.header("anthropic-version", ANTHROPIC_VERSION);
            }
            case AZURE -> request.header("api-key", apiKey);
            default -> request.header("Authorization", "Bearer " + apiKey);
        }
    }

    /** The version Anthropic's API is pinned to; it requires one and will not assume. */
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    /**
     * The request body.
     *
     * <p>Anthropic takes the system prompt as its own field rather than as messages, so the system turns this
     * tool builds -- the rules, the element listing, what has already been applied -- are folded into one and
     * lifted out. Everything else is the same conversation either way.
     */
    public JsonObject body(String model, JsonArray messages, int maxTokens, double temperature) {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("temperature", temperature);
        body.addProperty("max_tokens", maxTokens);
        if (this != ANTHROPIC) {
            body.addProperty("stream", false);
            body.add("messages", messages);
            return body;
        }
        StringBuilder system = new StringBuilder();
        JsonArray turns = new JsonArray();
        for (JsonElement entry : messages) {
            JsonObject message = entry.getAsJsonObject();
            String role = message.get("role").getAsString();
            String content = message.get("content").getAsString();
            if ("system".equals(role)) {
                if (system.length() > 0) {
                    system.append("\n\n");
                }
                system.append(content);
            } else {
                turns.add(message);
            }
        }
        if (system.length() > 0) {
            body.addProperty("system", system.toString());
        }
        body.add("messages", turns);
        return body;
    }

    /** The assistant's text, wherever this provider puts it. */
    public String replyOf(String responseBody) {
        JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
        if (this == ANTHROPIC) {
            JsonArray content = root.getAsJsonArray("content");
            StringBuilder text = new StringBuilder();
            for (JsonElement entry : content) {
                JsonObject block = entry.getAsJsonObject();
                if (block.has("text")) {
                    text.append(block.get("text").getAsString());
                }
            }
            return text.toString();
        }
        return root.getAsJsonArray("choices").get(0).getAsJsonObject()
                .getAsJsonObject("message").get("content").getAsString();
    }

    private static String trimSlash(String url) {
        String value = url == null ? "" : url.trim();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}

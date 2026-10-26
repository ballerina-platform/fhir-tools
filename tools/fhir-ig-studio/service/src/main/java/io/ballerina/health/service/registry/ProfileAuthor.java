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

package io.ballerina.health.service.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Locale;

/**
 * Builds a new profile from the few things that actually distinguish one.
 *
 * <p>A profile is a constraint on something that already exists -- a core resource, or another profile in this
 * package -- so what has to be supplied is a name, what it derives from, and what it is for. The kind, the
 * type, the derivation and the root element are all determined by the base, so they are filled in here rather
 * than asked for.
 *
 * <p>It is created with nothing constrained yet. That is deliberate: the element table is where constraints
 * are made, and a profile that narrowed something the moment it was created would be asserting a rule nobody
 * asked for.
 */
public final class ProfileAuthor {

    private ProfileAuthor() {
    }

    /**
     * @param id            the profile's id, which also names its file and completes its canonical URL
     * @param title         human-readable title
     * @param description   what the profile is for
     * @param canonicalBase the package's canonical prefix, so the URL sits alongside its siblings
     * @param baseUrl       the canonical URL of the resource or profile this one constrains
     * @param resourceType  the FHIR resource type being profiled, taken from the base
     * @param fhirVersion   the version the package is written against
     */
    public static JsonObject build(String id, String title, String description, String canonicalBase,
                                   String baseUrl, String resourceType, String fhirVersion) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("A profile needs an id.");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("A profile needs something to be based on.");
        }
        if (resourceType == null || resourceType.isBlank()) {
            throw new IllegalArgumentException("The base definition does not say which resource it profiles.");
        }

        String safeId = ExtensionAuthor.sanitiseId(id);
        JsonObject definition = new JsonObject();
        definition.addProperty("resourceType", "StructureDefinition");
        definition.addProperty("id", safeId);
        definition.addProperty("url", trimSlash(canonicalBase) + "/" + safeId);
        definition.addProperty("name", toName(safeId));
        if (title != null && !title.isBlank()) {
            definition.addProperty("title", title);
        }
        definition.addProperty("status", "draft");
        if (description != null && !description.isBlank()) {
            definition.addProperty("description", description);
        }
        definition.addProperty("fhirVersion", fhirVersion == null || fhirVersion.isBlank()
                ? "4.0.1" : fhirVersion);
        definition.addProperty("kind", "resource");
        definition.addProperty("abstract", false);
        definition.addProperty("type", resourceType);
        definition.addProperty("baseDefinition", baseUrl);
        definition.addProperty("derivation", "constraint");

        // The root element alone: a profile has to say which resource it is about, and nothing more until
        // somebody constrains something.
        JsonObject root = new JsonObject();
        root.addProperty("id", resourceType);
        root.addProperty("path", resourceType);
        if (title != null && !title.isBlank()) {
            root.addProperty("short", title);
        }
        if (description != null && !description.isBlank()) {
            root.addProperty("definition", description);
        }
        JsonArray elements = new JsonArray();
        elements.add(root);
        JsonObject differential = new JsonObject();
        differential.add("element", elements);
        definition.add("differential", differential);
        return definition;
    }

    /** "au-core-patient" -> "AuCorePatient", which is the shape FHIR wants for a name. */
    private static String toName(String id) {
        StringBuilder builder = new StringBuilder();
        for (String part : id.split("[-.]")) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.length() == 0 ? "Profile" : builder.toString();
    }

    private static String trimSlash(String url) {
        String value = url == null || url.isBlank()
                ? "http://example.org/fhir/StructureDefinition" : url.trim();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}

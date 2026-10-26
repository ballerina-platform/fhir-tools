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

import java.util.List;
import java.util.Locale;

/**
 * Builds a new extension StructureDefinition from the handful of things that actually distinguish one.
 *
 * <p>An extension is a profile of the {@code Extension} datatype, and almost all of it is boilerplate that is
 * the same every time: the kind, the base definition, the fixed {@code Extension.url}, and the
 * {@code Extension.extension 0..0} that marks it as simple rather than nested. What a person supplies is the
 * name, where it may be used, and what it carries -- so that is all the editor asks for, and this fills in the
 * rest in the shape published guides use.
 */
public final class ExtensionAuthor {

    /** The FHIR datatypes offered for an extension's value. Complex nesting is out of scope here. */
    public static final List<String> VALUE_TYPES = List.of(
            "boolean", "integer", "decimal", "string", "uri", "url", "canonical", "code", "date", "dateTime",
            "instant", "time", "markdown", "base64Binary", "Coding", "CodeableConcept", "Quantity", "Identifier",
            "HumanName", "Address", "ContactPoint", "Period", "Range", "Ratio", "Attachment", "Annotation",
            "Reference");

    private ExtensionAuthor() {
    }

    /**
     * @param id           the extension's id, which also names its file and completes its canonical URL
     * @param name         computer-friendly name, as FHIR requires for a StructureDefinition
     * @param title        human-readable title
     * @param description  what the extension is for
     * @param canonicalBase the package's canonical prefix, so the URL sits alongside its siblings
     * @param contexts     the element paths the extension may be used on, e.g. "Patient" or "Patient.name"
     * @param valueTypes   the types its value may take; more than one produces a choice
     * @param required     whether the extension is mandatory where it is used
     * @param repeats      whether it may appear more than once
     */
    public static JsonObject build(String id, String name, String title, String description,
                                   String canonicalBase, List<String> contexts, List<String> valueTypes,
                                   boolean required, boolean repeats) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("An extension needs an id.");
        }
        if (contexts == null || contexts.isEmpty()) {
            throw new IllegalArgumentException("An extension needs at least one context: where it may be used.");
        }
        if (valueTypes == null || valueTypes.isEmpty()) {
            throw new IllegalArgumentException("An extension needs at least one value type.");
        }
        for (String type : valueTypes) {
            if (!VALUE_TYPES.contains(type)) {
                throw new IllegalArgumentException("'" + type + "' is not a value type an extension can carry.");
            }
        }

        String safeId = sanitiseId(id);
        String url = trimSlash(canonicalBase) + "/" + safeId;

        JsonObject definition = new JsonObject();
        definition.addProperty("resourceType", "StructureDefinition");
        definition.addProperty("id", safeId);
        definition.addProperty("url", url);
        definition.addProperty("name", name == null || name.isBlank() ? toName(safeId) : name);
        if (title != null && !title.isBlank()) {
            definition.addProperty("title", title);
        }
        definition.addProperty("status", "draft");
        if (description != null && !description.isBlank()) {
            definition.addProperty("description", description);
        }
        definition.addProperty("fhirVersion", "4.0.1");
        definition.addProperty("kind", "complex-type");
        definition.addProperty("abstract", false);
        definition.addProperty("type", "Extension");
        definition.addProperty("baseDefinition", "http://hl7.org/fhir/StructureDefinition/Extension");
        definition.addProperty("derivation", "constraint");

        JsonArray context = new JsonArray();
        for (String expression : contexts) {
            JsonObject entry = new JsonObject();
            entry.addProperty("type", "element");
            entry.addProperty("expression", expression);
            context.add(entry);
        }
        definition.add("context", context);

        JsonArray elements = new JsonArray();
        elements.add(root(title == null || title.isBlank() ? safeId : title, description, required, repeats));
        elements.add(noNestedExtensions());
        elements.add(fixedUrl(url));
        elements.add(value(valueTypes));

        JsonObject differential = new JsonObject();
        differential.add("element", elements);
        definition.add("differential", differential);
        return definition;
    }

    private static JsonObject root(String short_, String description, boolean required, boolean repeats) {
        JsonObject element = new JsonObject();
        element.addProperty("id", "Extension");
        element.addProperty("path", "Extension");
        element.addProperty("short", short_);
        if (description != null && !description.isBlank()) {
            element.addProperty("definition", description);
        }
        element.addProperty("min", required ? 1 : 0);
        element.addProperty("max", repeats ? "*" : "1");
        return element;
    }

    /** 0..0 on the nested extension is what says "this carries a value, not other extensions". */
    private static JsonObject noNestedExtensions() {
        JsonObject element = new JsonObject();
        element.addProperty("id", "Extension.extension");
        element.addProperty("path", "Extension.extension");
        element.addProperty("max", "0");
        return element;
    }

    private static JsonObject fixedUrl(String url) {
        JsonObject element = new JsonObject();
        element.addProperty("id", "Extension.url");
        element.addProperty("path", "Extension.url");
        element.addProperty("fixedUri", url);
        return element;
    }

    private static JsonObject value(List<String> valueTypes) {
        JsonObject element = new JsonObject();
        element.addProperty("id", "Extension.value[x]");
        element.addProperty("path", "Extension.value[x]");
        element.addProperty("min", 1);
        element.addProperty("max", "1");
        JsonArray types = new JsonArray();
        for (String code : valueTypes) {
            JsonObject type = new JsonObject();
            type.addProperty("code", code);
            types.add(type);
        }
        element.add("type", types);
        return element;
    }

    /** Ids appear in URLs and file names, so they are held to the character set FHIR allows. */
    public static String sanitiseId(String id) {
        String cleaned = id.trim().replaceAll("[^A-Za-z0-9-.]", "-").replaceAll("-{2,}", "-")
                .replaceAll("^[-.]+", "").replaceAll("[-.]+$", "");
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("That id has no usable characters.");
        }
        return cleaned.length() > 64 ? cleaned.substring(0, 64) : cleaned;
    }

    /** "us-core-birth-time" -> "UsCoreBirthTime", which is the shape FHIR wants for a name. */
    private static String toName(String id) {
        StringBuilder builder = new StringBuilder();
        for (String part : id.split("[-.]")) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.length() == 0 ? "Extension" : builder.toString();
    }

    private static String trimSlash(String url) {
        String value = url == null || url.isBlank()
                ? "http://example.org/fhir/StructureDefinition" : url.trim();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}

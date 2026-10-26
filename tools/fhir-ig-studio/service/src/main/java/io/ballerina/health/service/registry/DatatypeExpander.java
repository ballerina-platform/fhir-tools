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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Expands the complex datatypes a profile's elements carry, so their children can be seen and constrained.
 *
 * <p>A resource snapshot stops at the element that holds a datatype: {@code Patient.name.period} says it is a
 * Period and nothing more, so {@code start} and {@code end} are invisible even though the datatype is known.
 * Published IG sites handle this by linking out to the datatype's own page; here the children are brought
 * inline instead, which is what makes them constrainable in place.
 *
 * <p>The type's own StructureDefinition is the source, so a datatype the IG profiles for itself expands from
 * that profile rather than from the core datatype: an element typed with a profiled Address shows the
 * constraints the IG put on it. Where an element names several types, expanding would be ambiguous, so it is
 * left alone.
 *
 * <p>Expansion produces a copy. The stored profile keeps exactly the elements it was published with; only what
 * is shown and offered for editing is widened.
 */
public final class DatatypeExpander {

    /**
     * Types not worth drilling into. Primitives have no children to speak of, and an Extension is reached by
     * slicing it rather than by opening up its url and value[x] on every element that can carry one -- which
     * otherwise buries the real datatype children under boilerplate.
     */
    private static final Set<String> NOT_EXPANDED = Set.of(
            "boolean", "integer", "integer64", "string", "decimal", "uri", "url", "canonical", "base64Binary",
            "instant", "date", "dateTime", "time", "code", "oid", "id", "markdown", "unsignedInt", "positiveInt",
            "uuid", "xhtml", "Extension");

    /** A guard against runaway growth: datatypes nest, and every level multiplies the rows on screen. */
    private static final int MAX_ELEMENTS = 2500;

    private DatatypeExpander() {
    }

    /**
     * @param definition the profile to expand, which is not modified
     * @param resolve    finds a StructureDefinition by canonical URL, or returns null
     * @param passes     how many levels deep to go; each pass expands the leaves the previous one revealed
     * @return a copy whose snapshot carries the datatype children, or the original when nothing was added
     */
    public static JsonObject expand(JsonObject definition, Function<String, JsonObject> resolve, int passes) {
        if (definition == null || !definition.has("snapshot")) {
            return definition;
        }
        JsonObject expanded = JsonParser.parseString(definition.toString()).getAsJsonObject();
        boolean changed = false;
        for (int pass = 0; pass < passes; pass++) {
            if (!expandOnce(expanded, resolve)) {
                break;
            }
            changed = true;
        }
        return changed ? expanded : definition;
    }

    private static boolean expandOnce(JsonObject definition, Function<String, JsonObject> resolve) {
        JsonObject snapshot = definition.getAsJsonObject("snapshot");
        if (!snapshot.has("element") || !snapshot.get("element").isJsonArray()) {
            return false;
        }
        List<JsonObject> elements = new ArrayList<>();
        for (JsonElement entry : snapshot.getAsJsonArray("element")) {
            if (entry.isJsonObject()) {
                elements.add(entry.getAsJsonObject());
            }
        }
        if (elements.size() >= MAX_ELEMENTS) {
            return false;
        }
        Set<String> paths = new LinkedHashSet<>();
        for (JsonObject element : elements) {
            paths.add(string(element, "path"));
        }

        JsonArray rebuilt = new JsonArray();
        boolean added = false;
        for (JsonObject element : elements) {
            rebuilt.add(element);
            String path = string(element, "path");
            if (path == null || hasChildren(path, paths)) {
                continue;
            }
            JsonObject datatype = datatypeOf(element, resolve);
            if (datatype == null) {
                continue;
            }
            for (JsonObject child : childrenOf(datatype, path)) {
                if (paths.contains(string(child, "path")) || rebuilt.size() >= MAX_ELEMENTS) {
                    continue;
                }
                rebuilt.add(child);
                added = true;
            }
        }
        if (added) {
            snapshot.add("element", rebuilt);
        }
        return added;
    }

    /** An element already expanded in the snapshot is left as the publisher produced it. */
    private static boolean hasChildren(String path, Set<String> paths) {
        String prefix = path + ".";
        return paths.stream().anyMatch(candidate -> candidate != null && candidate.startsWith(prefix));
    }

    /**
     * The definition of the single complex type an element carries. A choice of types has no one expansion, and
     * a primitive has nothing worth showing, so both return null.
     */
    private static JsonObject datatypeOf(JsonObject element, Function<String, JsonObject> resolve) {
        if (!element.has("type") || !element.get("type").isJsonArray()) {
            return null;
        }
        JsonArray types = element.getAsJsonArray("type");
        if (types.size() != 1 || !types.get(0).isJsonObject()) {
            return null;
        }
        JsonObject type = types.get(0).getAsJsonObject();
        String code = string(type, "code");
        if (code == null || NOT_EXPANDED.contains(code)) {
            return null;
        }
        // A profiled datatype expands from the profile, so the IG's own constraints are what is shown.
        if (type.has("profile") && type.get("profile").isJsonArray()
                && type.getAsJsonArray("profile").size() == 1) {
            JsonObject profiled = resolve.apply(type.getAsJsonArray("profile").get(0).getAsString());
            if (profiled != null) {
                return profiled;
            }
        }
        return resolve.apply("http://hl7.org/fhir/StructureDefinition/" + code);
    }

    /** The datatype's own elements, rebased onto the element being expanded. */
    private static List<JsonObject> childrenOf(JsonObject datatype, String parentPath) {
        List<JsonObject> children = new ArrayList<>();
        if (!datatype.has("snapshot") || !datatype.getAsJsonObject("snapshot").has("element")) {
            return children;
        }
        JsonArray source = datatype.getAsJsonObject("snapshot").getAsJsonArray("element");
        String root = null;
        for (JsonElement entry : source) {
            if (!entry.isJsonObject()) {
                continue;
            }
            String path = string(entry.getAsJsonObject(), "path");
            if (path == null) {
                continue;
            }
            if (root == null) {
                // The first element is the datatype itself; its children hang off that name.
                root = path;
                continue;
            }
            if (!path.startsWith(root + ".")) {
                continue;
            }
            String tail = path.substring(root.length() + 1);
            // One level only: deeper elements come from the next pass, once their parent exists here.
            if (tail.contains(".")) {
                continue;
            }
            JsonObject child = JsonParser.parseString(entry.getAsJsonObject().toString()).getAsJsonObject();
            child.addProperty("path", parentPath + "." + tail);
            child.addProperty("id", parentPath + "." + tail);
            children.add(child);
        }
        return children;
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
}

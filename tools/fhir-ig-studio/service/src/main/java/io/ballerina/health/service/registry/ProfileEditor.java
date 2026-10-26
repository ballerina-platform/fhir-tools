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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns a profile into something a form can edit, and applies changes back into its differential.
 *
 * <p>The point is that the UI should not be able to express an illegal profile in the first place. For each
 * element this reports both what the profile currently says and what its base allows, so the controls can be
 * bounded: a cardinality spinner that will not go below the base's minimum, a type picker holding only the
 * base's types, a binding strength list starting at the base's strength. {@link ProfileRuleValidator} still
 * checks the result on save, so a hand-edited JSON or a stale form cannot get around it.
 */
public final class ProfileEditor {

    /** What a profile may change about an element. Anything not in here is not offered by the editor. */
    private static final Set<String> EDITABLE = Set.of(
            "min", "max", "mustSupport", "short", "definition", "comment", "types", "bindingStrength",
            "bindingValueSet");

    private ProfileEditor() {
    }

    /**
     * Describes every element of the profile, with the bounds its base imposes.
     *
     * <p>Read from the profile's snapshot when it has one, since that is the complete element list and so the
     * full set of things that could be constrained; the differential only holds what has been constrained
     * already.
     */
    public static List<Map<String, Object>> describeElements(JsonObject profile, JsonObject base) {
        List<JsonObject> source = elements(profile, "snapshot");
        boolean fromSnapshot = !source.isEmpty();
        if (!fromSnapshot) {
            source = elements(profile, "differential");
        }
        // What the profile itself says about an element, which is the differential rather than the resolved
        // snapshot. It matters where the snapshot cannot carry the constraint -- an element brought in by
        // expanding a datatype is not in the snapshot at all, so its edits would read back as if never made.
        Map<String, JsonObject> constrainedBy = new LinkedHashMap<>();
        for (JsonObject element : elements(profile, "differential")) {
            constrainedBy.put(key(string(element, "path"), string(element, "sliceName")), element);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (JsonObject element : source) {
            String path = string(element, "path");
            if (path == null) {
                continue;
            }
            String sliceName = string(element, "sliceName");
            JsonObject baseElement = ProfileRuleValidator.exactBaseElement(path, base);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("path", path);
            row.put("sliceName", sliceName);
            row.put("id", string(element, "id"));
            JsonObject declared = constrainedBy.get(key(path, sliceName));
            row.put("constrained", declared != null);

            // The differential wins field by field: it says what the profile changed, and the snapshot fills in
            // everything it did not.
            JsonObject effective = declared == null ? element : merge(element, declared);
            Map<String, Object> current = new LinkedHashMap<>();
            current.put("min", integer(effective, "min"));
            current.put("max", string(effective, "max"));
            current.put("mustSupport", bool(effective, "mustSupport"));
            current.put("short", string(effective, "short"));
            current.put("definition", string(effective, "definition"));
            current.put("comment", string(effective, "comment"));
            current.put("types", new ArrayList<>(typeCodes(effective)));
            JsonObject binding = object(effective, "binding");
            current.put("bindingStrength", binding == null ? null : string(binding, "strength"));
            current.put("bindingValueSet", binding == null ? null : string(binding, "valueSet"));
            row.put("current", current);

            // Null bounds mean "the base says nothing here", which the UI shows as unconstrained rather than
            // as permission to do anything: the validator still has the final say.
            Map<String, Object> limits = new LinkedHashMap<>();
            boolean isSlice = sliceName != null && !sliceName.isBlank();
            limits.put("minMin", isSlice ? Integer.valueOf(0) : integer(baseElement, "min"));
            limits.put("maxMax", baseElement == null ? null : string(baseElement, "max"));
            limits.put("allowedTypes", baseElement == null ? null : new ArrayList<>(typeCodes(baseElement)));
            JsonObject baseBinding = object(baseElement, "binding");
            limits.put("minBindingStrength", baseBinding == null ? null : string(baseBinding, "strength"));
            limits.put("knownToBase", baseElement != null);
            row.put("limits", limits);

            rows.add(row);
        }
        return rows;
    }

    /**
     * Applies a set of constraint changes to one element, writing them into the profile's differential.
     *
     * <p>The differential is the profile's own statement of what it constrains, so that is what an edit has to
     * change; the snapshot is regenerated from it afterwards by the caller. An element not yet constrained gets
     * a differential entry created for it.
     *
     * @return the differential element as it now stands
     */
    public static JsonObject applyChanges(JsonObject profile, String path, String sliceName,
                                          JsonObject changes) {
        for (String field : changes.keySet()) {
            if (!EDITABLE.contains(field)) {
                throw new IllegalArgumentException("'" + field + "' is not something a profile may change here.");
            }
        }
        JsonObject element = differentialElement(profile, path, sliceName);

        if (changes.has("min")) {
            setOrRemove(element, "min", changes.get("min"));
        }
        if (changes.has("max")) {
            setOrRemove(element, "max", changes.get("max"));
        }
        if (changes.has("mustSupport")) {
            setOrRemove(element, "mustSupport", changes.get("mustSupport"));
        }
        for (String text : List.of("short", "definition", "comment")) {
            if (changes.has(text)) {
                setOrRemove(element, text, changes.get(text));
            }
        }
        if (changes.has("types")) {
            applyTypes(element, changes.get("types"));
        }
        if (changes.has("bindingStrength") || changes.has("bindingValueSet")) {
            applyBinding(element, changes);
        }
        return element;
    }

    /**
     * Adds a slice to a repeating element.
     *
     * <p>Slicing an {@code extension} is how a profile says "this resource carries that extension", so a slice
     * on an extension path binds the chosen extension through {@code type.profile}. Slicing anything else names
     * a subset of the repeats and leaves what distinguishes them to the discriminator.
     *
     * @param extensionUrl the extension the slice binds, for extension slices; null otherwise
     * @param description  what the slice is for, shown as the element's short text
     */
    public static JsonObject addSlice(JsonObject profile, String path, String sliceName, Integer min, String max,
                                      boolean mustSupport, String extensionUrl, String description,
                                      JsonObject base) {
        if (sliceName == null || sliceName.isBlank()) {
            throw new IllegalArgumentException("A slice needs a name.");
        }
        String safeName = sliceName.trim();
        if (!safeName.matches("[A-Za-z][A-Za-z0-9-]*")) {
            throw new IllegalArgumentException(
                    "'" + safeName + "' is not a usable slice name: use a letter followed by letters, digits or "
                            + "hyphens.");
        }
        for (JsonObject existing : elements(profile, "differential")) {
            if (path.equals(string(existing, "path")) && safeName.equals(string(existing, "sliceName"))) {
                throw new IllegalArgumentException("'" + path + "' already has a slice called '" + safeName + "'.");
            }
        }
        boolean isExtension = path.endsWith(".extension") || path.endsWith(".modifierExtension");
        if (isExtension && (extensionUrl == null || extensionUrl.isBlank())) {
            throw new IllegalArgumentException("An extension slice has to bind an extension.");
        }
        ensureSlicing(profile, path, isExtension ? "url" : "$this", base);

        JsonObject slice = differentialElement(profile, path, safeName);
        slice.addProperty("min", min == null ? 0 : min);
        slice.addProperty("max", max == null || max.isBlank() ? "1" : max);
        if (mustSupport) {
            slice.addProperty("mustSupport", true);
        }
        if (description != null && !description.isBlank()) {
            // "short" is the one-line label element tables print; the fuller "definition" mirrors it so the
            // slice reads the same wherever it is shown.
            slice.addProperty("short", description.trim());
            slice.addProperty("definition", description.trim());
        }
        if (extensionUrl != null && !extensionUrl.isBlank()) {
            JsonObject type = new JsonObject();
            type.addProperty("code", "Extension");
            JsonArray profiles = new JsonArray();
            profiles.add(extensionUrl);
            type.add("profile", profiles);
            JsonArray types = new JsonArray();
            types.add(type);
            slice.add("type", types);
        }
        return slice;
    }

    /**
     * Declares how an element is sliced, unless it is already said somewhere.
     *
     * <p>Extensions are always told apart by their url, which is why published guides often add slices without
     * restating it. Stating it explicitly costs nothing and leaves the profile readable on its own.
     */
    private static void ensureSlicing(JsonObject profile, String path, String discriminatorPath, JsonObject base) {
        for (JsonObject existing : elements(profile, "differential")) {
            if (path.equals(string(existing, "path")) && string(existing, "sliceName") == null
                    && existing.has("slicing")) {
                return;
            }
        }
        JsonObject baseElement = ProfileRuleValidator.exactBaseElement(path, base);
        if (baseElement != null && baseElement.has("slicing")) {
            return;
        }
        JsonObject discriminator = new JsonObject();
        discriminator.addProperty("type", "value");
        discriminator.addProperty("path", discriminatorPath);
        JsonArray discriminators = new JsonArray();
        discriminators.add(discriminator);
        JsonObject slicing = new JsonObject();
        slicing.add("discriminator", discriminators);
        slicing.addProperty("rules", "open");
        differentialElement(profile, path, null).add("slicing", slicing);
    }

    private static void applyTypes(JsonObject element, JsonElement types) {
        if (types == null || types.isJsonNull() || !types.isJsonArray() || types.getAsJsonArray().isEmpty()) {
            element.remove("type");
            return;
        }
        // Existing type entries carry profiles and target profiles, so keep the ones still selected rather than
        // rebuilding them from the code alone and losing that detail.
        Map<String, JsonObject> existing = new LinkedHashMap<>();
        if (element.has("type") && element.get("type").isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray("type")) {
                if (entry.isJsonObject()) {
                    String code = string(entry.getAsJsonObject(), "code");
                    if (code != null) {
                        existing.put(code, entry.getAsJsonObject());
                    }
                }
            }
        }
        JsonArray updated = new JsonArray();
        for (JsonElement selected : types.getAsJsonArray()) {
            String code = selected.getAsString();
            JsonObject kept = existing.get(code);
            if (kept != null) {
                updated.add(kept);
            } else {
                JsonObject fresh = new JsonObject();
                fresh.addProperty("code", code);
                updated.add(fresh);
            }
        }
        element.add("type", updated);
    }

    private static void applyBinding(JsonObject element, JsonObject changes) {
        JsonObject binding = object(element, "binding");
        if (binding == null) {
            binding = new JsonObject();
        }
        if (changes.has("bindingStrength")) {
            setOrRemove(binding, "strength", changes.get("bindingStrength"));
        }
        if (changes.has("bindingValueSet")) {
            setOrRemove(binding, "valueSet", changes.get("bindingValueSet"));
        }
        if (binding.keySet().isEmpty()) {
            element.remove("binding");
        } else {
            element.add("binding", binding);
        }
    }

    /** Finds the differential entry for an element, creating one in path order when it has none yet. */
    private static JsonObject differentialElement(JsonObject profile, String path, String sliceName) {
        JsonObject differential = object(profile, "differential");
        if (differential == null) {
            differential = new JsonObject();
            profile.add("differential", differential);
        }
        JsonArray list = differential.has("element") && differential.get("element").isJsonArray()
                ? differential.getAsJsonArray("element") : new JsonArray();
        differential.add("element", list);

        for (JsonElement entry : list) {
            if (!entry.isJsonObject()) {
                continue;
            }
            JsonObject candidate = entry.getAsJsonObject();
            if (key(path, sliceName).equals(key(string(candidate, "path"), string(candidate, "sliceName")))) {
                return candidate;
            }
        }
        JsonObject created = new JsonObject();
        created.addProperty("path", path);
        if (sliceName != null && !sliceName.isBlank()) {
            created.addProperty("sliceName", sliceName);
        }
        created.addProperty("id", sliceName == null || sliceName.isBlank() ? path : path + ":" + sliceName);
        // Differential elements are read in document order, so insert before the first element that sorts after
        // this one rather than appending and leaving the list out of order.
        JsonArray reordered = new JsonArray();
        boolean inserted = false;
        for (JsonElement entry : list) {
            String candidatePath = entry.isJsonObject() ? string(entry.getAsJsonObject(), "path") : null;
            if (!inserted && candidatePath != null && candidatePath.compareTo(path) > 0) {
                reordered.add(created);
                inserted = true;
            }
            reordered.add(entry);
        }
        if (!inserted) {
            reordered.add(created);
        }
        differential.add("element", reordered);
        return created;
    }

    private static void setOrRemove(JsonObject target, String field, JsonElement value) {
        if (value == null || value.isJsonNull()
                || (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                && value.getAsString().isBlank())) {
            target.remove(field);
            return;
        }
        target.add(field, value);
    }

    /**
     * Lays a profile's differential over its snapshot, element by element.
     *
     * <p>The snapshot is normally rebuilt from the base by the FHIR libraries, but that fails for some
     * published profiles -- their own differential contains elements the generator will not match -- and then
     * the snapshot keeps whatever it shipped with. Everything derived from the differential is still right, so
     * the element view looks correct while the rendered table, which reads the snapshot, shows the old values.
     * This brings the snapshot into line so both say the same thing.
     *
     * @return how many snapshot elements were changed
     */
    public static int overlayDifferential(JsonObject profile) {
        if (profile == null || !profile.has("snapshot") || !profile.has("differential")) {
            return 0;
        }
        JsonObject snapshot = profile.getAsJsonObject("snapshot");
        JsonObject differential = profile.getAsJsonObject("differential");
        if (!snapshot.has("element") || !differential.has("element")) {
            return 0;
        }
        Map<String, JsonObject> declared = new LinkedHashMap<>();
        for (JsonElement entry : differential.getAsJsonArray("element")) {
            if (entry.isJsonObject()) {
                JsonObject element = entry.getAsJsonObject();
                declared.put(key(string(element, "path"), string(element, "sliceName")), element);
            }
        }
        JsonArray rebuilt = new JsonArray();
        int changed = 0;
        for (JsonElement entry : snapshot.getAsJsonArray("element")) {
            if (!entry.isJsonObject()) {
                rebuilt.add(entry);
                continue;
            }
            JsonObject element = entry.getAsJsonObject();
            JsonObject over = declared.get(key(string(element, "path"), string(element, "sliceName")));
            if (over == null) {
                rebuilt.add(element);
                continue;
            }
            JsonObject merged = merge(element, over);
            if (!merged.equals(element)) {
                changed++;
            }
            rebuilt.add(merged);
        }
        if (changed > 0) {
            snapshot.add("element", rebuilt);
        }
        return changed;
    }

    /** The snapshot element with the differential's own statements laid over it. */
    private static JsonObject merge(JsonObject snapshot, JsonObject differential) {
        JsonObject merged = new JsonObject();
        for (String member : snapshot.keySet()) {
            merged.add(member, snapshot.get(member));
        }
        for (String member : differential.keySet()) {
            merged.add(member, differential.get(member));
        }
        return merged;
    }

    private static String key(String path, String sliceName) {
        return sliceName == null || sliceName.isBlank() ? String.valueOf(path) : path + ":" + sliceName;
    }

    private static List<JsonObject> elements(JsonObject definition, String section) {
        List<JsonObject> result = new ArrayList<>();
        JsonObject container = object(definition, section);
        if (container == null || !container.has("element") || !container.get("element").isJsonArray()) {
            return result;
        }
        for (JsonElement element : container.getAsJsonArray("element")) {
            if (element.isJsonObject()) {
                result.add(element.getAsJsonObject());
            }
        }
        return result;
    }

    private static Set<String> typeCodes(JsonObject element) {
        Set<String> codes = new LinkedHashSet<>();
        if (element == null || !element.has("type") || !element.get("type").isJsonArray()) {
            return codes;
        }
        for (JsonElement type : element.getAsJsonArray("type")) {
            if (type.isJsonObject()) {
                String code = string(type.getAsJsonObject(), "code");
                if (code != null) {
                    codes.add(code);
                }
            }
        }
        return codes;
    }

    private static JsonObject object(JsonObject json, String member) {
        return json != null && json.has(member) && json.get(member).isJsonObject()
                ? json.getAsJsonObject(member) : null;
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

    private static Integer integer(JsonObject json, String member) {
        if (json == null || !json.has(member) || json.get(member).isJsonNull()) {
            return null;
        }
        try {
            return json.get(member).getAsInt();
        } catch (Exception e) {
            return null;
        }
    }

    private static Boolean bool(JsonObject json, String member) {
        if (json == null || !json.has(member) || json.get(member).isJsonNull()) {
            return null;
        }
        try {
            return json.get(member).getAsBoolean();
        } catch (Exception e) {
            return null;
        }
    }
}

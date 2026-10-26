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
 * Checks an edited profile against the rules FHIR puts on profiling: a profile may only make the base
 * definition more restrictive, never less.
 *
 * <p>This is what stops the editor from producing something that looks fine but is not a legal derivation of
 * what it claims to profile. The checks are the ones that can be decided from the base snapshot alone:
 *
 * <ul>
 *   <li>every constrained element has to exist in the base (allowing for slices and extensions);</li>
 *   <li>cardinality may only narrow -- {@code min} can rise, {@code max} can fall, and neither may cross;</li>
 *   <li>types may only be a subset of the base's, never new ones;</li>
 *   <li>a value set binding may only be strengthened, following example &lt; preferred &lt; extensible &lt;
 *       required;</li>
 *   <li>the profile's own identity ({@code type}, {@code baseDefinition}, {@code derivation}) may not change,
 *       since that would make it a profile of something else.</li>
 * </ul>
 *
 * <p>Findings are reported as errors, which block the save, or warnings, which do not.
 */
public final class ProfileRuleValidator {

    /** Binding strengths, weakest first; a profile may move right along this list but not left. */
    private static final List<String> BINDING_STRENGTHS = List.of("example", "preferred", "extensible", "required");

    private ProfileRuleValidator() {
    }

    /**
     * @param edited   the profile as the user wants to save it
     * @param original the profile as it stands before the edit, used to keep its identity fixed
     * @param base     the snapshot of the definition it profiles, or null when it cannot be resolved
     */
    public static List<Finding> validate(JsonObject edited, JsonObject original, JsonObject base) {
        List<Finding> findings = new ArrayList<>();
        checkIdentity(edited, original, findings);
        if (base == null) {
            findings.add(Finding.warning("",
                    "The base definition could not be resolved, so cardinality, type and binding changes were "
                            + "not checked against it."));
            return findings;
        }
        Map<String, JsonObject> baseElements = elementsByPath(base, "snapshot");
        if (baseElements.isEmpty()) {
            findings.add(Finding.warning("", "The base definition ships no snapshot, so elements were not checked."));
            return findings;
        }
        for (JsonObject element : elements(edited, "differential")) {
            checkElement(element, baseElements, findings);
        }
        return findings;
    }

    /**
     * The base element that governs {@code path}, but only when the base really describes that element: used by
     * the editor to work out what an element may be constrained to, where an ancestor's values would be wrong.
     *
     * @return the base element, or null when the base says nothing specific about this path
     */
    public static JsonObject exactBaseElement(String path, JsonObject base) {
        if (base == null) {
            return null;
        }
        BaseMatch match = resolveBase(path, elementsByPath(base, "snapshot"));
        return match != null && match.exact() ? match.element() : null;
    }

    /** Every binding strength, weakest first, for callers offering the legal choices. */
    public static List<String> bindingStrengths() {
        return BINDING_STRENGTHS;
    }

    /** A profile that changes what it profiles is no longer an edit of this profile. */
    private static void checkIdentity(JsonObject edited, JsonObject original, List<Finding> findings) {
        for (String field : List.of("type", "baseDefinition", "derivation")) {
            String before = string(original, field);
            String after = string(edited, field);
            if (before != null && !before.equals(after)) {
                findings.add(Finding.error(field,
                        "'" + field + "' may not change: this profile constrains " + before
                                + ", changing it would make it a profile of something else."));
            }
        }
    }

    private static void checkElement(JsonObject element, Map<String, JsonObject> baseElements,
                                     List<Finding> findings) {
        String path = string(element, "path");
        if (path == null || path.isBlank()) {
            findings.add(Finding.error("", "Every element in the differential needs a 'path'."));
            return;
        }
        BaseMatch match = resolveBase(path, baseElements);
        if (match == null) {
            findings.add(Finding.error(path,
                    "'" + path + "' does not exist in the base definition. A profile can only constrain "
                            + "elements the base already declares (extensions go on an 'extension' slice)."));
            return;
        }
        if (!match.exact()) {
            // The match is an ancestor that carries a datatype, not this element: the base snapshot stops at
            // Patient.name and says nothing about Patient.name.family. Its cardinality, types and binding
            // belong to the ancestor, so comparing this element against them would be meaningless -- and was
            // the cause of published, valid profiles being rejected.
            return;
        }
        checkCardinality(element, match.element(), path, findings);
        checkTypes(element, match.element(), path, findings);
        checkBinding(element, match.element(), path, findings);
    }

    private static void checkCardinality(JsonObject element, JsonObject baseElement, String path,
                                         List<Finding> findings) {
        // Slices divide up the content of the element they slice, so an individual slice may be optional even
        // where the base requires the element overall: AU Base makes each medication[x] slice 0..1 under a
        // base that is 1..1. Only the upper bound still binds a slice.
        boolean isSlice = string(element, "sliceName") != null;
        Integer min = integer(element, "min");
        Integer baseMin = integer(baseElement, "min");
        if (!isSlice && min != null && baseMin != null && min < baseMin) {
            findings.add(Finding.error(path,
                    "min " + min + " is weaker than the base's " + baseMin
                            + ". A profile may only make an element more required, never less."));
        }
        Integer max = maxAsInt(string(element, "max"));
        Integer baseMax = maxAsInt(string(baseElement, "max"));
        if (max != null && baseMax != null && max > baseMax) {
            findings.add(Finding.error(path,
                    "max " + string(element, "max") + " is wider than the base's " + string(baseElement, "max")
                            + ". A profile may only narrow how many times an element can repeat."));
        }
        Integer effectiveMin = min != null ? min : (isSlice ? null : baseMin);
        Integer effectiveMax = max != null ? max : baseMax;
        if (effectiveMin != null && effectiveMax != null && effectiveMin > effectiveMax) {
            findings.add(Finding.error(path, "min " + effectiveMin + " is greater than max " + effectiveMax + "."));
        }
    }

    private static void checkTypes(JsonObject element, JsonObject baseElement, String path,
                                   List<Finding> findings) {
        Set<String> declared = typeCodes(element);
        Set<String> allowed = typeCodes(baseElement);
        if (declared.isEmpty() || allowed.isEmpty()) {
            return;
        }
        // Primitive values are represented in the base as a fhirpath system type rather than the FHIR type the
        // profile names, so the two are not comparable.
        if (allowed.stream().anyMatch(type -> type.startsWith("http://hl7.org/fhirpath/System."))) {
            return;
        }
        for (String type : declared) {
            if (!allowed.contains(type)) {
                findings.add(Finding.error(path,
                        "type '" + type + "' is not one the base allows (" + String.join(", ", allowed)
                                + "). A profile may only pick from the base's types, not add new ones."));
            }
        }
    }

    private static void checkBinding(JsonObject element, JsonObject baseElement, String path,
                                     List<Finding> findings) {
        JsonObject binding = object(element, "binding");
        JsonObject baseBinding = object(baseElement, "binding");
        if (binding == null || baseBinding == null) {
            return;
        }
        int strength = BINDING_STRENGTHS.indexOf(String.valueOf(string(binding, "strength")));
        int baseStrength = BINDING_STRENGTHS.indexOf(String.valueOf(string(baseBinding, "strength")));
        if (strength >= 0 && baseStrength >= 0 && strength < baseStrength) {
            // Reported, not blocked: the spec says bindings should only strengthen, but published guides do
            // relax them deliberately (US Core's vital signs profile takes the base's 'required' code binding
            // down to 'extensible'), so refusing the save would reject real, shipped content.
            findings.add(Finding.warning(path,
                    "binding strength '" + string(binding, "strength") + "' is weaker than the base's '"
                            + string(baseBinding, "strength") + "'. Bindings are normally only strengthened."));
        }
        String valueSet = string(binding, "valueSet");
        String baseValueSet = string(baseBinding, "valueSet");
        if (baseStrength == BINDING_STRENGTHS.indexOf("required") && valueSet != null && baseValueSet != null
                && !valueSet.equals(baseValueSet)) {
            findings.add(Finding.warning(path,
                    "the base binds this to " + baseValueSet + " as 'required'; a different value set is only "
                            + "valid if it is a subset of it."));
        }
    }

    /**
     * Finds the base element a differential path refers to.
     *
     * <p>Only two rewrites are allowed, and both are real FHIR spellings rather than a general "walk up until
     * something matches": slice names ({@code Patient.extension:race}) do not appear in a base snapshot, and a
     * choice element may be named for the type it is constrained to ({@code Patient.deceasedBoolean} for
     * {@code Patient.deceased[x]}). Anything else that does not match is a path the base does not declare, and
     * saying so is the point of the check.
     */
    private static BaseMatch resolveBase(String path, Map<String, JsonObject> baseElements) {
        JsonObject direct = baseElements.get(path);
        if (direct != null) {
            return new BaseMatch(direct, true);
        }
        String withoutSlices = stripSliceNames(path);
        JsonObject sliced = baseElements.get(withoutSlices);
        if (sliced != null) {
            // A slice constrains the element it slices, so the base element really is this one.
            return new BaseMatch(sliced, true);
        }
        JsonObject choice = resolveChoice(withoutSlices, baseElements);
        if (choice != null) {
            // A choice narrowed to one type still has the base choice's cardinality and binding, but its type
            // is deliberately a subset, so the type check would misfire.
            return new BaseMatch(choice, false);
        }
        JsonObject insideDatatype = resolveInsideDatatype(withoutSlices, baseElements);
        return insideDatatype == null ? null : new BaseMatch(insideDatatype, false);
    }

    /**
     * A base element a differential element was matched to, and whether it is the same element ({@code exact})
     * or merely the typed ancestor it lives under. Only an exact match can have its values compared.
     */
    private record BaseMatch(JsonObject element, boolean exact) {
    }

    /**
     * A base snapshot stops at the element that carries a datatype: it declares {@code Patient.name} as a
     * HumanName but not {@code Patient.name.family}. Profiles constrain into those children all the time, so a
     * path whose parent is a typed element is accepted and checked against that parent.
     *
     * <p>Descent is only allowed through an element that actually has a type, which is what separates
     * {@code Patient.name.family} from {@code Patient.notAnElement}: the root element carries no type, so its
     * children have to be declared in the base itself.
     *
     * <p>What this does not do is resolve the datatype's own definition to confirm the child exists in it, so a
     * misspelled leaf inside a datatype is not caught here. Accepting those is the deliberate trade: a false
     * error would block a legitimate edit, which is worse than missing an unusual typo.
     */
    private static JsonObject resolveInsideDatatype(String path, Map<String, JsonObject> baseElements) {
        String candidate = path;
        while (candidate.contains(".")) {
            candidate = candidate.substring(0, candidate.lastIndexOf('.'));
            JsonObject ancestor = baseElements.get(candidate);
            if (ancestor == null) {
                ancestor = resolveChoice(candidate, baseElements);
            }
            if (ancestor != null) {
                return typeCodes(ancestor).isEmpty() ? null : ancestor;
            }
        }
        return null;
    }

    /** "Patient.extension:race.url" -> "Patient.extension.url". */
    private static String stripSliceNames(String path) {
        StringBuilder builder = new StringBuilder();
        for (String segment : path.split("\\.", -1)) {
            if (builder.length() > 0) {
                builder.append('.');
            }
            int slice = segment.indexOf(':');
            builder.append(slice >= 0 ? segment.substring(0, slice) : segment);
        }
        return builder.toString();
    }

    /** "Patient.deceasedBoolean" matches the base's "Patient.deceased[x]". */
    private static JsonObject resolveChoice(String path, Map<String, JsonObject> baseElements) {
        int lastDot = path.lastIndexOf('.');
        if (lastDot < 0) {
            return null;
        }
        String parent = path.substring(0, lastDot);
        String leaf = path.substring(lastDot + 1);
        for (Map.Entry<String, JsonObject> entry : baseElements.entrySet()) {
            String candidate = entry.getKey();
            if (!candidate.startsWith(parent + ".") || !candidate.endsWith("[x]")) {
                continue;
            }
            String stem = candidate.substring(parent.length() + 1, candidate.length() - 3);
            if (leaf.length() > stem.length() && leaf.startsWith(stem)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static Map<String, JsonObject> elementsByPath(JsonObject definition, String section) {
        Map<String, JsonObject> byPath = new LinkedHashMap<>();
        for (JsonObject element : elements(definition, section)) {
            String path = string(element, "path");
            if (path != null) {
                byPath.putIfAbsent(path, element);
            }
        }
        return byPath;
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
        JsonArray types = element.getAsJsonArray("type");
        for (JsonElement type : types) {
            if (type.isJsonObject()) {
                String code = string(type.getAsJsonObject(), "code");
                if (code != null) {
                    codes.add(code);
                }
            }
        }
        return codes;
    }

    /** "*" means unbounded, which compares as larger than any number. */
    private static Integer maxAsInt(String max) {
        if (max == null || max.isBlank()) {
            return null;
        }
        if ("*".equals(max.trim())) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(max.trim());
        } catch (NumberFormatException e) {
            return null;
        }
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

    /** One rule finding. Errors block a save; warnings are advisory. */
    public record Finding(String severity, String path, String message) {

        public static Finding error(String path, String message) {
            return new Finding("error", path, message);
        }

        public static Finding warning(String path, String message) {
            return new Finding("warning", path, message);
        }

        public boolean isError() {
            return "error".equals(severity);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("severity", severity);
            map.put("path", path);
            map.put("message", message);
            return map;
        }
    }
}

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

package org.wso2.healthcare.fhir.codegen.ballerina.project.tool.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Assigns the names of the generated per-profile search-dispatch stub functions ("search&lt;suffix&gt;").
 *
 * <p>These functions are declared at module level, so they must be unique across everything rendered into the
 * same file: one resource type's profiles in separate-service mode, but <em>every</em> resource type's profiles
 * in aggregated mode. Naming is therefore decided here, by whoever owns the full set, rather than by an
 * individual profile that cannot see its siblings.
 *
 * <p>The name a developer actually has to fill in stays short -- normally just the profile name, e.g.
 * {@code searchC4BBOrganization} -- and is only qualified further when that would genuinely collide. Two
 * different profiles can share a bare name (the raw international-base spec reuses a generic example profile
 * verbatim across unrelated resource types, and two IGs can profile the same resource under the same name),
 * which previously produced a "redeclared symbol" failure at {@code bal build}.
 */
public final class SearchFunctionNaming {

    private SearchFunctionNaming() {
    }

    /**
     * Builds a profile-URL to function-suffix map for every profile that shares a generated file.
     *
     * @param profiles all profiles rendered into the same Ballerina source file
     * @return canonical profile URL to unique, Ballerina-identifier-safe function name suffix
     */
    public static Map<String, String> assignSuffixes(Collection<FHIRProfile> profiles) {
        // Progressively more qualified candidates. The first tier that is unambiguous for a given profile wins,
        // so the common case keeps the short name and only genuinely clashing profiles pay for disambiguation.
        // The last tier is the canonical URL, which is unique per profile by the FHIR spec, so it always ends.
        List<Function<FHIRProfile, String>> tiers = List.of(
                p -> identifier(p.getName()),
                p -> identifier(p.getResourceType(), p.getName()),
                p -> identifier(p.getResourceType(), p.getName(), p.getPackagePrefix()),
                p -> identifier(p.getResourceType(), p.getUrl()));

        List<FHIRProfile> pending = new ArrayList<>(profiles);
        Map<String, String> assigned = new LinkedHashMap<>();
        Set<String> taken = new HashSet<>();

        for (Function<FHIRProfile, String> tier : tiers) {
            if (pending.isEmpty()) {
                break;
            }
            Map<String, Integer> counts = new HashMap<>();
            for (FHIRProfile profile : pending) {
                counts.merge(tier.apply(profile), 1, Integer::sum);
            }
            List<FHIRProfile> stillPending = new ArrayList<>();
            for (FHIRProfile profile : pending) {
                String candidate = tier.apply(profile);
                // Unique among the profiles still competing, and not already handed to an earlier tier.
                if (counts.get(candidate) == 1 && !taken.contains(candidate)) {
                    assigned.put(profile.getUrl(), candidate);
                    taken.add(candidate);
                } else {
                    stillPending.add(profile);
                }
            }
            pending = stillPending;
        }

        // Only reachable if two profiles share a canonical URL, which would make them the same profile; number
        // them rather than emitting a duplicate declaration.
        int sequence = 0;
        for (FHIRProfile profile : pending) {
            String candidate = identifier(profile.getResourceType(), profile.getUrl()) + "_" + (++sequence);
            assigned.put(profile.getUrl(), candidate);
        }
        return assigned;
    }

    /**
     * Joins the parts into a Ballerina-identifier-safe name, preserving the existing casing of FHIR profile
     * names (they are already PascalCase, and case-folding them makes the generated stubs hard to match back to
     * the profile they implement).
     */
    private static String identifier(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            String sanitized = sanitize(part);
            if (sanitized.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('_');
            }
            builder.append(Character.toUpperCase(sanitized.charAt(0))).append(sanitized.substring(1));
        }
        String name = builder.toString();
        if (name.isEmpty() || !Character.isLetter(name.charAt(0))) {
            name = "P" + name;
        }
        return name;
    }

    private static String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.replaceAll("[^a-zA-Z0-9]+", "_").replaceAll("^_+", "").replaceAll("_+$", "");
    }
}

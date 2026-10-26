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

import ca.uhn.fhir.context.FhirContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.hl7.fhir.r4.conformance.ProfileUtilities;
import org.hl7.fhir.r4.context.IWorkerContext;
import org.hl7.fhir.r4.context.SimpleWorkerContext;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.hl7.fhir.utilities.xhtml.XhtmlComposer;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders a StructureDefinition as the differential and snapshot tables published FHIR IG sites show.
 *
 * <p>This does not reimplement those tables: it calls HL7's own renderer, the same
 * {@link ProfileUtilities#generateTable} the IG publisher uses, so the output is the real thing rather than a
 * look-alike. The tree connector and type icons it needs are shipped in {@code resources/ig-assets} and unpacked
 * once to a temporary directory, because the renderer reads them from disk and base64-inlines them into the
 * HTML; nothing is fetched at runtime and the generated markup carries no external image URLs.
 *
 * <p>R4 and R5 are both covered, through the renderer each version ships (R5 moved table generation out of
 * {@code ProfileUtilities} into {@code StructureDefinitionRenderer}). R4B and STU3 packages fall back to the
 * viewer's own element table.
 */
public class StructureDefinitionTableRenderer {

    private static final List<String> ASSETS = List.of(
            "icon_choice.gif", "icon_clipboard_j.png", "icon_clipboard_x.png", "icon_datatype.gif",
            "icon_element.gif", "icon_element.png", "icon_entry_blue.png", "icon_extension.png",
            "icon_extension_complex.png", "icon_extension_simple.png", "icon_fixed.gif",
            "icon_modifier_extension_complex.png", "icon_modifier_extension_simple.png", "icon_primitive.png",
            "icon_profile.png", "icon_q_item.png", "icon_q_root.gif", "icon_reference.png", "icon_resource.png",
            "icon_reuse.png", "icon_slice.png", "icon_slice_item.png", "tbl_blank.png", "tbl_spacer.png",
            "tbl_vjoin.png", "tbl_vjoin_end.png", "tbl_vjoin_end_slice.png", "tbl_vjoin_end_slicer.png",
            "tbl_vjoin_slice.png", "tbl_vjoin_slicer.png", "tbl_vline.png", "tbl_vline_end.png",
            "tbl_vline_slice.png", "tbl_vline_slicer.png");

    private final FhirContext fhirContext;
    private final IWorkerContext worker;
    private final Path imageFolder;

    /**
     * @param corePackageTgz the hl7.fhir.r4.core package archive. The renderer resolves base resources and
     *                       datatypes through it -- rendering a snapshot expands choice elements like
     *                       {@code value[x]}, which needs the datatype definitions on hand -- so the context is
     *                       built from the real core package rather than an empty one.
     */
    public StructureDefinitionTableRenderer(Path corePackageTgz, Path assetsDir) throws IOException {
        this.fhirContext = FhirContext.forR4();
        try (InputStream in = Files.newInputStream(corePackageTgz)) {
            SimpleWorkerContext context = SimpleWorkerContext.fromPackage(NpmPackage.fromPackage(in));
            // Snapshot generation expands the value sets elements bind to, and the context refuses to expand
            // anything without expansion parameters, throwing a java.lang.Error rather than an exception.
            context.setExpansionProfile(new Parameters());
            context.setCanRunWithoutTerminology(true);
            this.worker = context;
        } catch (Exception e) {
            throw new IOException("Unable to load the FHIR R4 core definitions from " + corePackageTgz, e);
        }
        this.imageFolder = assetsDir;
        ensureAssets();
    }

    /** The unpacked tree and type icons, shared with the R5 renderer. */
    public Path imageFolder() {
        return imageFolder;
    }

    /** True when this renderer can handle the package's FHIR version. */
    public static boolean supports(String fhirVersion) {
        return fhirVersion == null || "r4".equalsIgnoreCase(fhirVersion) || "r5".equalsIgnoreCase(fhirVersion);
    }

    /**
     * @param definitionJson the StructureDefinition as stored in the workspace
     * @param differential   true for the differential table, false for the snapshot table
     * @return the table markup, ready to drop into the page
     */
    public String render(String definitionJson, boolean differential) throws IOException {
        StructureDefinition profile = fhirContext.newJsonParser()
                .parseResource(StructureDefinition.class, definitionJson);
        ProfileUtilities utilities = new ProfileUtilities(worker, new ArrayList<>(), new LocalKnowledgeProvider());
        XhtmlNode table = utilities.generateTable(
                "definition.html",
                profile,
                differential,
                imageFolder.toString() + "/",
                true,               // inline the tree images as data URIs
                profile.getId(),
                !differential,      // "snapshot" flag is the inverse of the differential one
                "",                 // corePath: no links out to the FHIR spec
                "",                 // imagePath
                false,              // not a logical model
                false,              // only the invariants the profile declares
                null);
        return new XhtmlComposer(true, false).compose(table);
    }

    /**
     * Rebuilds a profile's snapshot from its base and its differential, the way a publisher does, so that an
     * edit to the differential is reflected in the resolved element list the tables and the generator read.
     *
     * @return the regenerated snapshot, or null when it could not be produced
     */
    public JsonObject regenerateSnapshot(JsonObject profileJson, JsonObject baseJson) {
        try {
            StructureDefinition profile = fhirContext.newJsonParser()
                    .parseResource(StructureDefinition.class, profileJson.toString());
            StructureDefinition base = fhirContext.newJsonParser()
                    .parseResource(StructureDefinition.class, baseJson.toString());
            profile.setSnapshot(null);
            ProfileUtilities utilities =
                    new ProfileUtilities(worker, new ArrayList<>(), new LocalKnowledgeProvider());
            utilities.generateSnapshot(base, profile, profile.getUrl(), "", profile.getName());
            if (!profile.hasSnapshot()) {
                return null;
            }
            String regenerated = fhirContext.newJsonParser().encodeResourceToString(profile);
            JsonObject parsed = JsonParser.parseString(regenerated).getAsJsonObject();
            return parsed.has("snapshot") ? parsed.getAsJsonObject("snapshot") : null;
        } catch (Exception | StackOverflowError e) {
            return null;
        } catch (Error e) {
            // The FHIR libraries raise bare Errors for conditions that are really failures of this one call
            // (an unexpandable value set, for instance). Letting one through would kill the request thread.
            return null;
        }
    }

    /**
     * The renderer asks this for the hyperlinks an IG site would have. There is no surrounding site here, so
     * every link is suppressed and only the display text is kept; the table still reads correctly, it just is
     * not clickable.
     */
    private static final class LocalKnowledgeProvider implements ProfileUtilities.ProfileKnowledgeProvider {

        @Override
        public boolean isDatatype(String name) {
            return false;
        }

        @Override
        public boolean isResource(String typeSimple) {
            return false;
        }

        @Override
        public boolean hasLinkFor(String name) {
            return false;
        }

        @Override
        public String getLinkFor(String corePath, String name) {
            // Has to look like an IG page name. The renderer slices this apart (genFixedValue takes everything
            // before ".html" to build a definitions anchor), so null, "" or "#" all fail inside it.
            return pageName(name);
        }

        @Override
        public BindingResolution resolveBinding(StructureDefinition def,
                                                ElementDefinition.ElementDefinitionBindingComponent binding,
                                                String path) {
            BindingResolution resolution = new BindingResolution();
            resolution.display = binding != null && binding.hasValueSet()
                    ? lastSegment(binding.getValueSet()) : "?";
            resolution.url = null;
            return resolution;
        }

        @Override
        public BindingResolution resolveBinding(StructureDefinition def, String url, String path) {
            BindingResolution resolution = new BindingResolution();
            resolution.display = lastSegment(url);
            resolution.url = null;
            return resolution;
        }

        @Override
        public String getLinkForProfile(StructureDefinition profile, String url) {
            // The contract is "name|link"; with no link the renderer prints just the name, which is what keeps
            // the Type column readable instead of showing a raw canonical URL.
            return lastSegment(url) + "|" + pageName(lastSegment(url));
        }

        @Override
        public boolean prependLinks() {
            return false;
        }

        @Override
        public String getLinkForUrl(String corePath, String url) {
            return pageName(lastSegment(url));
        }

        /** An inert, IG-shaped page name; nothing here is served, the anchors just have to parse. */
        private static String pageName(String name) {
            String safe = name == null || name.isBlank() ? "definition" : name.replaceAll("[^A-Za-z0-9._-]", "");
            return (safe.isEmpty() ? "definition" : safe.toLowerCase()) + ".html";
        }

        private static String lastSegment(String url) {
            if (url == null || url.isBlank()) {
                return "";
            }
            String withoutVersion = url.split("\\|")[0];
            return withoutVersion.substring(withoutVersion.lastIndexOf('/') + 1);
        }
    }

    /**
     * Writes the bundled icons out where the renderer can read them, and puts back any that have gone.
     *
     * <p>The renderer wants a directory path rather than a classloader, so the icons have to exist as files.
     * They used to be unpacked once into a system temporary directory, which is fine until the process outlives
     * the operating system's patience with it: macOS reaps files under {@code /var/folders} after a few days,
     * and a service left running longer than that carried on rendering tables whose every icon had silently
     * become an empty image. They now live under the service's own workspace, and every render checks they are
     * still there -- a handful of file-existence checks against a failure that is invisible until someone looks
     * at the page.
     */
    public void ensureAssets() throws IOException {
        Files.createDirectories(imageFolder);
        for (String asset : ASSETS) {
            Path target = imageFolder.resolve(asset);
            if (Files.isRegularFile(target)) {
                continue;
            }
            try (InputStream in = StructureDefinitionTableRenderer.class
                    .getResourceAsStream("/ig-assets/" + asset)) {
                if (in == null) {
                    continue;
                }
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}

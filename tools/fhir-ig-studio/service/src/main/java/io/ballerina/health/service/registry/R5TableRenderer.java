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
import org.hl7.fhir.r5.conformance.profile.BindingResolution;
import org.hl7.fhir.r5.conformance.profile.ProfileKnowledgeProvider;
import org.hl7.fhir.r5.context.IWorkerContext;
import org.hl7.fhir.r5.context.SimpleWorkerContext;
import org.hl7.fhir.r5.model.ElementDefinition;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.renderers.StructureDefinitionRenderer;
import org.hl7.fhir.r5.renderers.utils.RenderingContext;
import org.hl7.fhir.utilities.MarkDownProcessor;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.hl7.fhir.utilities.validation.ValidationOptions;
import org.hl7.fhir.utilities.xhtml.XhtmlComposer;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The R5 counterpart of {@link StructureDefinitionTableRenderer}.
 *
 * <p>R5 moved element table generation out of {@code ProfileUtilities} into {@code StructureDefinitionRenderer},
 * driven by a {@link RenderingContext}, so the setup differs enough to keep it in its own class rather than
 * branching inside one. The output is the same published-IG table, and the icons come from the same bundled
 * assets.
 */
public class R5TableRenderer {

    private final FhirContext fhirContext;
    private final IWorkerContext worker;
    private final Path imageFolder;

    /**
     * @param corePackageTgz the hl7.fhir.r5.core package archive, needed to resolve base resources and
     *                       datatypes while rendering
     * @param imageFolder    the unpacked tree and type icons, shared with the R4 renderer
     */
    public R5TableRenderer(Path corePackageTgz, Path imageFolder) throws IOException {
        this.fhirContext = FhirContext.forR5();
        try (InputStream in = Files.newInputStream(corePackageTgz)) {
            // R5 has no static factory; the builder is the supported way in. Duplicates have to be allowed:
            // the published R5 core package carries the same CodeSystem under two versions, and the loader
            // rejects the whole package over it otherwise.
            this.worker = new SimpleWorkerContext.SimpleWorkerContextBuilder()
                    .withAllowLoadingDuplicates(true)
                    .fromPackage(NpmPackage.fromPackage(in));
        } catch (Exception e) {
            throw new IOException("Unable to load the FHIR R5 core definitions from " + corePackageTgz, e);
        }
        this.imageFolder = imageFolder;
    }

    public String render(String definitionJson, boolean differential) throws IOException {
        StructureDefinition profile = fhirContext.newJsonParser()
                .parseResource(StructureDefinition.class, definitionJson);
        RenderingContext context = new RenderingContext(
                worker,
                new MarkDownProcessor(MarkDownProcessor.Dialect.COMMON_MARK),
                ValidationOptions.defaults(),
                "",
                "",
                null,
                RenderingContext.ResourceRendererMode.END_USER,
                RenderingContext.GenerationRules.VALID_RESOURCE);
        context.setPkp(new LocalKnowledgeProvider());
        StructureDefinitionRenderer renderer = new StructureDefinitionRenderer(context);
        XhtmlNode table = renderer.generateTable(
                "definition.html",
                profile,
                differential,
                imageFolder.toString() + "/",
                true,               // inline the tree images as data URIs
                profile.getId(),
                !differential,
                "",                 // corePath: no links out to the FHIR spec
                "",                 // imagePath
                false,              // not a logical model
                false,              // only the invariants the profile declares
                null,
                false,
                context,
                "");
        return new XhtmlComposer(true, false).compose(table);
    }

    /** Same approach as the R4 provider: inert but well-formed links, so nothing points off this page. */
    private static final class LocalKnowledgeProvider implements ProfileKnowledgeProvider {

        @Override
        public boolean isDatatype(String name) {
            return false;
        }

        @Override
        public boolean isPrimitiveType(String name) {
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
}

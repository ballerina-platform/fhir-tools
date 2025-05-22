package org.wso2.healthcare.fhir.ballerina.packagegen.tool.modelgen;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRImplementationGuide;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.config.BallerinaPackageGenToolConfig;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.DataTypeDefinitionAnnotation;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.DatatypeTemplateContext;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.ResourceTemplateContext;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.Element;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.ExtendedElement;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.GeneratorUtils;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.AnnotationElement;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.BallerinaDataType;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Arrays;
import java.util.Iterator;
import java.util.HashSet;
import java.util.regex.Pattern;

import static org.wso2.healthcare.fhir.ballerina.packagegen.tool.ToolConstants.CONSTRAINTS_LIB_IMPORT;

public abstract class AbstractResourceContextGenerator {
    private static final Log LOG = LogFactory.getLog(AbstractResourceContextGenerator.class);
    public final Set<String> baseResources = new HashSet<>(Arrays.asList("Bundle", "OperationOutcome", "CodeSystem", "ValueSet", "DomainResource", "Resource"));
    protected ResourceTemplateContext resourceTemplateContextInstance;
    private final BallerinaPackageGenToolConfig toolConfig;
    private final Map<String, ResourceTemplateContext> resourceTemplateContextMap;
    private final Map<String, String> resourceNameTypeMap;
    private final Map<String, DatatypeTemplateContext> datatypeTemplateContextMap;
    private final Set<String> dependentIgs = new HashSet<>();

    public AbstractResourceContextGenerator(BallerinaPackageGenToolConfig config, FHIRImplementationGuide ig,
                                      Map<String, DatatypeTemplateContext> datatypeTemplateContextMap) {
        LOG.debug("Resource Context Generator Initiated");
        this.toolConfig = config;
        this.resourceTemplateContextMap = new HashMap<>();
        this.resourceNameTypeMap = new HashMap<>();
        this.datatypeTemplateContextMap = datatypeTemplateContextMap;
        populateResourceTemplateContexts(ig);
    }

    protected abstract void populateResourceTemplateContexts(FHIRImplementationGuide ig);

    protected void populateResourceElementMap(Element element) {
        if (!element.isSlice()) {
            if (element.hasChildElements()) {
                Iterator<Map.Entry<String, Element>> rootIterator = element.getChildElements().entrySet().iterator();
                Iterator<Map.Entry<String, Element>> iterator = rootIterator;
                while (iterator.hasNext()) {
                    Map.Entry<String, Element> childEntry = iterator.next();
                    if (childEntry.getValue().isSlice()) {
                        iterator.remove();
                    } else if (childEntry.getValue().hasChildElements()) {
                        rootIterator = iterator;
                        iterator = childEntry.getValue().getChildElements().entrySet().iterator();
                    } else {
                        iterator = rootIterator;
                    }
                }
            }
            // System.out.println(element.getName() + " : " + element.getDataType()); // birthdate : date
            checkAndAddConstraintImport(element);
            this.resourceTemplateContextInstance.getResourceElements().put(element.getName(), element);
        }
    }

    protected void checkAndAddConstraintImport(Element element) {
        boolean isCardinalityConstrained = (element.getMin() >= 1 && element.getMax() > 1) || (element.isArray() &&
                element.getMax() > 0 && element.getMax() < Integer.MAX_VALUE);
        boolean isConstraintsImportExists = this.resourceTemplateContextInstance.getResourceDependencies()
                .stream()
                .anyMatch(d -> d.equals(CONSTRAINTS_LIB_IMPORT));
        if (!isConstraintsImportExists && isCardinalityConstrained) {
            this.resourceTemplateContextInstance.getResourceDependencies().add(CONSTRAINTS_LIB_IMPORT);
        }
    }

    protected void markExtendedElements(Element element) {
        if (!"Extension".equals(element.getDataType())) {
            if (this.resourceTemplateContextInstance.getDifferentialElementIds().contains(element.getName())
                    || "Code".equals(element.getDataType())
                    || "BackboneElement".equals(element.getDataType())
                    || "BackboneType".equals(element.getDataType())
                    || element.hasFixedValue()) {
                element.setExtended(true);
            }
            if (element.hasChildElements()) {
                for (Map.Entry<String, Element> childEntry : element.getChildElements().entrySet()) {
                    markExtendedElements(childEntry.getValue());
                    if (childEntry.getValue().isExtended()) {
                        element.setExtended(true);
                    }
                }
            }
        }
    }

    /**
     * Populate extended elements map
     *
     * @param element resource element
     */
    protected void populateResourceExtendedElementsMap(Element element) {
        LOG.debug("Started: Resource Extended Element Map population");
        if (!element.getDataType().equals("Extension")) {
            if (element.hasChildElements()) {
                for (Map.Entry<String, Element> childEntry : element.getChildElements().entrySet()) {
                    populateResourceExtendedElementsMap(childEntry.getValue());
                }
            }
            validateAndPopulateExtendedElement(element);
        }
        LOG.debug("Ended: Resource Extended Element Map population");
    }

    /**
     * Validate and create extended elements from resource elements
     *
     * @param element resource element to be validated
     */
    private void validateAndPopulateExtendedElement(Element element) {
        LOG.debug("Started: Resource Extended Element validation");
        ExtendedElement extendedElement;
        String elementDataType = element.getDataType();

        if (elementDataType.equals("code") && element.hasChildElements()) {
            extendedElement = GeneratorUtils.getInstance().populateExtendedElement(element, BallerinaDataType.Enum, elementDataType,
                    this.resourceTemplateContextInstance.getResourceName());
            putExtendedElementIfAbsent(element, extendedElement);
        }
        else if (element.isSlice() || elementDataType.equals("BackboneElement") || elementDataType.equals("BackboneType") || (element.isExtended() && element.hasChildElements())) {
            extendedElement = GeneratorUtils.getInstance().populateExtendedElement(element, BallerinaDataType.Record, elementDataType,
                    this.resourceTemplateContextInstance.getResourceName());
            extendedElement.setElements(element.getChildElements());

            DataTypeDefinitionAnnotation annotation = new DataTypeDefinitionAnnotation();
            annotation.setName(extendedElement.getTypeName());

            if (element.hasChildElements()) {
                HashMap<String, AnnotationElement> childElementAnnotations = new HashMap<>();
                for (Element subElement : element.getChildElements().values()) {
                    checkAndAddConstraintImport(subElement);
                    AnnotationElement annotationElement = GeneratorUtils.getInstance().populateAnnotationElement(subElement);
                    childElementAnnotations.put(annotationElement.getName(), annotationElement);
                }
                annotation.setElements(childElementAnnotations);
            }
            extendedElement.setAnnotation(annotation);
            if (!element.isSlice() && this.resourceTemplateContextInstance.getSliceElements().containsKey(element.getPath())) {
                for (Element slice : this.resourceTemplateContextInstance.getSliceElements().get(element.getPath())) {
                    slice.setDataType(extendedElement.getTypeName());
                }
            }
            putExtendedElementIfAbsent(element, extendedElement);
        }
        LOG.debug("Ended: Resource Extended Element validation");
    }

    protected void putExtendedElementIfAbsent(Element element, ExtendedElement extendedElement) {
        if (extendedElement != null) {
            boolean isAlreadyExists = this.resourceTemplateContextInstance.getExtendedElements().containsKey(extendedElement.getTypeName());
            if (isAlreadyExists) {
                element.setDataType(this.resourceTemplateContextInstance.getExtendedElements().get(extendedElement.getTypeName()).getTypeName());
            } else {
                this.resourceTemplateContextInstance.getExtendedElements().put(extendedElement.getTypeName(), extendedElement);
            }
        }
    }

    protected void populateResourceElementAnnotationsMap(Element element) {
        LOG.debug("Started: Resource Element Annotation Map population");
        AnnotationElement annotationElement = GeneratorUtils.getInstance().populateAnnotationElement(element);
        this.resourceTemplateContextInstance.getResourceDefinitionAnnotation().getElements().put(element.getName(), annotationElement);
        this.resourceTemplateContextInstance.getResourceDefinitionAnnotation().getElements().put(annotationElement.getName(), annotationElement);
        LOG.debug("Ended: Resource Element Annotation Map population");
    }

    /**
     * Validates whether given string has codes
     *
     * @param string A string with/without codes delimited by pipe(|)
     * @return True or False
     */
    private boolean isCodedString(String string) {
        String[] codes = string.split(Pattern.quote("|"));
        return codes.length > 1;
    }

    public Map<String, String> getResourceNameTypeMap(){
        return resourceNameTypeMap;
    }

    protected BallerinaPackageGenToolConfig getToolConfig(){
        return toolConfig;
    }

    protected Set<String> getDependentIgs(){
        return dependentIgs;
    }

    public Map<String, ResourceTemplateContext> getResourceTemplateContextMap(){
        return resourceTemplateContextMap;
    }

    protected Map<String, DatatypeTemplateContext> getDatatypeTemplateContextMap(){
        return datatypeTemplateContextMap;
    }
}

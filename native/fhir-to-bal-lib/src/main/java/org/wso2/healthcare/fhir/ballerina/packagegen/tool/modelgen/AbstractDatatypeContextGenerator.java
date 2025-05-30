package org.wso2.healthcare.fhir.ballerina.packagegen.tool.modelgen;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.common.FHIRSpecificationData;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRDataTypeDef;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.BallerinaDataType;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.DatatypeTemplateContext;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.Element;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.ExtendedElement;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.GeneratorUtils;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractDatatypeContextGenerator {
    private static final Log LOG = LogFactory.getLog(AbstractDatatypeContextGenerator.class);
    private final Map<String, FHIRDataTypeDef> datatypeDefnMap;
    private final Map<String, DatatypeTemplateContext> dataTypeTemplateContextMap;

    public AbstractDatatypeContextGenerator(FHIRSpecificationData fhirSpecificationData) {
        this.datatypeDefnMap = fhirSpecificationData.getDataTypes();
        this.dataTypeTemplateContextMap = new HashMap<>();
        populateDatatypeContext();
    }

    public Map<String, FHIRDataTypeDef> getDatatypeDefnMap() {
        return datatypeDefnMap;
    }

    public Map<String, DatatypeTemplateContext> getDataTypeTemplateContextMap() {
        return dataTypeTemplateContextMap;
    }

    protected Map<String, FHIRDataTypeDef> getDataTypeDefnMap() {
        return datatypeDefnMap;
    }

    protected Map<String, DatatypeTemplateContext> datatypeTemplateContextMap(){
        return dataTypeTemplateContextMap;
    }

    protected void populateExtendedElementsMap(Element element, DatatypeTemplateContext context) {
        LOG.debug("Started: Resource Extended Element Map population");
        if (!element.getDataType().equals("Extension")) {
            if (element.hasChildElements()) {
                for (Map.Entry<String, Element> childEntry : element.getChildElements().entrySet()) {
                    populateExtendedElementsMap(childEntry.getValue(), context);
                }
            }
            ExtendedElement extendedElement;
            String elementDataType = element.getDataType();
            if (elementDataType.equals("code") && element.hasChildElements()) {
                extendedElement = GeneratorUtils.getInstance().populateExtendedElement(element, BallerinaDataType.Enum, elementDataType,
                        context.getName());
                context.getExtendedElements().putIfAbsent(element.getName(), extendedElement);
                element.setExtended(true);
            }
        }
        LOG.debug("Ended: Resource Extended Element Map population");
    }

    protected abstract void populateDatatypeContext();
}

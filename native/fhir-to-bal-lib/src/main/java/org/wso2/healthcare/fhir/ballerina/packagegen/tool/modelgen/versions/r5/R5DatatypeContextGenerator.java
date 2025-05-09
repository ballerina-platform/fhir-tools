package org.wso2.healthcare.fhir.ballerina.packagegen.tool.modelgen.versions.r5;

import org.apache.commons.lang.StringUtils;
import org.hl7.fhir.r5.model.ElementDefinition;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.common.FHIRSpecificationData;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRDataTypeDef;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.versions.r5.model.FHIRR5DataTypeDef;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.ToolConstants;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.AnnotationElement;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.DataTypeDefinitionAnnotation;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.DatatypeTemplateContext;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.Element;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.modelgen.AbstractDatatypeContextGenerator;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.CommonUtil;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.GeneratorUtils;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.versions.r5.R5GeneratorUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class R5DatatypeContextGenerator extends AbstractDatatypeContextGenerator {
    private static final List<String> DEFAULT_DATA_TYPES = Arrays.asList(
            "Ratio", "Period", "Range", "RatioRange", "Attachment", "Identifier", "HumanName",
            "ContactPoint", "Address", "Quantity", "Age", "Distance", "Duration", "Count",
            "MoneyQuantity", "SimpleQuantity", "SampledData", "Signature", "BackboneType",
            "Timing", "Money", "Coding", "CodeableConcept", "Annotation",
            "ContactDetail", "Contributor", "DataRequirement", "TriggerDefinition", "ExtendedContactDetail",
            "UsageContext", "VirtualServiceDetail", "MonetaryComponent", "Expression", "Availability",
            "ParameterDefinition", "RelatedArtifact",
            "CodeableReference", "Meta", "Reference", "Dosage", "ElementDefinition",
            "Extension", "Narrative", "xhtml"
    );

    public R5DatatypeContextGenerator(FHIRSpecificationData fhirSpecificationData) {
        super(fhirSpecificationData);
    }

    @Override
    protected void populateDatatypeContext() {
        for(Map.Entry<String, FHIRDataTypeDef> dataTypeDefnEntry : getDataTypeDefnMap().entrySet()){
            FHIRR5DataTypeDef datatypeDefn = (FHIRR5DataTypeDef) dataTypeDefnEntry.getValue();

            if(DEFAULT_DATA_TYPES.contains(datatypeDefn.getDefinition().getName()) ||
                "Extension".equals(datatypeDefn.getDefinition().getType())){
                continue;
            }

            DatatypeTemplateContext context = new DatatypeTemplateContext();
            String typeName = CommonUtil.getSplitTokenAt(datatypeDefn.getDefinition().getUrl(), "/", ToolConstants.TokenPosition.END);
            context.setName(GeneratorUtils.getInstance().getUniqueIdentifierFromId(typeName));
            context.setBaseDataType(datatypeDefn.getDefinition().getType());

            DataTypeDefinitionAnnotation annotation = new DataTypeDefinitionAnnotation();
            annotation.setName(datatypeDefn.getDefinition().getName());

            context.setAnnotation(annotation);

            for(ElementDefinition elementDefinition : datatypeDefn.getDefinition().getSnapshot().getElement()){
                if(elementDefinition.getPath().contains(".")){
                    String elementName = elementDefinition.getPath().substring(elementDefinition.getPath().lastIndexOf(".") + 1);

                    if ("id".equals(elementName) || "extension".equals(elementName)
                            || elementDefinition.getPath().contains(".extension.")) {
                        //skipping for generating datatype extensions
                        continue;
                    }

                    Element element = new Element();
                    element.setMax(GeneratorUtils.getMaxCardinality(elementDefinition.getMax()));
                    element.setMin(elementDefinition.getMin());
                    element.setArray(!"0".equals(elementDefinition.getBase().getMax()) && !"1".equals(elementDefinition.getBase().getMax()));

                    String typeCode = elementDefinition.getType().get(0).getCode();
                    if (GeneratorUtils.getInstance().shouldReplacedByBalType(typeCode)) {
                        element.setDataType(GeneratorUtils.getInstance().resolveDataType(typeCode));
                    } else if (ToolConstants.ELEMENT.equals(typeCode)) {
                        element.setDataType(ToolConstants.ELEMENT + CommonUtil.toCamelCase(elementName));
                    } else {
                        element.setDataType(typeCode);
                    }

                    if (elementName.endsWith("[x]") && datatypeDefn.getDefinition().getType().equals(
                            ToolConstants.DATA_TYPE_EXTENSION)) {
                        context.setBaseDataType(StringUtils.capitalize(typeCode + ToolConstants.DATA_TYPE_EXTENSION));
                    } else if ("code".equals(typeCode)) {
                        R5GeneratorUtils.populateCodeValuesForCodeElements(elementDefinition, element);
                    }

                    element.setName(GeneratorUtils.getInstance().resolveMultiDataTypeFieldNames(elementName, typeCode));
                    element.setDescription(CommonUtil.parseMultilineString(elementDefinition.getDefinition()));
                    element.setPath(elementDefinition.getPath());

                    //populate extended elements
                    populateExtendedElementsMap(element, context);

                    //populate annotations
                    AnnotationElement annotationElement = GeneratorUtils.getInstance().populateAnnotationElement(element);
                    annotation.addElement(annotationElement);
                    context.addElement(element);
                }
            }
            datatypeTemplateContextMap().putIfAbsent(datatypeDefn.getDefinition().getUrl(), context);
        }
    }
}

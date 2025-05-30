package org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.versions.r5;

import org.hl7.fhir.r5.model.ElementDefinition;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.model.Element;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.CommonUtil;
import org.wso2.healthcare.fhir.ballerina.packagegen.tool.utils.GeneratorUtils;

import java.util.HashMap;
import java.util.regex.Pattern;

public class R5GeneratorUtils extends GeneratorUtils {
    private static final R5GeneratorUtils instance = new R5GeneratorUtils();

    public static R5GeneratorUtils getInstance() {
        return instance;
    }

    /**
     * Populates available code values for a given code element.
     *
     * @param elementDefinition element definition from FHIR specification
     * @param element           element model for template context
     */
    public static void populateCodeValuesForCodeElements(ElementDefinition elementDefinition, Element element) {
        if (!elementDefinition.getShort().contains("|")) {
            return;
        }
        HashMap<String, Element> childElements = new HashMap<>();
        String[] codes = elementDefinition.getShort().split(Pattern.quote("|"));
        for (String code : codes) {
            code = CommonUtil.validateCode(code);
            if (!code.trim().isEmpty()) {
                Element childElement = new Element();
                childElement.setName(code);
                childElement.setDataType("string");
                childElement.setRootElementName(element.getName());
                childElement.setValueSet(element.getValueSet());
                childElements.put(childElement.getName(), childElement);
            }
        }
        element.setChildElements(childElements);
    }
}

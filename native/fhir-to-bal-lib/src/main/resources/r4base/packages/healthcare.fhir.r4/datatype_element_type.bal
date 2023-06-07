// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# Data type and Profile for this element
#
# + id - Unique id for inter-element referencing
# + extension - Additional content defined by implementations  
# + code - Data type or Resource (reference to definition)  
# + profile - Profiles (StructureDefinition or IG) - one must apply
# + targetProfile - Profile (StructureDefinition or IG) on the Reference/canonical target - one must apply 
# + aggregation - contained | referenced | bundled - how aggregated 
# + versioning - either | independent | specific
@DataTypeDefinition {
    name: "ElementType",
    baseType: Element,
    elements: {
        "code": {
            name: "code",
            dataType: uri,
            min: 1,
            max: 1,
            isArray: false,
            description: "Data type or Resource (reference to definition)",
            valueSet: "https://hl7.org/fhir/valueset-fhir-element-types.html"
        },
        "profile": {
            name: "profile",
            dataType: canonical,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Profiles (StructureDefinition or IG) - one must apply"
        },
        "targetProfile": {
            name: "targetProfile",
            dataType: canonical,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "Profile (StructureDefinition or IG) on the Reference/canonical target - one must apply"
        },
        "aggregation": {
            name: "aggregation",
            dataType: TypeAggregation,
            min: 0,
            max: int:MAX_VALUE,
            isArray: true,
            description: "contained | referenced | bundled - how aggregated",
            valueSet: "https://hl7.org/fhir/valueset-resource-aggregation-mode.html"
        },
        "versioning": {
            name: "versioning",
            dataType: TypeVersioning,
            min: 0,
            max: 1,
            isArray: false,
            description: "either | independent | specific",
            valueSet: "https://hl7.org/fhir/valueset-reference-version-rules.html"
        }
    },
    serializers: {
        'xml: complexDataTypeXMLSerializer,
        'json: complexDataTypeJsonSerializer
    }
}
public type ElementType record {|
    *Element;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (START)
    string id?;
    Extension[] extension?;
    //Inherited child element from "Element" (Redefining to maintain order when serialize) (END)

    uri code;
    canonical[] profile?;
    canonical[] targetProfile?;
    TypeAggregation[] aggregation?;
    TypeVersioning versioning?;
|};

public enum TypeAggregation {
    contained,
    referenced,
    bundled
}

public enum TypeVersioning {
    either,
    independent,
    specific
}

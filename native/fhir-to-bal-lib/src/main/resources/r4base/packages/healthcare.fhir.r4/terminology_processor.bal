// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

type CodeConceptDetails record {
    uri url;
    CodeSystemConcept|ValueSetComposeIncludeConcept concept;
};

# Function definition for code system finder implementations
public type CodeSystemFinder isolated function (uri system, code code) returns CodeSystem | ValueSet | FHIRError;

# A processor to process terminology data and create relevent data elements
public class TerminologyProcessor {
    
    private map<CodeSystem> codeSystems = {};
    private map<ValueSet> valueSets = {};

    public function init() {
    }

    public function addTerminology(Terminology terminology) {
        self.addCodeSystems(terminology.codeSystems);
        self.addValueSets(terminology.valueSets);
    }

    public function addCodeSystems(CodeSystem[] codeSystems) {
        foreach CodeSystem codeSystem in codeSystems {
            self.codeSystems[<string>codeSystem.url] = codeSystem;
        }
    }

    public function addValueSets(ValueSet[] valueSets) {
        foreach ValueSet valueSet in valueSets {
            self.valueSets[<string>valueSet.url] = valueSet;
        }
    }

    # Create CodeableConcept for given code in a given system
    # + system - system uri of the code system or value set
    # + code - code interested
    # + codeSystemFinder - (optional) custom code system function (utility will used this function to find code 
    #                       system in a external source system)
    # + return - Created CodeableConcept record or FHIRError if not found
    public isolated function createCodeableConcept(uri system, code code, CodeSystemFinder? codeSystemFinder = ()) 
                                                                                    returns CodeableConcept|FHIRError {
        CodeConceptDetails? conceptResult = check self.findConcept(system, code, codeSystemFinder);
        if conceptResult != () {
            return self.conceptToCodeableConcept(conceptResult.concept, conceptResult.url);
        }
        string msg = "Code :" + code + " not found in system : " + system;
        return createInternalFHIRError(msg, ERROR, PROCESSING_NOT_FOUND);
    }

    # Create Coding for given code in a given system
    # + system - system uri of the code system or value set
    # + code - code interested
    # + codeSystemFinder - (optional) custom code system function (utility will used this function to find code 
    #                       system in a external source system)
    # + return - Created CodeableConcept record or FHIRError if not found
    public  isolated function createCoding(uri system, code code, CodeSystemFinder? codeSystemFinder = ()) 
                                                                                                returns Coding|FHIRError {
        CodeConceptDetails? conceptResult = check self.findConcept(system, code, codeSystemFinder);
        if conceptResult != () {
            return self.conceptToCoding(conceptResult.concept, conceptResult.url);
        }
        string msg = "Code :" + code + " not found in system : " + system;
        return createInternalFHIRError(msg, ERROR, PROCESSING_NOT_FOUND);
    }


    private isolated function findConcept(uri system, code code, CodeSystemFinder? codeSystemFinder = ()) 
                                                                                returns (CodeConceptDetails|FHIRError)? {
        if codeSystemFinder != () {
            (CodeSystem|ValueSet) & readonly result = check codeSystemFinder(system, code).cloneReadOnly();
            if result is CodeSystem {
                return self.findConceptInCodeSystem(result, code);
            } else {
                return self.findConceptInValueSet(result, code);
            }
        } else if self.valueSets.hasKey(system) {
            return self.findConceptInValueSet(self.valueSets.get(system), code);
        } else if self.codeSystems.hasKey(system) {
            return self.findConceptInCodeSystem(self.codeSystems.get(system), code);
        } else {
            string msg = "Unknown ValueSet or CodeSystem : " + system;
            return createInternalFHIRError(msg, ERROR, PROCESSING_NOT_FOUND);
        }
    }

    # Function to find code system concept within a CodeSystem
    # + codeSystem - Target CodeSystem
    # + code - code searching for
    # + return - Code system concept found in the CodeSystem 
    private isolated function findConceptInCodeSystem(CodeSystem codeSystem, code code) returns CodeConceptDetails? {
        CodeSystemConcept[]? concepts = codeSystem.concept;
        uri? url = codeSystem.url;
        if concepts != () && url != () {
            foreach CodeSystemConcept concept in concepts {
                if concept.code == code {
                    CodeConceptDetails codeConcept = {
                        url: url,
                        concept: concept
                    }; 
                    return codeConcept;
                }
            }
        }
        return;
    }

    # Function to find code system concept within a ValueSet
    # + valueSet - Target ValueSet
    # + code - code searching for
    # + return - ValueSet/CodeSystem concept found 
    private isolated function findConceptInValueSet(ValueSet valueSet, code code) returns (CodeConceptDetails)? {
        ValueSetCompose? composeBBE = valueSet.compose;
        if composeBBE != () {
            foreach ValueSetComposeInclude includeBBE in composeBBE.include {
                uri? systemValue = includeBBE.system;

                if systemValue != () {
                    ValueSetComposeIncludeConcept[]? includeConcepts = includeBBE.concept;
                    if includeConcepts != () {
                        foreach ValueSetComposeIncludeConcept includeConcept in includeConcepts {
                            if includeConcept.code == code {
                                // found the code
                                CodeConceptDetails codeConcept = {
                                    url: systemValue,
                                    concept: includeConcept
                                };
                                return codeConcept;
                            }
                        }
                    } else {
                        // Find CodeSystem
                        if self.codeSystems.hasKey(systemValue) {
                            CodeConceptDetails? result = self.findConceptInCodeSystem(self.codeSystems.get(systemValue), code);
                            if result != () {
                                return result;
                            }
                        }
                    }
                } else {
                    // check the contents included in this value set
                    canonical[]? valueSetResult = includeBBE.valueSet;
                    if valueSetResult != () {
                        //+ Rule: A value set include/exclude SHALL have a value set or a system
                        foreach canonical valueSetEntry in valueSetResult {
                            if self.valueSets.hasKey(valueSetEntry) {
                                CodeConceptDetails? concept = self.findConceptInValueSet(self.valueSets.get(valueSetEntry), code);
                                if concept != () {
                                    return concept;
                                }
                            }
                        }
                        
                    }
                }
            }
        }
        return;
    }

    private isolated function conceptToCodeableConcept(
            CodeSystemConcept|ValueSetComposeIncludeConcept concept, uri system) returns CodeableConcept {
        Coding codingValue = {
            code: concept.code,
            system: system
        };
        string? displayValue = concept.display;
        if displayValue != () {
            codingValue.display = displayValue;
        }

        CodeableConcept cConcept = {
            coding: [
                codingValue
            ]
        };

        if concept is CodeSystemConcept {
            string? defValue = concept.definition;
            if defValue != () {
                cConcept.text = defValue;
            }
        }
        return cConcept;
    } 

    private isolated function conceptToCoding (CodeSystemConcept|ValueSetComposeIncludeConcept concept, uri system) returns Coding {

        Coding codingValue = {
            code: concept.code,
            system: system
        };
        string? displayValue = concept.display;
        if displayValue != () {
            codingValue.display = displayValue;
        }
        return codingValue;
    } 
}

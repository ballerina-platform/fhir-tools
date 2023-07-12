// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# Holds Terminology information
#
# + codeSystems - CodeSystems belong to the terminology
# + valueSets - ValueSets belong to the terminology
public type TerminologyRecord record {|
    readonly CodeSystem[] codeSystems;
    readonly ValueSet[] valueSets;
|};

# Record type ro represent terminology
public type Terminology readonly & TerminologyRecord;

# Terminology loader definition
public type TerminologyLoader distinct object {
    public function load() returns Terminology|FHIRError;
};

# An in-memory implementation of a terminology loader
public class InMemoryTerminologyLoader {
    *TerminologyLoader;
    
    final json[] codeSystems;
    final json[] valueSets;

    public function init(json[] codeSystems, json[] valueSets) {
        self.codeSystems = codeSystems;
        self.valueSets = valueSets;
    }

    # load terminology
    # + return - Terminology populated
    public function load() returns Terminology|FHIRError {
        ValueSet[] valueSetArray = [];
        foreach json jValueSet in self.valueSets {
            do {
                ValueSet valueSet = check jValueSet.cloneWithType();
                valueSetArray.push(valueSet);
            } on fail error e {
                return createFHIRError("Error occurred while type casting json value set to ValueSet type", ERROR,
                                                                    PROCESSING, diagnostic = e.message(), cause = e);
            }
        }

        CodeSystem[] codeSystemArray = [];
        foreach json jCodeSystem in self.codeSystems {
            do {
                CodeSystem codeSystem = check jCodeSystem.cloneWithType();
                codeSystemArray.push(codeSystem);
            } on fail error e {
              return createFHIRError("Error occurred while type casting json code system to CodeSystem type", ERROR,
                                                                    PROCESSING, diagnostic = e.message(), cause = e);
            }
            
        }

        Terminology terminology = {
          codeSystems: codeSystemArray.cloneReadOnly(),
          valueSets: valueSetArray.cloneReadOnly()
        };
        return terminology;
    }
}

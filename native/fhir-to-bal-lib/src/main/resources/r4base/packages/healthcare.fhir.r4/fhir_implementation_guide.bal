// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# Represents FHIR Implementation Guide holding information and functionalities bound to it
public isolated class FHIRImplementationGuide {

    private final readonly & IGInfoRecord igRecord;
    
    public isolated function init(readonly & IGInfoRecord igRecord) {
        self.igRecord = igRecord;
    }

    public isolated function getName() returns string {
        return self.igRecord.name;
    }

    public isolated function getTerminology() returns Terminology {
        return self.igRecord.terminology;
    }

    public isolated function getProfiles() returns map<Profile> {
        return self.igRecord.profiles;
    }

    public isolated function getSearchParameters() returns map<FHIRSearchParameterDefinition[]>[] {
        return self.igRecord.searchParameters;
    }
}

# Record to hold information about an implementation guide
# + title - Name for this implementation guide (human friendly)
# + name - Name for this implementation guide (computer friendly)
# + terminology - terminology object  
# + profiles - profiles supported by the IG (key : profile uri)
# + searchParameters - search parameters defined in the IG (key: parameter name)
public type IGInfoRecord record {|
    readonly & string title;
    readonly & string name;
    Terminology terminology;
    readonly & map<Profile> profiles;
    readonly & map<FHIRSearchParameterDefinition[]>[]  searchParameters;
|};

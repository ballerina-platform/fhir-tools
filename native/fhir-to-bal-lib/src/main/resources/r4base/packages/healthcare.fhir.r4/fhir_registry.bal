// Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.

// This software is the property of WSO2 LLC. and its suppliers, if any.
// Dissemination of any information or reproduction of any material contained
// herein is strictly forbidden, unless permitted by WSO2 in accordance with
// the WSO2 Software License available at: https://wso2.com/licenses/eula/3.2
// For specific language governing the permissions and limitations under
// this license, please see the license as well as any agreement you’ve
// entered into with WSO2 governing the purchase of this software and any
// associated services.

# Hold FHIR related information in a particular deployment
public isolated class FHIRRegistry {

    private FHIRImplementationGuide[] implementationGuides = [];

    // profile map (key: profile url)
    private map<readonly & Profile> profileMap = {};

    // maintain resource type to profiles mapping
    private map<map<Profile>> resourceTypeProfiles = {};

    // resource type to profile of FHIR Base resources
    private map<readonly & Profile> fhirBaseIGProfiles = {};

    // search parameter map (key: resource type)
    private map<SearchParamCollection> searchParameterMap = {};

    public function init() {
    }

    public function addImplementationGuide(FHIRImplementationGuide ig) returns FHIRError? {
        lock {
            self.implementationGuides.push(ig);
        }
        
        lock {
            // add profiles to profile map
            foreach Profile profile in ig.getProfiles() {
                readonly & Profile profileClone = profile.cloneReadOnly();
                self.profileMap[profileClone.url] = profileClone;

                // Add to resource type bound profile mapping
                //ResourceDefinitionRecord resourceDefinition = check getResourceDefinition(profileClone.modelType);
                map<Profile> profiles;
                if self.resourceTypeProfiles.hasKey(profileClone.resourceType) {
                    profiles = self.resourceTypeProfiles.get(profileClone.resourceType);
                } else {
                    profiles = {};
                    self.resourceTypeProfiles[profileClone.resourceType] = profiles;
                }
                profiles[profileClone.url] = profileClone;

                // If the processed IG is FHIR base IG, we need to add it to FHIR base profile map
                if ig.getName() == FHIR_BASE_IG {
                    //ResourceDefinitionRecord resourceDef = check getResourceDefinition(profile.modelType);
                    self.fhirBaseIGProfiles[profileClone.resourceType] = profileClone;
                }
            }
        }

        lock {
            // Add search parameters
            foreach map<FHIRSearchParameterDefinition[]> paramsMap in ig.getSearchParameters() {    
                foreach FHIRSearchParameterDefinition[] params in paramsMap {
                    foreach FHIRSearchParameterDefinition param in params {
                        foreach string resourceType in param.base {
                            if (self.searchParameterMap.hasKey(resourceType)) {
                                SearchParamCollection collection = self.searchParameterMap.get(resourceType);
                                if !collection.hasKey(param.name) {
                                    collection[param.name] = param;
                                }
                            } else {
                                SearchParamCollection collection = {};
                                collection[param.name] = param;
                                self.searchParameterMap[resourceType] = collection;
                            }
                        }
                    }
                }
            }
        }

        // Update terminology processor
        terminologyProcessor.addTerminology(ig.getTerminology());
    }


    public isolated function getResourceProfiles(string resourceType) returns readonly & map<Profile & readonly> {
        lock {
            return self.resourceTypeProfiles.get(resourceType).cloneReadOnly();
        }
    }

    public isolated function getResourceSearchParameters(string resourceType) returns SearchParamCollection {
        lock {
            if self.searchParameterMap.hasKey(resourceType) {
                return self.searchParameterMap.get(resourceType).cloneReadOnly();
            }
        }
        return {};
    }

    public isolated function getResourceSearchParameterByName(string resourceType, string name) returns FHIRSearchParameterDefinition? {
        lock {
            if self.searchParameterMap.hasKey(resourceType) && self.searchParameterMap.get(resourceType).hasKey(name) {
                return self.searchParameterMap.get(resourceType).get(name).clone();
            }
        }
        return ();
    }

    public isolated function findProfile(string url) returns (readonly & Profile)? {
        lock {
            if self.profileMap.hasKey(url) {
                return self.profileMap.get(url);
            }
        }
        return ();
    }

    public isolated function findBaseProfile(string resourceType) returns (readonly & Profile)? {
        lock {
            if self.fhirBaseIGProfiles.hasKey(resourceType) {
                return self.fhirBaseIGProfiles.get(resourceType);
            }
        }
        return ();
    }

    public isolated function isSupportedResource (string resourceType) returns boolean {
        lock {
            return self.resourceTypeProfiles.hasKey(resourceType);
        }
    }

}


# Search parameter map (key: parameter name)
public type SearchParamCollection map<FHIRSearchParameterDefinition>;

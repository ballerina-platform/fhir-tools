import ballerinax/health.clients.fhir as fhirClient;
// helper function to add only non-empty params 
isolated function addIfPresent(fhirClient:SearchParameters m, string key, string?|string[]? val) { 
    if val is string { 
        m[key] = val; 
    } 
    else if val is string[] { 
        if val.length() > 0 { 
            m[key] = val; 
        } 
    }
}


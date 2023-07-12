# FhirPathError is the error object that is returned when an error occurs during the evaluation of a FHIRPath expression.
public type FHIRPathError distinct error;

# Method to create a FHIRPathError
#
# + errorMsg - the reason for the occurence of error
# + fhirPath - the fhirpath expression that is being evaluated
# + return - the error object
public isolated function createFhirPathError(string errorMsg, string? fhirPath) returns error {
    FHIRPathError fhirPathError = error(errorMsg, fhirpath = fhirPath);
    return fhirPathError;
}

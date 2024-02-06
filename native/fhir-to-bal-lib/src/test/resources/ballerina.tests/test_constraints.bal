import ballerina/constraint;
import ballerina/test;

@test:Config {}
function testResourceElementConstraints() {
    USCoreCareTeam careTeam = {
        subject: {
            reference: "Patient/123"
        }, 
        participant: []
    };
    USCoreCareTeam|constraint:Error validate = constraint:validate(careTeam, USCoreCareTeam);
    if validate is USCoreCareTeam {
        test:assertFail("careTeam should not be valid");
    } else {
        test:assertTrue(true, "careTeam is not valid");
    }
}

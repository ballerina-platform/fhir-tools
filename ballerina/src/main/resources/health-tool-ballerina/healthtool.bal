# This implementation will not be used in general scenarios.
#
# + param - param as a string
# + return - "Hello, " with the input string name
public function healthcheck(string param) returns string {
    if !(param is "") {
        return "Input is, " + param;
    }
    return "Healthcheck is OK";
}

#!/bin/sh
# Starts FHIR IG Studio. It runs from this folder because that is where it finds its UI (ui/dist) and its
# libraries (service-lib/). Arguments are passed to Java, e.g. ./run.sh -Dfhir.tool.service.port=9096

cd "$(dirname "$0")" || exit 1

if ! command -v java >/dev/null 2>&1; then
    echo "FHIR IG Studio needs Java 21 or later, and no 'java' was found on the PATH." >&2
    echo "Install one, for example from https://adoptium.net/, and run this again." >&2
    exit 1
fi

# "21.0.4" -> 21, "1.8.0_402" -> 1: anything before 21 cannot load the service's classes.
java_major=$(java -version 2>&1 | head -n 1 | sed -E 's/.*version "([0-9]+).*/\1/')
case "$java_major" in
    ''|*[!0-9]*) ;;
    *)
        if [ "$java_major" -lt 21 ]; then
            echo "FHIR IG Studio needs Java 21 or later; the 'java' on the PATH is version $java_major." >&2
            exit 1
        fi
        ;;
esac

# JAVA_OPTS is split on spaces on purpose, so it can carry several options.
# shellcheck disable=SC2086
exec java $JAVA_OPTS "$@" -jar health-service.jar

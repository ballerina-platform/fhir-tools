@echo off
rem Starts FHIR IG Studio. It runs from this folder because that is where it finds its UI (ui\dist) and its
rem libraries (service-lib\). Arguments are passed to Java, e.g. run.bat -Dfhir.tool.service.port=9096

cd /d "%~dp0"

where java >nul 2>nul
if errorlevel 1 (
    echo FHIR IG Studio needs Java 21 or later, and no 'java' was found on the PATH.
    echo Install one, for example from https://adoptium.net/, and run this again.
    exit /b 1
)

java %JAVA_OPTS% %* -jar health-service.jar

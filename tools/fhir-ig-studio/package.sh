#!/usr/bin/env bash
#
# Builds FHIR IG Studio and packages it as a zip anyone with Java 21 can unzip and run:
#
#   build/fhir-ig-studio-<version>.zip
#     fhir-ig-studio-<version>/
#       health-service.jar   the service, with its classpath pointing at service-lib/
#       service-lib/         its dependencies, the fhir-tools codegen jars among them
#       ui/dist/             the built UI, which the service finds relative to where it is started
#       run.sh, run.bat      start it from that folder
#       README.md            how to run and configure it
#
# The version is the service's own, from service/pom.xml. Any arguments are passed to Maven, e.g. a settings
# file:  ./package.sh -s ci-settings.xml
#
# Used as is by .github/workflows/fhir-ig-studio-release.yml, so a local run produces what a release ships.

set -euo pipefail
cd "$(dirname "$0")"

version=$(mvn -q -f service/pom.xml "$@" help:evaluate -Dexpression=project.version -DforceStdout)
name="fhir-ig-studio-${version}"
out="build/${name}"

echo "==> Building the UI"
(cd ui && npm ci && npm run build)

echo "==> Building the service ${version}"
mvn -B -f service/pom.xml "$@" clean package

echo "==> Assembling ${out}"
# Only this version's output: anything else under build/, such as an unzipped copy being tried out, stays.
rm -rf "${out}" "build/${name}.zip"
mkdir -p "${out}/ui"
cp service/target/health-service.jar "${out}/"
cp -R service/target/service-lib "${out}/"
cp -R ui/dist "${out}/ui/dist"
cp packaging/run.sh packaging/run.bat packaging/README.md "${out}/"
chmod +x "${out}/run.sh"

(cd build && zip -qr "${name}.zip" "${name}")
echo "==> build/${name}.zip"

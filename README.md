# Health Tools

## Building form Source
### Prerequisites
- [Java 17](https://openjdk.org/projects/jdk/17/)
- [Ballerina Swan Lake Update 8](https://ballerina.io/downloads/) (v2201.8.1)
- Add your Github Personal Access Token to the `.m2/settings.xml` to get access the `ballerina-lang` dependencies.
  (The access token should have the sufficient read access and make sure to have the id as `ballerina-language-repo` setting.xml file)
```xml
<servers>
    <server>
        <id>ballerina-language-repo</id>
        <username>{Github_username}</username>
        <password>{Github_PAT}</password>
    </server>
</servers>
```
- Build the [Codegen tool framework](https://github.com/wso2/open-healthcare-codegen-tool-framework) repo locally. 
  This will add the required dependencies to your local maven repository.
### Build
```shell
 mvn clean install
```

## Supported Commands
```shell
bal health fhir -m package -o output-dir spec-path
bal health fhir -m package --package-name my.package.name -o output-dir spec-path
bal health fhir -m template -o output-dir spec-path
bal health fhir -m template -o output-dir
bal health fhir -m template --ig hl7.fhir.us.core@6.1.0 -o output-dir
bal health fhir -m package --package-name my.package --ig hl7.fhir.us.core@6.1.0 -o output-dir
bal health fhir -m connector --config configuration-file-path -o output-dir
```
### Note
- `spec-path` is optional when `--ig` is provided or when template mode should use the default HL7 R4 core IG (`hl7.fhir.r4.core` from [packages.fhir.org](https://packages.fhir.org), extracted under `spec/international`).
- When the version is omitted from `--ig` (e.g. `--ig hl7.fhir.us.core`), the CLI lists published versions interactively from a terminal, and automatically selects `dist-tags.latest` when no console is available (e.g. in CI).
- With a local layout, `spec-path` points to FHIR specification folders (Implementation Guide JSON files).

Directory structure of the `spec-path` should be as follows.
```shell
└── spec-path
    ├── AU-Base
    │   ├── CodeSystem-au-body-site.json
    │   ├── CodeSystem-au-location-physical-type.json
    │   ├── StructureDefinition-ahpraprofession-details.json
    │   ├── StructureDefinition-ahpraregistration-details.json
    │   ├── ValueSet-contact-purpose.json
    │   └── ValueSet-contact-Relationship-Type.json
    ├── CarinBB
    ├── international
    └── USCore
```

# Health Tools

## Building form Source
### Prerequisites
- [Java 11](https://openjdk.org/projects/jdk/11/) (v11.0.19)
- [Ballerina Swan Lake Update 7](https://ballerina.io/downloads/) (v2201.7.0)
- Add your Github Personal Access Token to the `.m2/settings.xml` to get access the `ballerina-lang` dependencies.
  (Make sure to have the id as `ballerina-language-repo`)
```xml
<servers>
    <server>
        <id>ballerina-language-repo</id>
        <username>{Github_username}</username>
        <password>{Github_PAT}</password>
    </server>
</servers>
```
### Build
```shell
 mvn clean install
```

## Supported Commands
```shell\
bal health fhir -m package -o output-dir spec-path
bal health fhir -m package --package-name my.package.name -o output-dir spec-path
bal health fhir -m template -o output-dir spec-path
```
### Note
- `spec-path` is the path to the FHIR specifications. (i.e. In the specified path, there should be folder/s for each 
Implementation Guide containing the FHIR specification files.)

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

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

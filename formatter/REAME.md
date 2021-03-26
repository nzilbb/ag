# Formatters

Modules that convert annotation graphs to and from various tool-specific formats.

## Creating a new formatter

Usually the formatter namespace is named after the tool and the module class is named after
the file extension, e.g `nzilbb.formatter.praat.TextGridSerialization`

1. In this directory, run the following command (change *myformatter* to the required name):
   ```
   mvn archetype:generate \
     -DgroupId=nz.ac.canterbury.nzilbb \
     -DartifactId=myformatter \
     -DarchetypeArtifactId=maven-archetype-quickstart \
     -DarchetypeVersion=1.4 \
     -DinteractiveMode=false
   ```
2. In *myformatter/pom.xml*:
   - prefix the *artifactId* and *name* with "nzilbb.formatter."
   - change *version* to "0.1.0-SNAPSHOT" - i.e. use semantic versioning.
   - remove the *url* tag (so it can be inherited from the master pom.xml)
   - change *maven.compiler.source* and *maven.compiler.target* to "1.8"
   - add the following to *dependencies*
   ```
    <dependency>
      <groupId>nz.ac.canterbury.nzilbb</groupId>
      <artifactId>nzilbb.ag</artifactId>
      <version>0.1.0-SNAPSHOT</version>
      <scope>compile</scope>
    </dependency>
   ```
3. Remove the groupId-based source code structure:
   ```
   cd myformatter
   rm -r src/main/java/nz
   rm -r src/test/java/nz
   ```
4. Add directory structure for the formatter code:
   ```
   mkdir src/main/java/nzilbb
   mkdir src/main/java/nzilbb/formatter
   mkdir src/main/java/nzilbb/formatter/myformatter
   mkdir src/test/java/nzilbb
   mkdir src/test/java/nzilbb/formatter
   mkdir src/test/java/nzilbb/formatter/myformatter
   ```
5. Add your implementation to *myformatter/src/main/java/nzilbb/formatter/myformatter*
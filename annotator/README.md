# Annotators

Modules that perform automatic annotation tasks on annotation graphs.

## Creating a new annotator

1. In this directory, run the following command (change *myannotator* to the required name):
   ```
   mvn archetype:generate \
     -DgroupId=nz.ac.canterbury.nzilbb \
     -DartifactId=myannotator \
     -DarchetypeArtifactId=maven-archetype-quickstart \
     -DarchetypeVersion=1.4 \
     -DinteractiveMode=false
   ```
2. In *myannotator/pom.xml*:
   - prefix the *artifactId* and *name* with "nzilbb.annotator."
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
   cd myannotator
   rm -r src/main/java/nz
   rm -r src/test/java/nz
   ```
4. Add directory structure for the annotator code:
   ```
   mkdir src/main/java/nzilbb
   mkdir src/main/java/nzilbb/annotator
   mkdir src/main/java/nzilbb/annotator/myannotator
   mkdir src/test/java/nzilbb
   mkdir src/test/java/nzilbb/annotator
   mkdir src/test/java/nzilbb/annotator/myannotator
   ```
5. Add your implementation to *myannotator/src/main/java/nzilbb/annotator/myannotator*
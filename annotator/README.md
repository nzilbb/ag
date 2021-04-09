# Annotators

Modules that perform automatic annotation tasks on annotation graphs.

## Creating a new annotator

1. In this directory, run the following command (change *myannotator* to the required name):
   ```
   mvn archetype:generate \
     -DgroupId=nzilbb \
     -DartifactId=myannotator \
     -DarchetypeArtifactId=maven-archetype-quickstart \
     -DarchetypeVersion=1.4 \
     -DinteractiveMode=false
   ```
2. In *myannotator/pom.xml*:
   - prefix the *artifactId* and *name* with "nzilbb.annotator."
   - change *version* to "0.1.0" - i.e. use semantic versioning.
   - remove the *url* tag (so it can be inherited from the master pom.xml)
   - add the following *properties*
   ```
    <package.path>nzilbb/annotator/myannotator/</package.path>
    <annotator.class>nzilbb.annotator.myannotator.MyAnnotator</annotator.class>
   ```
   - change *maven.compiler.source* and *maven.compiler.target* to "1.8"
   - add the following to *dependencies*
   ```
    <dependency>
      <groupId>nzilbb</groupId>
      <artifactId>nzilbb.ag</artifactId>
      <version>[1.0.0,)</version>
      <scope>compile</scope>
    </dependency>
   ```
   - add the following to *build*
   ```
    <plugins>
      <!-- add manifest attributes required for identifying the annotator class -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jar-plugin</artifactId>
        <configuration>
          <archive>
            <manifest addDefaultImplementationEntries="true" />
            <manifestEntries>
              <nzilbb-ag-automation-Annotator>${annotator.class}</nzilbb-ag-automation-Annotator>
              <Name>${package.path}</Name>
              <Implementation-Title>${project.description}</Implementation-Title>
              <Implementation-Version>${project.version}</Implementation-Version>
              <Implementation-Vendor>New Zealand Institute of Language, Brain and Behaviour</Implementation-Vendor>
            </manifestEntries>
          </archive>
        </configuration>
      </plugin>
    </plugins>
   ```
3. Remove the groupId-based source code structure:
   ```
   cd myannotator
   rm src/main/java/nzilbb/App.java
   rm src/test/java/nzilbb/AppTest.java
   ```
4. Add directory structure for the annotator code:
   ```
   mkdir src/main/java/nzilbb/annotator
   mkdir src/main/java/nzilbb/annotator/myannotator
   mkdir src/test/java/nzilbb/annotator
   mkdir src/test/java/nzilbb/annotator/myannotator
   ```
5. Add your implementation to *myannotator/src/main/java/nzilbb/annotator/myannotator*
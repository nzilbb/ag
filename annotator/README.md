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
   - add a *description* tag describing what the annotator does.
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
      <version>[1.0.6,)</version>
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
          <outputDirectory>../../bin</outputDirectory>
        </configuration>
      </plugin>
    </plugins>
   ```
3. Remove the groupId-based source code structure:
   ```
   cd myannotator
   rm src/main/java/nzilbb/App.java src/test/java/nzilbb/AppTest.java
   ```
4. Add directory structure for the annotator code:
   ```
   mkdir src/main/java/nzilbb/annotator
   mkdir src/main/java/nzilbb/annotator/myannotator
   mkdir src/main/resources
   mkdir src/main/resources/nzilbb
   mkdir src/main/resources/nzilbb/annotator
   mkdir src/main/resources/nzilbb/annotator/myannotator
   mkdir src/test/java/nzilbb/annotator
   mkdir src/test/java/nzilbb/annotator/myannotator
   ```
5. Add your implementation to *myannotator/src/main/java/nzilbb/annotator/myannotator*

## Testing web-apps

To test the annotator's info document, use the following command:

```
java -cp ~/.m2/repository/nzilbb/nzilbb.ag/1.0.3/nzilbb.ag-1.0.3.jar:~/.m2/repository/org/glassfish/javax.json/1.1.4/javax.json-1.1.4.jar nzilbb.ag.automation.util.Info annotator.jar
```

To test the installation/configuration web-app, use the following command:

```
java -cp ~/.m2/repository/nzilbb/nzilbb.ag/1.0.3/nzilbb.ag-1.0.3.jar:~/.m2/repository/org/glassfish/javax.json/1.1.4/javax.json-1.1.4.jar:~/.m2/repository/commons-fileupload/commons-fileupload/1.4/commons-fileupload-1.4.jar:~/.m2/repository/commons-io/commons-io/2.6/commons-io-2.6.jar  nzilbb.ag.automation.util.ConfigApp annotator.jar
```

To test the task parameters web-app, use the following command:

```
java -cp ~/.m2/repository/nzilbb/nzilbb.ag/1.0.3/nzilbb.ag-1.0.3.jar:~/.m2/repository/org/glassfish/javax.json/1.1.4/javax.json-1.1.4.jar:~/.m2/repository/commons-fileupload/commons-fileupload/1.4/commons-fileupload-1.4.jar:~/.m2/repository/commons-io/commons-io/2.6/commons-io-2.6.jar  nzilbb.ag.automation.util.TaskApp annotator.jar taskId
```
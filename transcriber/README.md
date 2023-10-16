# Transcribers

Modules that perform automatic transcription of speech recordings using ASR/STT.

## Creating a new transcriber

1. In this directory, run the following command (change *myannotator* to the required name):
   ```
   mvn archetype:generate \
     -DgroupId=nzilbb \
     -DartifactId=mytranscriber \
     -DarchetypeArtifactId=maven-archetype-quickstart \
     -DarchetypeVersion=1.4 \
     -DinteractiveMode=false
   ```
2. In *myannotator/pom.xml*:
   - prefix the *artifactId* and *name* with "nzilbb.transcriber."
   - change *version* to "0.1.0" - i.e. use semantic versioning.
   - add a *description* tag describing what the transcriber does.
   - remove the *url* tag (so it can be inherited from the master pom.xml)
   - add the following *properties*
   ```
    <package.path>nzilbb/transcriber/mytranscriber/</package.path>
    <annotator.class>nzilbb.transcriber.mytranscriber.MyTranscriber</annotator.class>
   ```
   - change *maven.compiler.source* and *maven.compiler.target* to "1.8"
   - add the following to *dependencies*
   ```
    <dependency>
      <groupId>nzilbb</groupId>
      <artifactId>nzilbb.ag</artifactId>
      <version>[1.0.7,)</version>
      <scope>compile</scope>
    </dependency>
   ```
   - add the following to *build*
   ```
    <plugins>
      <!-- add manifest attributes required for identifying the transcriber class -->
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
   cd mytranscriber
   rm src/main/java/nzilbb/App.java src/test/java/nzilbb/AppTest.java
   ```
4. Add directory structure for the transcriber code:
   ```
   mkdir src/main/java/nzilbb/transcriber
   mkdir src/main/java/nzilbb/transcriber/mytranscriber
   mkdir src/main/resources
   mkdir src/main/resources/nzilbb
   mkdir src/main/resources/nzilbb/transcriber
   mkdir src/main/resources/nzilbb/transcriber/mytranscriber
   mkdir src/test/java/nzilbb/transcriber
   mkdir src/test/java/nzilbb/transcriber/mytranscriber
   ```
5. Add your implementation to *mytranscriber/src/main/java/nzilbb/transcriber/mytranscriber*

## Testing web-apps

To test the transcriber's info document, use the following command:

```
java -cp nzilbb.ag.jar:~/.m2/repository/org/glassfish/javax.json/1.1.4/javax.json-1.1.4.jar nzilbb.ag.automation.util.Info nzilbb.transcriber.mytranscriber-0.1.0
```

To test the installation/configuration web-app, use the following command:

```
java -cp nzilbb.ag.jar:~/.m2/repository/org/glassfish/javax.json/1.1.4/javax.json-1.1.4.jar:~/.m2/repository/commons-fileupload/commons-fileupload/1.4/commons-fileupload-1.4.jar:~/.m2/repository/commons-io/commons-io/2.6/commons-io-2.6.jar  nzilbb.ag.automation.util.ConfigApp nzilbb.transcriber.mytranscriber-0.1.0.jar
```

To test the transcriber, use the following command:

```
java -cp nzilbb.ag.jar"~/.m2/repository/org/glassfish/javax.json/1.1.4/javax.json-1.1.4.jar nzilbb.ag.automation.util.Transcribe nzilbb.transcriber.mytranscriber-0.1.0 speech.wav
```

This will output a JSON representation of the transcript annotation graph.


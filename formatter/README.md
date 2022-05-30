# Formatters

Modules that convert annotation graphs to and from various tool-specific formats.

## Creating a new formatter

Usually the formatter namespace is named after the tool and the module class is named after
the file extension, e.g `nzilbb.formatter.praat.TextGridSerialization`

1. In this directory, run the following command (change *myformatter* to the required name):
   ```
   mvn archetype:generate \
     -DgroupId=nzilbb \
     -DartifactId=myformatter \
     -DarchetypeArtifactId=maven-archetype-quickstart \
     -DarchetypeVersion=1.4 \
     -DinteractiveMode=false
   ```
2. In *myformatter/pom.xml*:  
   - prefix the *artifactId* and *name* with "nzilbb.formatter."
   - change *version* to "0.1.0" - i.e. use semantic versioning.
   - remove the *url* tag (so it can be inherited from the master pom.xml)
   - change *maven.compiler.source* and *maven.compiler.target* to "1.8"
   - add the following to *properties*
   ```
    <package.path>nzilbb/formatter/myformatter/</package.path>
    <formatter.class>nzilbb.formatter.myformatter.MyFormatterClass</formatter.class>
   ```
   - add the following to *dependencies*
   ```
    <dependency>
      <groupId>nzilbb</groupId>
      <artifactId>nzilbb.ag</artifactId>
      <version>[1.0.0,)</version>
      <scope>compile</scope>
    </dependency>
   ```
   - add the following to *build*:
   ```
    <plugins>
      <!-- add manifest attributes required for identifying the formatter class -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jar-plugin</artifactId>
        <configuration>
          <archive>
            <manifest addDefaultImplementationEntries="true" />
            <manifestEntries>
              <nzilbb-ag-serialize-GraphSerializer>${formatter.class}</nzilbb-ag-serialize-GraphSerializer>
              <nzilbb-ag-serialize-GraphDeserializer>${formatter.class}</nzilbb-ag-serialize-GraphDeserializer>
              <Name>${package.path}</Name>
              <Implementation-Title>${project.description}</Implementation-Title>
              <Implementation-Version>${project.version}</Implementation-Version>
              <Implementation-Vendor>New Zealand Institute of Language, Brain and Behaviour</Implementation-Vendor>
            </manifestEntries>
          </archive>
          <outputDirectory>../../bin</outputDirectory>
        </configuration>
      </plugin>
      <plugin>
        <artifactId>maven-surefire-plugin</artifactId>
        <configuration>
          <trimStackTrace>false</trimStackTrace>
        </configuration>
      </plugin>
    </plugins>    
   ```
3. Add directory structure for the formatter code:
   ```
   rm src/main/java/nzilbb.App.java
   mkdir src/main/java/nzilbb/formatter
   mkdir src/main/java/nzilbb/formatter/myformatter
   mkdir src/test/java/nzilbb/formatter
   mkdir src/test/java/nzilbb/formatter/
   ```
4. Add your implementation to *myformatter/src/main/java/nzilbb/formatter/myformatter*
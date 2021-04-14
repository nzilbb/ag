# Converters

Standalone programs that convert transcripts from one tool format to another, e.g.

There are a number converter utilities that can be used to convert files from one format
to another, including:

* vtt - *web subtitles (Web VTT)*
* eaf - *ELAN* files
* trs - *Transcriber* transcripts
* pdf - *PDF* files
* txt - *plain text* files
* slt - *SALT* transcripts

| from↓ to→ | trs | eaf  | textgrid | vtt | pdf | txt |
| --- | :---: | :---:  | :---: | :---: | :---: | :---: |
| **trs** | | [trs-to-eaf](../bin/trs-to-eaf.jar?raw=true) | [trs-to-textgrid](../bin/trs-to-textgrid.jar?raw=true) | [trs-to-vtt](../bin/trs-to-vtt.jar?raw=true) | [trs-to-pdf](../bin/trs-to-pdf.jar?raw=true) | [trs-to-txt](../bin/trs-to-txt.jar?raw=true) |
| **eaf** | [eaf-to-trs](../bin/eaf-to-trs.jar?raw=true) | | | | |
| **vtt** | [vtt-to-trs](../bin/vtt-to-trs.jar?raw=true) | [vtt-to-eaf](../bin/vtt-to-eaf.jar?raw=true) | [vtt-to-textgrid](../bin/vtt-to-textgrid.jar?raw=true) | | |
| **slt** | | [slt-to-eaf](../bin/slt-to-eaf.jar?raw=true) | | | | |

These use the serializers/deserializers in the *formatters* directory to read a file in
one format, convert it to an annotation graph, and then write that graph out as a file in
another format.

## Creating a new converter

1. In this directory, run the following command (change *myannotator* to the required name):
   ```
   mvn archetype:generate \
     -DgroupId=nzilbb \
     -DartifactId=informattooutformat \
     -DarchetypeArtifactId=maven-archetype-quickstart \
     -DarchetypeVersion=1.4 \
     -DinteractiveMode=false
   ```
2. In *informattooutformat/pom.xml*:
   - set the *artifactId* as "informat-to-outformat"
   - prefix the *name* with "nzilbb.converter."
   - change *version* to "0.1.0" - i.e. use semantic versioning.
   - remove the *url* tag (so it can be inherited from the master pom.xml)
   - change *maven.compiler.source* and *maven.compiler.target* to "1.8"
   - add the following to *properties*
   ```    
    <input.package>nzilbb.formatter.informattool</input.package>
    <input.path>nzilbb/formatter/informattool/</input.path>
    <input.version>i.i.i</input.version>
    
    <output.package>nzilbb.formatter.outformattool</output.package>
    <output.path>nzilbb/formatter/outformattool/</output.path>
    <output.version>o.o.o</output.version>    
   ```
   - add the following to *dependencies*
   ```
    <dependency>
      <groupId>nzilbb</groupId>
      <artifactId>converter-base</artifactId>
      <version>[1.0.0,)</version>
    </dependency>
    <dependency>
      <groupId>nzilbb</groupId>
      <artifactId>${input.package}</artifactId>
      <version>[${input.version},)</version>
    </dependency>
    <dependency>
      <groupId>nzilbb</groupId>
      <artifactId>${output.package}</artifactId>
      <version>[${output.version},)</version>
    </dependency>
   ```
   - add the following to *build*
   ```
    <plugins>
      <plugin>
        <artifactId>maven-jar-plugin</artifactId>
        <version>3.0.2</version>
        <configuration>
          <archive>
            <manifest>
              <mainClass>nzilbb.converter.InformatToOutformat</mainClass>
            </manifest>
            <!-- ensure version is available to the serializations -->
            <manifestSections>
              <manifestSection>
                <name>${input.path}</name>
                <manifestEntries>
                  <Implementation-Title>${project.description}</Implementation-Title>
                  <Implementation-Version>${input.version}</Implementation-Version>
                  <Implementation-Vendor>New Zealand Institute of Language, Brain and Behaviour</Implementation-Vendor>
                </manifestEntries>
              </manifestSection>
              <manifestSection>
                <name>${output.path}</name>
                <manifestEntries>
                  <Implementation-Title>${project.description}</Implementation-Title>
                  <Implementation-Version>${output.version}</Implementation-Version>
                  <Implementation-Vendor>New Zealand Institute of Language, Brain and Behaviour</Implementation-Vendor>
                </manifestEntries>
              </manifestSection>
            </manifestSections>
          </archive>
          <outputDirectory>../../bin</outputDirectory>
        </configuration>
      </plugin>
      <!-- include all the necessary dependencies in our jar for it to run standalone -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-dependency-plugin</artifactId>
        <executions>
          <execution>
            <id>unpack</id>
            <phase>generate-sources</phase>
            <goals>
              <goal>unpack</goal>
            </goals>
            <configuration>
              <artifactItems>
                <artifactItem>
                  <groupId>nzilbb</groupId>
                  <artifactId>${input.package}</artifactId>
                  <version>${input.version}</version>
                  <outputDirectory>${project.build.directory}/classes</outputDirectory>
                </artifactItem>
                <artifactItem>
                  <groupId>nzilbb</groupId>
                  <artifactId>${output.package}</artifactId>
                  <version>${output.version}</version>
                  <outputDirectory>${project.build.directory}/classes</outputDirectory>
                </artifactItem>
                <artifactItem>
                  <groupId>nzilbb</groupId>
                  <artifactId>converter-base</artifactId>
                  <version>1.0.1</version>
                  <outputDirectory>${project.build.directory}/classes</outputDirectory>
                </artifactItem>
                <artifactItem>
                  <groupId>nzilbb</groupId>
                  <artifactId>nzilbb.ag</artifactId>
                  <version>n.n.n</version>
                  <outputDirectory>${project.build.directory}/classes</outputDirectory>
                </artifactItem>
                <artifactItem>
                  <groupId>org.glassfish</groupId>
                  <artifactId>javax.json</artifactId>
                  <version>1.1.4</version>
                  <outputDirectory>${project.build.directory}/classes</outputDirectory>
                </artifactItem>
              </artifactItems>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
   ```
3. Remove the groupId-based source code structure:
   ```
   cd informattooutformat
   rm src/main/java/nzilbb/App.java
   rm src/test/java/nzilbb/AppTest.java
   ```
4. Add directory structure for the annotator code:
   ```
   mkdir src/main/java/nzilbb/converter
   mkdir src/test/java/nzilbb/converter
   ```
5. Add your implementation to *informattooutformat/src/main/java/nzilbb/converter/InformatToOutformat.java
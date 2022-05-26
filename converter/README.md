# Converters

Standalone programs that convert transcripts from one tool format to another, e.g.

* trs - [Transcriber](http://trans.sourceforge.net/en/presentation.php) transcripts
* eaf - [ELAN](https://archive.mpi.nl/tla/elan) files
* vtt - [web subtitles (Web VTT)](https://en.wikipedia.org/wiki/WebVTT)
* slt - [SALT](https://www.saltsoftware.com/) transcripts
* cha - [CLAN](https://dali.talkbank.org/clan/) CHAT transcripts
* textgrid - [Praat](https://praat.org) TextGrids
* pdf - *PDF* files
* tex - *LaTeX* files
* txt - *plain text* files
* kaldi - input files for the [Kaldi](https://kaldi-asr.org/) automatic speech recognition training system

| to↓ from→ | trs | eaf  | vtt | slt | cha | textgrid | txt |
| --- | :---: | :---:  | :---: | :---: | :---: | :---: | :---: |
| **trs** | | [eaf-to-trs](../bin/eaf-to-trs.jar?raw=true) | [vtt-to-trs](../bin/vtt-to-trs.jar?raw=true) | [slt-to-trs](../bin/slt-to-trs.jar?raw=true) | [cha-to-trs](../bin/cha-to-trs.jar?raw=true) | [textgrid-to-trs](../bin/textgrid-to-trs.jar?raw=true) | |
| **eaf** | [trs-to-eaf](../bin/trs-to-eaf.jar?raw=true) | | [vtt-to-eaf](../bin/vtt-to-eaf.jar?raw=true) | [slt-to-eaf](../bin/slt-to-eaf.jar?raw=true) | [cha-to-eaf](../bin/cha-to-eaf.jar?raw=true) | [textgrid-to-eaf](../bin/textgrid-to-eaf.jar?raw=true) | [txt-to-eaf](../bin/txt-to-eaf.jar?raw=true) |
| **vtt** | [trs-to-vtt](../bin/trs-to-vtt.jar?raw=true) | [eaf-to-vtt](../bin/eaf-to-vtt.jar?raw=true) | | [slt-to-vtt](../bin/slt-to-vtt.jar?raw=true) | [cha-to-vtt](../bin/cha-to-vtt.jar?raw=true) | [textgrid-to-vtt](../bin/textgrid-to-vtt.jar?raw=true) | |
| **slt** | [trs-to-slt](../bin/trs-to-slt.jar?raw=true) | [eaf-to-slt](../bin/eaf-to-slt.jar?raw=true) | | | | | |
| **cha** | [trs-to-cha](../bin/trs-to-cha.jar?raw=true) | [eaf-to-cha](../bin/eaf-to-cha.jar?raw=true) | [vtt-to-cha](../bin/vtt-to-cha.jar?raw=true) | | | | |
| **textgrid** | [trs-to-textgrid](../bin/trs-to-textgrid.jar?raw=true) | [eaf-to-textgrid](../bin/eaf-to-textgrid.jar?raw=true) | [vtt-to-textgrid](../bin/vtt-to-textgrid.jar?raw=true) | [slt-to-textgrid](../bin/slt-to-textgrid.jar?raw=true) | [cha-to-textgrid](../bin/cha-to-textgrid.jar?raw=true) | | |
| **txt** | [trs-to-txt](../bin/trs-to-txt.jar?raw=true) | | | | | | |
| **pdf** | [trs-to-pdf](../bin/trs-to-pdf.jar?raw=true) | [eaf-to-pdf](../bin/eaf-to-pdf.jar?raw=true) | [vtt-to-pdf](../bin/vtt-to-pdf.jar?raw=true) | [slt-to-pdf](../bin/slt-to-pdf.jar?raw=true) | [cha-to-pdf](../bin/cha-to-pdf.jar?raw=true) | [textgrid-to-pdf](../bin/textgrid-to-pdf.jar?raw=true) | |
| **tex** | [trs-to-tex](../bin/trs-to-tex.jar?raw=true) | [eaf-to-tex](../bin/eaf-to-tex.jar?raw=true) | [vtt-to-tex](../bin/vtt-to-tex.jar?raw=true) | [slt-to-tex](../bin/slt-to-tex.jar?raw=true) | | [textgrid-to-tex](../bin/textgrid-to-tex.jar?raw=true) | |
| **kaldi** | [trs-to-kaldi](../bin/trs-to-kaldi.jar?raw=true) | [eaf-to-kaldi](../bin/eaf-to-kaldi.jar?raw=true) | | | | [textgrid-to-kaldi](../bin/textgrid-to-kaldi.jar?raw=true) | |


These use the serializers/deserializers in the *formatter* directory of this repository
to read a file in one format, convert it to an annotation graph, and then write that graph
out as a file in another format. As pointed out by
Cochran et al. (2007 - *Report from TILR Working Group 1 : Tools interoperability and input/output formats*)
this saves having order *n<sup>2</sup>* explicit conversion algorithms between formats;
only *2n* format conversions are required
(as some of these formats above are output-only, it's actually less than *2n*).

This exemplifies an approach to linguistic data interoperability called the *interlingua
philosophy on interoperability* by
[Witt et al. (2009)](https://www.w3.org/People/fsasaki/docs/lre-intro.pdf)
and uses annotation graphs as an 'interlingua' similar to work by 
[Schmidt et al. (2008)](https://ids-pub.bsz-bw.de/frontdoor/deliver/index/docId/2308/file/Schmidt%20etc_An_exchange_format_for_multimodal_annotations_2008.pdf),
except that rather using a third file format as a persistent intermediary, the annotation
graph models of the linguistic data are ephemeral, existing in memory only for the duration of the
conversion.

As there is no persistent intermediate file, and many formats do not support the
meta-data, annotation granularity or ontology of other formats, many of these conversions
necessarily entail loss of data. However, mappings are made from one format to another
wherever possible.

For notes about specific correspondences or data losses, use the `--help` command-line
switch, or use the *Help|Information* menu option of the conversion utility concerned.

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
   - add a *description* tag
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
      <version>[1.0.4,)</version>
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
                <name>nzilbb/converter/</name>
                <manifestEntries>
                  <Implementation-Title>${project.description}</Implementation-Title>
                  <Implementation-Version>${project.version}</Implementation-Version>
                  <Implementation-Vendor>New Zealand Institute of Language, Brain and Behaviour</Implementation-Vendor>
                </manifestEntries>
              </manifestSection>
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
                  <version>1.0.4</version>
                  <outputDirectory>${project.build.directory}/classes</outputDirectory>
                </artifactItem>
                <artifactItem>
                  <groupId>nzilbb</groupId>
                  <artifactId>nzilbb.ag</artifactId>
                  <version>1.0.6</version>
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
   rm src/main/java/nzilbb/App.java src/test/java/nzilbb/AppTest.java
   ```
4. Add directory structure for the annotator code:
   ```
   mkdir src/main/java/nzilbb/converter src/main/resources src/main/resources/nzilbb \
     src/main/resources/nzilbb/converter \
     src/test/java/nzilbb/converter src/test/resources src/test/resources/nzilbb \
     src/test/resources/nzilbb/converter
   ```
5. Add an icon for the converter, e.g.:
   ```
   cp ../../ag/src/site/resources/images/labbcat.png \
     src/main/resources/nzilbb/converter/InformatToOutformat.png
   ```
6. Add your implementation to *informattooutformat/src/main/java/nzilbb/converter/InformatToOutformat.java